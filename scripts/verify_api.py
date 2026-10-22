#!/usr/bin/env python3
"""AI-assisted: run only against a disposable database; creates test users/orders.
Usage: python3 scripts/verify_api.py --base http://localhost:18080/api --allow-write
"""
import argparse, json, time, urllib.request, urllib.error, http.cookiejar
from datetime import datetime
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base',default='http://localhost:8080/api');parser.add_argument('--allow-write',action='store_true');parser.add_argument('--output',default='docs/evidence/api-verification.json')
args=parser.parse_args()
if not args.allow_write: parser.error('Use --allow-write only for a disposable test database')
results=[]
class Client:
 def __init__(self):
  self.jar=http.cookiejar.CookieJar();self.http=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar));self.csrf=None
 def call(self,method,path,data=None,expected=200,label=None,csrf=True,headers=None):
  h=dict(headers or {})
  if method not in ('GET','OPTIONS') and csrf:
   self.csrf=self.call('GET','/auth/csrf')['token']
   h['X-XSRF-TOKEN']=self.csrf
  if data is not None:h['Content-Type']='application/json'
  req=urllib.request.Request(args.base+path,data=json.dumps(data).encode() if data is not None else None,headers=h,method=method)
  try:
   response=self.http.open(req);status=response.status;body=response.read()
  except urllib.error.HTTPError as ex: status=ex.code;body=ex.read()
  value=json.loads(body)
  assert status==expected,(label or path,status,value)
  if label:results.append({'check':label,'method':method,'path':path,'status':status,'passed':True})
  return value.get('data',value)
 def login(self,name):
  data=self.call('POST','/auth/login',{'username':name,'password':'password'},label=name+'登录')
  assert 'token' not in data,'JWT must not appear in login JSON'
  cookie=next(c for c in self.jar if c.name=='DORM_AUTH');assert cookie.has_nonstandard_attr('HttpOnly')
  return data['user']
try:
 anon=Client();anon.call('GET','/users',expected=401,label='匿名访问被拒绝')
 anon.call('POST','/auth/login',{'username':'student','password':'password'},expected=403,csrf=False,label='无CSRF登录被拒绝')
 admin=Client();admin.login('sysadmin');roles=admin.call('GET','/users/roles');student_role=next(r['id'] for r in roles if r['code']=='STUDENT')
 name='course_test_'+str(int(time.time()))
 user=admin.call('POST','/users',{'username':name,'password':'password','name':'实验临时用户','roleId':student_role},label='用户新增')
 assert not user.get('passwordHash');uid=user['id']
 page=admin.call('GET','/users?keyword='+name,label='用户查询');assert any(r['id']==uid for r in page['records'])
 admin.call('PUT',f'/users/{uid}',{'name':'实验修改用户','roleId':student_role,'status':'ACTIVE'},label='用户修改')
 assert admin.call('GET','/users?keyword='+name)['records'][0]['name']=='实验修改用户'
 admin.call('DELETE',f'/users/{uid}',label='用户删除');assert admin.call('GET','/users?keyword='+name)['total']==0
 student=Client();student.login('student');student.call('GET','/users',expected=403,label='学生越权用户管理被拒绝')
 student.call('POST','/notices',{'title':'越权测试','content':'不应保存'},expected=403,label='方法角色权限校验')
 student.call('POST','/repair-orders',{},expected=400,label='空报修表单被拒绝')
 student.call('GET','/auth/me',headers={'Authorization':'Bearer forged.jwt.value'},expected=401,label='伪造JWT被拒绝')
 admin.call('PUT','/users/4',{'name':'系统管理员','roleId':student_role,'status':'ACTIVE'},expected=400,label='最后一个系统管理员禁止降权')
 rooms=student.call('GET','/public/dorm/rooms');categories=student.call('GET','/public/categories')
 order=student.call('POST','/repair-orders',{'categoryId':categories[0]['id'],'dormRoomId':rooms[0]['id'],'description':'课程实验独立数据库验证：灯具故障','contactPhone':'13800000000'},label='学生提交报修')
 oid=order['id'];manager=Client();manager.login('admin');manager.call('PUT',f'/repair-orders/{oid}/audit',{'approved':True},label='管理员审核')
 worker=Client();wu=worker.login('repairer');manager.call('PUT',f'/repair-orders/{oid}/assign',{'repairerId':wu['id'],'remark':'课程实验'},label='管理员派单')
 worker.call('PUT',f'/repair-orders/{oid}/start',label='维修人员开始维修')
 worker.call('PUT',f'/repair-orders/{oid}/finish',{'faultReason':'灯泡损坏','repairMethod':'更换灯泡','resultSummary':'照明恢复'},label='维修人员提交结果')
 done=student.call('PUT',f'/repair-orders/{oid}/feedback',{'speedScore':5,'attitudeScore':5,'qualityScore':5,'overallScore':5,'comment':'实验验证','unresolved':False},label='学生确认评价')
 assert done['status']=='COMPLETED'
 student.call('PUT',f'/repair-orders/{oid}/cancel',expected=400,label='已完成工单禁止取消')
 student.call('POST','/auth/logout',label='退出登录');student.call('GET','/auth/me',expected=401,label='退出后Cookie失效')
except Exception as ex:
 results.append({'check':'执行中断','passed':False,'error':str(ex)});raise
finally:
 out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True)
 out.write_text(json.dumps({'verifiedAt':datetime.now().astimezone().isoformat(),'baseURL':args.base,'checks':results},ensure_ascii=False,indent=2))
 print(json.dumps({'passed':sum(r['passed'] for r in results),'total':len(results),'evidence':str(out)},ensure_ascii=False))
