'use strict';
// Runs only inside the dedicated client container. Tokens travel over stdin/stdout, never argv/logs.
const origin = 'http://backend:48080';
async function main() {
  const ready = process.argv[2] === '--ready';
  let request;
  if (ready) request = {method:'GET',path:'/actuator/health',headers:{}};
  else {
    let input='';
    for await (const chunk of process.stdin) {
      input+=chunk;
      if (input.length>1024*1024) throw Error('Request too large');
    }
    request=JSON.parse(input);
    if (!['GET','POST','PUT','DELETE'].includes(request.method) ||
        !/^\/app-api\/member\/[a-z0-9/?=&%_.-]+$/i.test(request.path)) throw Error('Unexpected acceptance route');
  }
  const response=await fetch(origin+request.path, {method:request.method,headers:request.headers,
    body:request.body,redirect:'error',signal:AbortSignal.timeout(30000)});
  const data=await response.json();
  if (ready) {
    if (response.status!==200 || data.status!=='UP') throw Error('HTTP health is not UP');
    process.stdout.write(JSON.stringify({http:200,status:'UP',origin}));
  } else {
    if (!Number.isInteger(data.code)) throw Error('Missing business status');
    process.stdout.write(JSON.stringify({...data,http:response.status}));
  }
}
main().catch(()=>{process.stderr.write('Isolated HTTP transport/readiness failed\n');process.exitCode=1;});
