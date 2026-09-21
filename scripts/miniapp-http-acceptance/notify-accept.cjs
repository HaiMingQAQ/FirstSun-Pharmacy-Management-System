'use strict';
// Notification-only acceptance on the existing PASS_SCOPED DB. Never reruns accept.cjs or resets its marker.
const fs=require('node:fs');
const path=require('node:path');
const e=require('./environment.cjs');
const report={status:'RUNNING',checks:[]};
let stage='preflight';
function check(ok,message) { e.ensure(ok,message); report.checks.push(message); }
async function http(route,body,tenant=901,token,headers={}) {
  return e.networkHttp({method:'POST',path:'/app-api/member/'+route,
    headers:{'tenant-id':String(tenant),'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{}),...headers},
    body:body===undefined?undefined:JSON.stringify(body)});
}
function ok(r) { e.ensure(r.http===200 && r.code===0,`${stage}: HTTP ${r.http}, code ${r.code}`); return r.data; }
function businessSnapshot() {
  return [
    'SELECT id,pay_status,status,IFNULL(pay_no,\'\'),point_deduct,point_earned FROM ph_wx_order ORDER BY id',
    'SELECT id,qty_total,qty_avail,qty_frozen,qty_sold FROM ph_inv_batch ORDER BY id',
    'SELECT id,qty,qty_frozen FROM ph_inv_location_stock ORDER BY id',
    'SELECT id,biz_no,biz_line_id,flow_type,out_qty FROM ph_inv_flow ORDER BY id',
    'SELECT id,status,qty,out_biz_line_id FROM ph_wx_order_line_alloc ORDER BY id',
    'SELECT id,point FROM member_user ORDER BY id',
    'SELECT id,biz_id,biz_type,point,total_point FROM member_point_record ORDER BY id',
    'SELECT id,status,price,refund_price FROM pay_order ORDER BY id'
  ].map(sql=>e.query(sql)).join('\n---\n');
}
async function main() {
  e.verifyFiles(); const env=e.guard(); await e.ready();
  check(JSON.parse(fs.readFileSync(path.join(e.runtime,'result.json'),'utf8')).status==='PASS_SCOPED','Existing scoped acceptance evidence');
  check(e.query('SELECT run_id FROM acceptance_marker')===env.ACCEPT_RUN_ID,'Isolated database marker');
  fs.writeFileSync(path.join(e.runtime,'notify-acceptance.started'),new Date().toISOString(),{flag:'wx'});
  stage='login for one notification fixture';
  ok(await http('auth/send-sms-code?mobile=19900009011'));
  const login=ok(await http('auth/login-or-register?mobile=19900009011',undefined,901,undefined,{'X-Sms-Code':env.ACCEPT_SMS_CODE}));
  check(login.userId===90011 && typeof login.accessToken==='string','Real synthetic login');
  const initialQty=Number(e.query('SELECT qty FROM ph_inv_location_stock WHERE id=9402 AND tenant_id=901'));
  const initialPoints=Number(e.query('SELECT point FROM member_user WHERE id=90011 AND tenant_id=901'));
  stage='create one notification fixture';
  ok(await http('wx-cart/add',{storeId:9101,drugId:9301,qty:1},901,login.accessToken));
  const orderId=ok(await http('wx-order/create',{storeId:9101,orderType:0,usePoints:0,remark:'notification acceptance'},901,login.accessToken));
  e.ensure(Number.isSafeInteger(orderId)&&orderId>0,'Invalid order ID');
  stage='automatic payment business notification';
  ok(await http(`wx-order/simulate-pay?id=${orderId}`,undefined,901,login.accessToken));
  const order=JSON.parse(e.query(`SELECT JSON_OBJECT('merchantOrderId',order_no,'payOrderId',CAST(pay_no AS UNSIGNED)) FROM ph_wx_order WHERE id=${orderId} AND tenant_id=901`));
  e.ensure(Number.isSafeInteger(order.payOrderId)&&order.payOrderId>0,'Invalid persisted payment ID');
  let delivered=false;
  for(let i=0;i<30;i++) {
    if(e.query(`SELECT COUNT(*) FROM pay_notify_task WHERE app_id=9501 AND tenant_id=901 AND type=1 AND data_id=${order.payOrderId} AND status=10`)==='1') { delivered=true;break; }
    await new Promise(resolve=>setTimeout(resolve,1000));
  }
  check(delivered,'Pay module automatic HTTP delivery acknowledged (task status 10)');
  check(e.query(`SELECT COUNT(*) FROM pay_notify_log l JOIN pay_notify_task t ON t.id=l.task_id WHERE t.data_id=${order.payOrderId} AND t.app_id=9501 AND t.tenant_id=901 AND l.status=10`)==='1','Successful delivery log');
  check(Number(e.query('SELECT qty FROM ph_inv_location_stock WHERE id=9402 AND tenant_id=901'))===initialQty-1,'One fixture stock deduction');
  check(Number(e.query('SELECT point FROM member_user WHERE id=90011 AND tenant_id=901'))===initialPoints,'No payment-time point change');
  const before=businessSnapshot();
  stage='duplicate and concurrent notification';
  ok(await http('wx-order/payment-notify',order)); // No member token, as in PayNotifyService.
  const repeated=await Promise.allSettled(Array.from({length:4},()=>http('wx-order/payment-notify',order)));
  for(const r of repeated) { e.ensure(r.status==='fulfilled','Notification transport failure');ok(r.value); }
  check(businessSnapshot()===before,'Duplicate/concurrent notification leaves all business records unchanged');
  stage='invalid notification and retry';
  const foreign=await http('wx-order/payment-notify',order,902);
  check(foreign.http<500 && foreign.code===1030011000,'Other tenant rejected');
  const invalid=await http('wx-order/payment-notify',{...order,payOrderId:0});
  check(invalid.http<500 && invalid.code!==0 && invalid.code!==500,'Invalid payment rejected');
  ok(await http('wx-order/payment-notify',order));
  check(businessSnapshot()===before,'Valid retry after rejection is side-effect free');
  report.status='PASS_NOTIFY';report.orderId=orderId;report.payOrderId=order.payOrderId;
  report.limit='Delivery and duplicate/rejected retry verified; stock failure rollback is covered by targeted H2 tests, not injected here';
}
main().catch(error=>{report.status='FAIL';report.stage=stage;report.error='Notification acceptance stopped; inspect isolated redacted diagnostics';process.exitCode=1;})
  .finally(()=>{if(fs.existsSync(e.runtime))fs.writeFileSync(path.join(e.runtime,'notify-result.json'),JSON.stringify(report,null,2));console.log(`${report.status}: ${stage}; ${report.checks.length} checks`);});
