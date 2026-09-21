'use strict';
// No external npm modules. check/prepare are filesystem-only; all Docker operations are explicit commands.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawnSync, spawn } = require('node:child_process');
const base = __dirname, root = path.resolve(base, '../..'), runtime = path.join(base, 'runtime');
const project = 'firstsun-miniapp-http-acceptance', label = 'io.firstsun.acceptance';
const compose = JSON.parse(fs.readFileSync(path.join(base, 'compose.json'), 'utf8'));
const migrations = ['20260915_c_inventory_facade_flow_ref.sql', '20260916_f_wx_order_line_alloc.sql',
  '20260916_f_wx_order_alloc_frozen_stage.sql', '20260916_f_wx_order_alloc_out_ref.sql',
  '20260918_f_member_sms_code.sql', '20260918_f_wx_order_points.sql'];
const inputs = ['backend/sql/mysql/ruoyi-vue-pro.sql', 'sql/firstsun_pharmacy_init.sql', 'backend/Dockerfile',
  ...migrations.map(n => `sql/migrations/${n}`), ...['compose.json','env.example','seed.sql','mysql.Dockerfile','redis.Dockerfile','http-client.Dockerfile','http-client.cjs','.dockerignore','environment.cjs','accept.cjs'].map(n => `scripts/miniapp-http-acceptance/${n}`)];
