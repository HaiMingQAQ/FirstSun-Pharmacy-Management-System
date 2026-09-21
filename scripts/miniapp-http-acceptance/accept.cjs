'use strict';
// Real HTTP + real MySQL assertions. No service mocks, reflection, SQL writes or token fabrication.
const fs = require('node:fs');
const path = require('node:path');
const e = require('./environment.cjs');
const report = {scope:'HTTP/MySQL development mock payment; no real money', status:'RUNNING', checks:[],
  notificationDelivery:'NOT ACCEPTED: current pharmacy business callback endpoint is absent',
  excluded:['prescriptions','receipt/completion','real payment channels','pre-reserved employee stock flow']};
let stage='preflight', currentOrder=null, env;
function check(ok,label) { e.ensure(ok,`Assertion failed: ${label}`); report.checks.push(label); }
function n(sql) { const value=e.query(sql); e.ensure(/^-?\d+$/.test(value),'SQL expected an integer'); return Number(value); }
function id(value) { e.ensure(Number.isSafeInteger(value) && value>0,'Invalid server ID'); return value; }
async function http(method,url,session,body,headers={}) {
  const response=await e.networkHttp({path:`/app-api${url}`,
    method,headers:{'tenant-id':String(session?.tenant || 901),
      ...(session?.token ? {Authorization:`Bearer ${session.token}`} : {}),
      ...(body ? {'Content-Type':'application/json'} : {}),...headers},body:body ? JSON.stringify(body) : undefined});
  e.ensure(Number.isInteger(response.code),`${stage}: missing business status`);
  return response;
}
function success(result) {
  e.ensure(result.http>=200 && result.http<300 && result.code===0,
    `${stage}: HTTP ${result.http}, business code ${result.code}; response content withheld`);
  return result.data;
}
async function login(mobile,user,tenant) {
  const session={tenant};
  stage=`login synthetic member ${user}`;
  success(await http('POST',`/member/auth/send-sms-code?mobile=${mobile}`,session));
  const data=success(await http('POST',`/member/auth/login-or-register?mobile=${mobile}`,session,undefined,{'X-Sms-Code':env.ACCEPT_SMS_CODE}));
  check(data.userId===user && typeof data.accessToken==='string' && data.accessToken.length>10,`real login ${user}`);
  return {tenant,token:data.accessToken};
}
function snapshot(orderId) {
  id(orderId);
  const rows=e.query(`SELECT JSON_OBJECT('id',o.id,'payStatus',o.pay_status,'status',o.status,
    'amountFen',CAST(o.payable_amount*100 AS SIGNED),'deduct',o.point_deduct,'earned',o.point_earned,
    'payId',o.pay_no,
    'payments',(SELECT COUNT(*) FROM pay_order p WHERE p.merchant_order_id=o.order_no AND p.app_id=9501 AND p.tenant_id=901),
    'paid',(SELECT COUNT(*) FROM pay_order p WHERE p.id=o.pay_no AND p.tenant_id=901 AND p.app_id=9501
      AND p.merchant_order_id=o.order_no AND p.user_id=o.member_id AND p.user_type=1 AND p.price=o.payable_amount*100
      AND p.status=10 AND p.success_time IS NOT NULL AND p.refund_price=0 AND p.channel_code='mock'),
    'extensions',(SELECT COUNT(*) FROM pay_order_extension x WHERE x.order_id=o.pay_no AND x.tenant_id=901),
    'successfulExtensions',(SELECT COUNT(*) FROM pay_order_extension x WHERE x.order_id=o.pay_no AND x.tenant_id=901 AND x.status=10 AND x.channel_code='mock'),
    'notifyTasks',(SELECT COUNT(*) FROM pay_notify_task t WHERE t.data_id=o.pay_no AND t.app_id=9501 AND t.tenant_id=901 AND t.type=1),
    'flows',(SELECT COUNT(*) FROM ph_inv_flow f WHERE f.biz_no=o.order_no AND f.tenant_id=901),
    'outQty',(SELECT COALESCE(SUM(f.out_qty),0) FROM ph_inv_flow f WHERE f.biz_no=o.order_no AND f.tenant_id=901 AND f.flow_type=20 AND f.biz_type=2 AND f.operator=0),
    'allocations',(SELECT COUNT(*) FROM ph_wx_order_line_alloc a WHERE a.wx_order_id=o.id AND a.tenant_id=901 AND a.status=1),
    'allocatedQty',(SELECT COALESCE(SUM(a.qty),0) FROM ph_wx_order_line_alloc a WHERE a.wx_order_id=o.id AND a.tenant_id=901 AND a.status=1),
    'pointEvents',(SELECT COUNT(*) FROM member_point_record r WHERE r.biz_id=o.order_no AND r.user_id=90011 AND r.tenant_id=901),
    'deductEvents',(SELECT COUNT(*) FROM member_point_record r WHERE r.biz_id=o.order_no AND r.user_id=90011 AND r.tenant_id=901 AND r.biz_type=3 AND r.point=-100),
    'pointDelta',(SELECT COALESCE(SUM(r.point),0) FROM member_point_record r WHERE r.biz_id=o.order_no AND r.user_id=90011 AND r.tenant_id=901))
    FROM ph_wx_order o WHERE o.id=${orderId} AND o.member_id=90011 AND o.tenant_id=901;`);
  e.ensure(rows,'Order missing from isolated DB'); return JSON.parse(rows);
}
function stockAndPoints(sold,points) {
  check(n(`SELECT qty FROM ph_inv_location_stock WHERE id=9402 AND tenant_id=901`)===50-sold,'location stock exact');
  check(n(`SELECT COUNT(*) FROM ph_inv_batch WHERE id=9401 AND tenant_id=901 AND qty_total=${50-sold} AND qty_avail=${50-sold} AND qty_frozen=0 AND qty_sold=${sold}`)===1,'batch conservation');
  check(n('SELECT qty_frozen FROM ph_inv_location_stock WHERE id=9402')===0,'no unexpected reservation');
  check(n('SELECT point FROM member_user WHERE id=90011 AND tenant_id=901')===points,'member point balance exact');
  check(n('SELECT COUNT(*) FROM member_user WHERE id IN (90012,90021) AND point=500')===2,'other members unchanged');
}
async function create(session,qty,number) {
  stage=`create order ${number}`;
  success(await http('POST','/member/wx-cart/add',session,{storeId:9101,drugId:9301,qty}));
  const orderId=id(success(await http('POST','/member/wx-order/create',session,
    {storeId:9101,orderType:0,usePoints:100,remark:`synthetic HTTP acceptance ${number}`,payableAmount:0,payStatus:1})));
  currentOrder=orderId;
  const s=snapshot(orderId);
  check(s.amountFen===qty*1234-100 && s.payStatus===0 && s.status===0,'server amount/state ignores client fields');
  check(s.payments===0 && s.flows===0 && s.allocations===0,'creation does not move stock or create payment');
  check(s.deduct===100 && s.earned===0 && s.pointEvents===1 && s.deductEvents===1 && s.pointDelta===-100,'points deducted once at creation only');
  stockAndPoints(number===1 ? 0 : 2,500-100*number);
  return orderId;
}
function settled(orderId,qty,totalSold,points) {
  const s=snapshot(orderId);
  check(s.payStatus===1 && s.status===1 && s.amountFen===qty*1234-100,'paid order and server amount');
  check(s.payments===1 && s.paid===1 && s.extensions===1 && s.successfulExtensions===1,'one matching successful payment and extension');
  check(s.notifyTasks===1,'one persisted notification task (delivery not claimed)');
  check(s.flows===1 && s.outQty===qty && s.allocations===1 && s.allocatedQty===qty,'stock flow and allocation exactly once');
  check(s.pointEvents===1 && s.deductEvents===1 && s.pointDelta===-100 && s.earned===0,'payment never deducts/earns points again');
  stockAndPoints(totalSold,points);
  return s;
}
async function deny(session,orderId,codes,label,headers={}) {
  stage=label;
  const before=snapshot(orderId);
  const result=await http('POST',`/member/wx-order/simulate-pay?id=${orderId}`,session,undefined,headers);
  check(result.http<500 && codes.includes(result.code),label); // 500 / unrelated failures never count as access denial.
  check(JSON.stringify(snapshot(orderId))===JSON.stringify(before),'denied request makes no order/payment/stock/point changes');
}
async function main() {
  e.verifyFiles(); env=e.guard();
  for (const [name,s] of Object.entries(e.compose.services)) {
    const c=e.inspect('container',s.container_name);
    e.ensure(c?.State.Running && c.State.Health?.Status==='healthy',`${name} not healthy`);
    if (name==='client') {
      e.ensure(Object.keys(c.HostConfig.PortBindings || {}).length===0,'HTTP client must not publish ports');
      continue;
    }
    const port=name==='backend' ? '48080/tcp' : name==='mysql' ? '3306/tcp' : '6379/tcp';
    const expected=name==='backend' ? env.ACCEPT_HTTP_PORT : name==='mysql' ? env.ACCEPT_MYSQL_PORT : env.ACCEPT_REDIS_PORT;
    e.ensure(c.HostConfig.PortBindings[port]?.length===1 && c.HostConfig.PortBindings[port][0].HostIp==='127.0.0.1'
      && c.HostConfig.PortBindings[port][0].HostPort===expected,'Unexpected port binding');
  }
  await e.ready(); // Actual HTTP response, not HostConfig or TCP configuration, gates login.
  check(e.query('SELECT run_id FROM acceptance_marker')===env.ACCEPT_RUN_ID,'database identity marker');
  check(e.query('SELECT DATABASE()')===env.ACCEPT_DB,'explicit isolated database');
  check(n('SELECT COUNT(*) FROM ph_wx_order')===0 && n('SELECT COUNT(*) FROM pay_order')===0,'fresh database required');
  check(n('SELECT COUNT(*) FROM pay_channel WHERE code<>\'mock\' OR app_id<>9501 OR tenant_id<>901')===0,'only synthetic mock channel');
  fs.writeFileSync(path.join(e.runtime,'acceptance.started'),new Date().toISOString(),{flag:'wx'});
  stockAndPoints(0,500);
  const owner=await login('19900009011',90011,901), other=await login('19900009012',90012,901), foreign=await login('19900009021',90021,902);
  const first=await create(owner,2,1);
  await deny(other,first,[1030011007],'other member payment denied');
  await deny(foreign,first,[1030011000,403],'other tenant payment denied');
  await deny(owner,first,[1030011000,403,401],'tenant header replay denied',{'tenant-id':'902'});
  await deny(null,first,[401,403],'anonymous payment denied');
  stage='first simulated payment';
  success(await http('POST',`/member/wx-order/simulate-pay?id=${first}`,owner,{price:1,status:10}));
  const before=settled(first,2,2,400);
  stage='repeat simulated payment';
  success(await http('POST',`/member/wx-order/simulate-pay?id=${first}`,owner));
  check(JSON.stringify(settled(first,2,2,400))===JSON.stringify(before),'sequential repeat unchanged');
  const second=await create(owner,3,2);
  stage='four concurrent simulated payments';
  const responses=await Promise.allSettled(Array.from({length:4},()=>http('POST',`/member/wx-order/simulate-pay?id=${second}`,owner)));
  for (const response of responses) { e.ensure(response.status==='fulfilled','Concurrent HTTP transport failure'); success(response.value); }
  settled(second,3,5,300);
  check(n('SELECT COUNT(*) FROM pay_order')===2 && n('SELECT COUNT(*) FROM pay_order_extension')===2,'global payment count unchanged by concurrency');
  check(n('SELECT COUNT(*) FROM ph_inv_flow WHERE flow_type=20')===2 && n('SELECT COUNT(*) FROM member_point_record')===2,'global stock and point events exact');
  report.status='PASS_SCOPED'; report.orderIds=[first,second];
  report.message='Real HTTP and MySQL checks passed for listed scope; notification delivery and excluded flows remain unaccepted';
}
main().catch(error=>{
  report.status='FAIL'; report.stage=stage;
  // Do not print tokens, raw HTTP bodies, SQL result dumps or low-level exceptions.
  report.message=error.message?.startsWith('Assertion failed:') || error.message?.includes('response content withheld')
    ? error.message : `Stopped at ${stage}; inspect isolated redacted diagnostic log`;
  if (currentOrder) {
    try {
      report.failureSnapshot=snapshot(currentOrder);
      report.failureStockAndPoints=JSON.parse(e.query(`SELECT JSON_OBJECT(
        'locationQty',(SELECT qty FROM ph_inv_location_stock WHERE id=9402 AND tenant_id=901),
        'batchQty',(SELECT qty_total FROM ph_inv_batch WHERE id=9401 AND tenant_id=901),
        'memberPoints',(SELECT point FROM member_user WHERE id=90011 AND tenant_id=901))`));
    } catch { report.failureSnapshot='unavailable'; }
  }
  process.exitCode=1;
}).finally(()=>{
  if (fs.existsSync(e.runtime)) fs.writeFileSync(path.join(e.runtime,'result.json'),JSON.stringify(report,null,2));
  console.log(`${report.status}: ${report.message || stage}; ${report.checks.length} checks recorded`);
});
