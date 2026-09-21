// SQL fixture regression, NOT HTTP or payment acceptance. Requires a local mysql:8.0 image.
const { spawnSync } = require('node:child_process');
const { readFileSync } = require('node:fs');
const { randomBytes } = require('node:crypto');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const name = `firstsun-miniapp-audit-inventory-${process.pid}`;
const database = process.env.FIRSTSUN_AUDIT_DB || 'firstsun_miniapp_audit';
if (!/^[a-zA-Z][a-zA-Z0-9_]{0,63}$/.test(database)) throw Error('Invalid isolated database name');
const password = randomBytes(24).toString('hex');
const env = { ...process.env, MYSQL_ROOT_PASSWORD: password, MYSQL_PWD: password };
function docker(args, input, allowFailure = false) {
  const r = spawnSync('docker', args, { input, env, encoding: 'utf8', timeout: 60000 });
  if (r.status !== 0 && !allowFailure) throw Error(`Docker operation failed: ${r.stderr || r.error}`);
  return r;
}
function sql(query) {
  return docker(['exec', '-i', '--env', 'MYSQL_PWD', name, 'mysql', '-h127.0.0.1', '-uroot', '--batch', '--skip-column-names', database], query).stdout.trim();
}
(async () => {
  docker(['run', '-d', '--rm', '--name', name, '--network', 'none', '--tmpfs', '/var/lib/mysql', '--env', 'MYSQL_ROOT_PASSWORD', '--env', `MYSQL_DATABASE=${database}`, 'mysql:8.0']);
  try {
    let ready = false;
    for (let i = 0; i < 45; i++) {
      const r = docker(['exec', '--env', 'MYSQL_PWD', name, 'mysql', '-h127.0.0.1', '-uroot', '-e', 'SELECT 1'], undefined, true);
      if (r.status === 0) { ready = true; break; }
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
    if (!ready) throw Error('Isolated MySQL did not start');
    // Minimal fixture schema; use the actual app projection SQL, not a reimplementation.
    sql(`CREATE TABLE ph_store(id BIGINT,tenant_id BIGINT,status INT,deleted INT);
      CREATE TABLE ph_drug(id BIGINT,tenant_id BIGINT,status INT,approve_status INT,saleable_online INT,deleted INT);
      CREATE TABLE ph_warehouse(id BIGINT,tenant_id BIGINT,store_id BIGINT,status INT,deleted INT);
      CREATE TABLE ph_location(id BIGINT,tenant_id BIGINT,warehouse_id BIGINT,status INT,deleted INT);
      CREATE TABLE ph_inv_batch(id BIGINT,tenant_id BIGINT,store_id BIGINT,warehouse_id BIGINT,drug_id BIGINT,quality_status INT,expiry_date DATE,deleted INT);
      CREATE TABLE ph_inv_location_stock(id BIGINT,tenant_id BIGINT,batch_id BIGINT,location_id BIGINT,qty INT,qty_frozen INT,deleted INT);
      INSERT INTO ph_store VALUES(9,7,1,0);
      INSERT INTO ph_drug VALUES(1,7,1,1,1,0);
      INSERT INTO ph_warehouse VALUES(2,7,9,1,0);
      INSERT INTO ph_location VALUES(3,7,2,1,0);
      INSERT INTO ph_inv_batch VALUES(4,7,9,2,1,0,DATE_ADD(CURRENT_DATE,INTERVAL 1 DAY),0);
      INSERT INTO ph_inv_location_stock VALUES(5,7,4,3,10,2,0);`);
    const source = readFileSync(path.join(root, 'backend/yudao-module-pharmacy/src/main/java/cn/iocoder/yudao/module/pharmacy/dal/mysql/inventory/AppAvailableInventoryMapper.java'), 'utf8');
    const query = source.split('<script>')[1].split('</script>')[0]
      .replace(/<foreach[\s\S]*?<\/foreach>/, '(1)')
      .replaceAll('#{tenantId}', '7').replaceAll('#{storeId}', '9').replaceAll('&gt;', '>');
    assert.equal(sql(query), '1\t8');
    let checks = 1;
    for (const [table, column, value, restore] of [
      ['ph_store','status',0,1], ['ph_store','tenant_id',8,7],
      ['ph_drug','status',0,1], ['ph_drug','approve_status',0,1], ['ph_drug','saleable_online',0,1],
      ['ph_drug','tenant_id',8,7], ['ph_warehouse','status',0,1], ['ph_location','status',0,1],
      ['ph_inv_batch','quality_status',1,0], ['ph_inv_batch','deleted',1,0],
      ['ph_inv_location_stock','tenant_id',8,7], ['ph_inv_location_stock','deleted',1,0],
    ]) {
      sql(`UPDATE ${table} SET ${column}=${value}`);
      assert.equal(sql(query), '', `${table}.${column} must hide inventory`);
      sql(`UPDATE ${table} SET ${column}=${restore}`); checks++;
    }
    sql('UPDATE ph_inv_batch SET expiry_date=DATE_SUB(CURRENT_DATE,INTERVAL 1 DAY)');
    assert.equal(sql(query), ''); checks++;
    sql('UPDATE ph_inv_batch SET expiry_date=CURRENT_DATE');
    assert.equal(sql(query), '1\t8'); checks++;
    assert.equal(sql(query.replace('b.tenant_id = 7', 'b.tenant_id = 8')), ''); checks++;
    assert.equal(sql(query.replace('b.store_id = 9', 'b.store_id = 10')), ''); checks++;
    console.log(JSON.stringify({ type:'isolated MySQL SQL fixture', checks, database, container:name, publishedPorts:[], sharedEnvironmentTouched:false }));
  } finally {
    docker(['stop', name]);
  }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