function ensure(ok, message) { if (!ok) throw Error(message); }
function read(relative) { return fs.readFileSync(path.join(root, relative), 'utf8'); }
function sha(value) { return crypto.createHash('sha256').update(value).digest('hex'); }
function parseEnv(text) {
  const result = {};
  for (const line of text.split(/\r?\n/).filter(l => l.trim() && !l.startsWith('#'))) {
    const match = line.match(/^([A-Z_]+)=([A-Za-z0-9_]+)$/);
    ensure(match && !Object.hasOwn(result, match[1]), 'Invalid or duplicate isolated env entry');
    result[match[1]] = match[2];
  }
  return result;
}
function config() {
  const env = parseEnv(fs.readFileSync(path.join(runtime, 'acceptance.env'), 'utf8'));
  const template = parseEnv(fs.readFileSync(path.join(base, 'env.example'), 'utf8'));
  ensure(Object.keys(env).length === Object.keys(template).length, 'Unexpected env keys');
  for (const [key,value] of Object.entries(template)) {
    ensure(value === 'GENERATE' ? /^[a-f0-9]{32,64}$/.test(env[key] || '') ||
      (key === 'ACCEPT_SMS_CODE' && /^\d{6}$/.test(env[key] || '')) : env[key] === value,
      `Invalid isolated setting: ${key}`);
  }
  return env;
}
function check() {
  ensure(compose.name === project, 'Wrong project');
  ensure(Object.keys(compose.services).join(',') === 'mysql,redis,backend,client', 'Unexpected services');
  for (const [name,s] of Object.entries(compose.services)) {
    ensure(s.container_name === `firstsun-miniapp-http-${name}` && s.image === `${s.container_name}:acceptance`, 'Resource isolation mismatch');
    ensure(s.networks.length === 1 && s.networks[0] === 'acceptance', 'External network not allowed');
    ensure(!s.privileged && !s.network_mode && !s.env_file, 'Unexpected service privileges/config');
    ensure((s.ports || []).every(p => p.startsWith('127.0.0.1:')), 'Only loopback ports allowed');
    ensure(fs.existsSync(path.resolve(base,s.build.context,s.build.dockerfile)), 'Missing Dockerfile');
  }
  ensure(compose.networks.acceptance.internal === true && !compose.networks.acceptance.external, 'Network must be internal');
  ensure(Object.values(compose.volumes).every(v => !v.external && v.name.startsWith('firstsun-miniapp-http-')), 'External volume');
  for (const file of inputs) ensure(fs.existsSync(path.join(root,file)), `Missing input: ${file}`);
  // Keep the original source table definitions; never import baseline accounts/secrets or DROP statements.
  const tables = [...read('backend/sql/mysql/ruoyi-vue-pro.sql').matchAll(/^CREATE TABLE[\s\S]*?^\)[^\r\n]*;/gm)];
  ensure(tables.length > 20, 'Framework schema extraction failed');
  for (const name of ['system_tenant','system_oauth2_client','system_oauth2_access_token','system_oauth2_refresh_token'])
    ensure(tables.some(m => m[0].startsWith(`CREATE TABLE \`${name}\``)), `Missing framework table: ${name}`);
  for (const name of migrations) ensure(!/^\s*USE\s|^\s*CREATE\s+DATABASE\s/im.test(read(`sql/migrations/${name}`)), 'Migration changes database');
  ensure(!/^\s*(USE|INSERT|DROP)\s/im.test(read('sql/firstsun_pharmacy_init.sql')), 'Unexpected baseline DML');
  const definitions = new Map();
  for (const text of [read('backend/sql/mysql/ruoyi-vue-pro.sql'), read('sql/firstsun_pharmacy_init.sql')])
    for (const match of text.matchAll(/^CREATE TABLE[\s\S]*?^\)[^\r\n]*;/gm)) {
      const name=match[0].match(/^CREATE TABLE\s+`?(\w+)/)[1];
      definitions.set(name, new Set([...match[0].matchAll(/^\s+`?(\w+)`?\s+(?:BIGINT|INT|TINYINT|SMALLINT|VARCHAR|CHAR|DECIMAL|DATETIME|TIMESTAMP|BIT|JSON|TEXT|DOUBLE|DATE)\b/gim)].map(m=>m[1])));
    }
  const seed=fs.readFileSync(path.join(base,'seed.sql'),'utf8');
  for (const match of seed.matchAll(/INSERT INTO\s+(\w+)\s*\(([^)]+)\)/g)) {
    ensure(definitions.has(match[1]), `Unknown seed table: ${match[1]}`);
    for (const column of match[2].split(',').map(c=>c.trim())) ensure(definitions.get(match[1]).has(column),`Unknown seed column: ${match[1]}.${column}`);
  }
  console.log('Static recipe/path/isolation checks PASS (not Docker/MySQL validation)');
}
function prepare() {
  check();
  ensure(!fs.existsSync(runtime), 'runtime already exists; do not overwrite credentials or evidence');
  const values = parseEnv(fs.readFileSync(path.join(base,'env.example'),'utf8'));
  for (const key of Object.keys(values)) if (values[key] === 'GENERATE') values[key] = key === 'ACCEPT_SMS_CODE'
    ? String(crypto.randomInt(100000,1000000)) : crypto.randomBytes(key === 'ACCEPT_RUN_ID' ? 16 : 24).toString('hex');
  fs.mkdirSync(path.join(runtime,'init'), {recursive:true});
  fs.writeFileSync(path.join(runtime,'acceptance.env'), Object.entries(values).map(([k,v])=>`${k}=${v}`).join('\n')+'\n', {mode:0o600,flag:'wx'});
  const framework = [...read('backend/sql/mysql/ruoyi-vue-pro.sql').matchAll(/^CREATE TABLE[\s\S]*?^\)[^\r\n]*;/gm)]
    .map(m=>m[0]).filter(sql => /^CREATE TABLE `(?:system_|infra_)/.test(sql));
  fs.writeFileSync(path.join(runtime,'init','01-framework.sql'), 'SET NAMES utf8mb4;\n'+framework.join('\n\n')+'\n');
  fs.copyFileSync(path.join(root,'sql/firstsun_pharmacy_init.sql'),path.join(runtime,'init','02-pharmacy.sql'));
  migrations.forEach((file,i)=>fs.copyFileSync(path.join(root,'sql/migrations',file),path.join(runtime,'init',`${String(i+3).padStart(2,'0')}-${file}`)));
  fs.writeFileSync(path.join(runtime,'init','99-seed.sql'), fs.readFileSync(path.join(base,'seed.sql'),'utf8')
    .replaceAll('__RUN_ID__',values.ACCEPT_RUN_ID).replaceAll('__OAUTH_SECRET__',crypto.randomBytes(24).toString('hex')));
  const hashes = Object.fromEntries(inputs.map(file=>[file,sha(fs.readFileSync(path.join(root,file)))]));
  for (const file of fs.readdirSync(path.join(runtime,'init'))) hashes[`scripts/miniapp-http-acceptance/runtime/init/${file}`] = sha(fs.readFileSync(path.join(runtime,'init',file)));
  hashes['scripts/miniapp-http-acceptance/runtime/acceptance.env'] = sha(fs.readFileSync(path.join(runtime,'acceptance.env')));
  fs.writeFileSync(path.join(runtime,'manifest.json'),JSON.stringify(hashes,null,2));
  console.log('Prepared isolated env + schema + seed; generated secrets are not printed');
}
function verifyFiles() {
  for (const [file,hash] of Object.entries(JSON.parse(fs.readFileSync(path.join(runtime,'manifest.json'),'utf8'))))
    ensure(sha(fs.readFileSync(path.join(root,file))) === hash, `Prepared input changed: ${file}; STOP`);
}
function docker(args, input, optional = false, timeout = 60000) {
  // Parent environment must never override the explicit isolated Compose env file.
  const env = {...process.env};
  for (const key of Object.keys(env)) if (key.startsWith('ACCEPT_') || key.startsWith('COMPOSE_')) delete env[key];
  const result = spawnSync('docker',args,{cwd:base,env,input,encoding:'utf8',timeout,maxBuffer:32*1024*1024});
  if (!optional && result.status!==0 && fs.existsSync(runtime))
    fs.writeFileSync(path.join(runtime,'command.redacted.log'),redact(`${result.stdout || ''}\n${result.stderr || ''}`));
  if (!optional) ensure(result.status === 0, `Docker ${args[0]} failed; output withheld (may contain credentials). Use diagnose.`);
  return result;
}
function localDocker() {
  ensure(!process.env.DOCKER_HOST && !process.env.DOCKER_CONTEXT, 'Explicit Docker host/context override is forbidden');
  const ctx = JSON.parse(docker(['context','inspect']).stdout)[0];
  ensure(/^(npipe|unix):/.test(ctx.Endpoints.docker.Host), 'Only local Docker Desktop/Unix engine is allowed');
  docker(['info','--format','{{.ServerVersion}}']);
}
function inspect(type,name) {
  const r = docker([type,'inspect',name],undefined,true);
  if (r.status !== 0) { ensure(/No such|not found/i.test(r.stderr), `Cannot inspect ${type} ${name}`); return null; }
  return JSON.parse(r.stdout)[0];
}
function guard(fresh=false) {
  const env = config();
  const manifest=JSON.parse(fs.readFileSync(path.join(runtime,'manifest.json'),'utf8'));
  ensure(sha(fs.readFileSync(path.join(base,'compose.json')))===manifest['scripts/miniapp-http-acceptance/compose.json'], 'Compose changed after preparation; STOP');
  localDocker();
  for (const s of Object.values(compose.services)) {
    const item = inspect('container',s.container_name);
    if (item) {
      ensure(!fresh, 'Named container already exists; refusing to recreate');
      ensure(item.Config.Labels?.[label] === env.ACCEPT_RUN_ID && item.Config.Labels?.['com.docker.compose.project'] === project, 'Container ownership mismatch');
      ensure(item.Config.Image === s.image && Object.keys(item.NetworkSettings.Networks).every(n=>n===compose.networks.acceptance.name), 'Unexpected image/network');
      for (const mount of item.Mounts) if (mount.Type === 'volume') ensure(Object.values(compose.volumes).some(v=>v.name===mount.Name), 'Unexpected mounted volume');
    }
    const img = inspect('image',s.image);
    if (img) ensure(img.Config.Labels?.[label] === env.ACCEPT_RUN_ID, 'Image name belongs to another run; STOP');
  }
  for (const [type,resources] of [['volume',Object.values(compose.volumes)],['network',Object.values(compose.networks)]]) {
    for (const r of resources) {
      const item = inspect(type,r.name);
      if (item) {
        ensure(!fresh && item.Labels?.[label] === env.ACCEPT_RUN_ID && item.Labels?.['com.docker.compose.project'] === project, 'Volume/network ownership mismatch');
        if (type === 'network') ensure(Object.values(item.Containers || {}).every(c=>Object.values(compose.services).some(s=>s.container_name===c.Name)), 'Unexpected container in acceptance network');
      }
    }
  }
  return env;
}
function composeCommand(args, timeout=60000) {
  return docker(['compose','--project-name',project,'--env-file',path.join(runtime,'acceptance.env'),'-f',path.join(base,'compose.json'),...args],undefined,false,timeout);
}
function query(sql) {
  config();
  // Password remains inside the container; neither command arguments nor stdout contain it.
  return docker(['exec','-i',compose.services.mysql.container_name,'sh','-c',
    'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -h127.0.0.1 -u"$MYSQL_USER" --default-character-set=utf8mb4 --batch --raw --skip-column-names "$MYSQL_DATABASE"'],sql).stdout.trim();
}
function networkHttp(request) {
  // Async child processes preserve true overlap for the existing concurrent-payment assertions.
  return new Promise((resolve,reject)=>{
    const args=['exec','-i',compose.services.client.container_name,'node','/client/http-client.cjs'];
    if (!request) args.push('--ready');
    const child=spawn('docker',args,{cwd:base,stdio:['pipe','pipe','pipe'],timeout:35000});
    let output='';
    child.stdout.on('data',chunk=>{output+=chunk; if (output.length>2*1024*1024) child.kill();});
    child.stderr.resume(); // May contain transport diagnostics; never leak request/response secrets.
    child.stdin.on('error',()=>{});
    child.on('error',()=>reject(Error('Isolated HTTP client could not start')));
    child.on('close',code=>{
      if (code!==0) return reject(Error('Isolated HTTP client failed; response withheld'));
      try { resolve(JSON.parse(output)); } catch { reject(Error('Invalid isolated HTTP response')); }
    });
    child.stdin.end(request ? JSON.stringify(request) : '');
  });
}
async function ready() {
  verifyFiles(); guard();
  const client=inspect('container',compose.services.client.container_name);
  ensure(client?.State.Running,'Dedicated HTTP client is not running');
  let result;
  for (let attempt=0;attempt<6;attempt++) {
    try { result=await networkHttp(); break; } catch {
      if (attempt===5) throw Error('Real HTTP readiness failed; acceptance must not start');
      await new Promise(resolve=>setTimeout(resolve,2000));
    }
  }
  ensure(result.http===200 && result.status==='UP','HTTP health failed');
  fs.writeFileSync(path.join(runtime,'http-readiness.json'),JSON.stringify({...result,checkedAt:new Date().toISOString()},null,2));
  console.log('Real HTTP readiness PASS: dedicated client -> http://backend:48080/actuator/health (200 UP)');
}
function redact(text) {
  for (const value of Object.values(config())) if (value.length>=6) text=text.replaceAll(value,'[REDACTED]');
  return text.replace(/\b1\d{10}\b/g,'[MOBILE]').replace(/(Bearer\s+)\S+/gi,'$1[REDACTED]')
    .replace(/(["']?(?:access_token|accessToken|refresh_token|refreshToken|password|secret|code|X-Sms-Code)["']?\s*[:=]\s*)(?:"[^"]*"|'[^']*'|[^,\s}\]]+)/gi,'$1[REDACTED]');
}
function safeError(error) { console.error(error.message || 'Acceptance operation failed'); process.exitCode=1; }
async function main() {
  const command=process.argv[2];
  if (command==='check') return check();
  if (command==='prepare') return prepare();
  if (command==='ready') return ready();
  if (command==='up') {
    verifyFiles(); guard(true); composeCommand(['config','--quiet']);
    console.log('Building only the four isolated images (may take several minutes)');
    composeCommand(['build','mysql','redis','backend','client'],3600000);
    guard(true); composeCommand(['up','-d','--no-build','--wait','--wait-timeout','600'],660000);
    await ready();
    console.log('Isolated services HTTP-ready; run accept.cjs next'); return;
  }
  if (command==='down') {
    // Cleanup still works after source edits, but resource ownership must match the generated run ID.
    guard(); composeCommand(['down','--volumes','--rmi','all','--timeout','30'],120000);
    console.log('Removed only this project containers, images, network and volumes; local evidence retained'); return;
  }
  if (command==='diagnose') {
    guard();
    let logs='';
    for (const s of Object.values(compose.services)) {
      const r=docker(['logs','--tail','120',s.container_name],undefined,true);
      logs += `\n${s.container_name}\n${r.stdout || ''}\n${r.stderr || ''}`;
    }
    fs.writeFileSync(path.join(runtime,'diagnostic.redacted.log'),redact(logs));
    console.log('Saved runtime/diagnostic.redacted.log; inspect locally, do not publish raw Docker logs'); return;
  }
  throw Error('Usage: node environment.cjs check|prepare|up|ready|down|diagnose');
}
module.exports={root,base,runtime,compose,config,verifyFiles,guard,inspect,query,ensure,safeError,networkHttp,ready};
if (require.main===module) main().catch(safeError);
