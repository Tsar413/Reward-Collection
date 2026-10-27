from runtime import *
import requests,hashlib,secrets,zipfile,xml.etree.ElementTree as ET,shutil
from PIL import Image,ImageDraw
F=REPO/'frontend';P=F/'evidence/private';P.mkdir(exist_ok=True);P.chmod(0o700)
fixtures=F/'tests/fixtures';fixtures.mkdir(exist_ok=True)
s=requests.Session();s.trust_env=False;url=f'http://127.0.0.1:{APP_PORT}'
password=json.loads((P/'accounts.json').read_text())['admin']['password'];prefix='FE'+BASE.name.removeprefix('frontend_run_').replace('_','')[2:]
def req(method,path,data=None,file=None):
 kw={'json':data} if file is None else {'files':file}
 r=s.request(method,url+'/api'+path,timeout=30,**kw)
 assert r.status_code==200,(path,r.status_code,r.text[:250]);return r.json()['data']
token=req('POST','/sys-login/login',{'userNo':'IT_ADM_A','password':password})['token'];s.headers['Authorization']='Bearer '+token
orgs=req('GET','/sys-org/all');a,b=[o['id'] for o in orgs]
for org,name in [(a,'青禾实验学校'),(b,'远山第二学校')]:req('PUT',f'/sys-org/update/{org}',{'orgName':name,'remark':'前端验收专用合成组织'})
users={};classes={};accounts={'admin':{'userNo':'IT_ADM_A','password':password}}
for alias,name,org in [('teacher','林老师',a),('teacher2','周老师',a),('teacherB','陈老师',b)]:
 no=prefix+'_'+alias;req('POST','/sys-user/registration-one',{'orgId':org,'realName':name,'userNo':no,'password':password,'role':'TEACHER'});u=next(u for u in req('GET','/sys-user/all') if u['userNo']==no);users[alias]=u;accounts[alias]={'userNo':no,'password':password}
for key,org,teacher,name in [('A1',a,'teacher','青禾一班'),('A2',a,'teacher','青禾二班'),('A3',a,'teacher2','青禾三班'),('B1',b,'teacherB','远山一班')]:
 req('POST','/sys-class/add',{'orgId':org,'classCode':'IT_'+key,'className':name,'admissionYear':2026,'teacherUserId':users[teacher]['id'],'remark':'前端演示班级'});classes[key]=next(c for c in req('GET','/sys-class/all') if c['classCode']=='IT_'+key)
for i,(key,name) in enumerate([('A1','叶知秋'),('A1','顾星辰'),('A2','苏晨'),('A2','许愿'),('A3','程墨'),('B1','江雨')],1):
 no=prefix+'_S'+str(i);cl=classes[key];req('POST','/sys-user/registration-one',{'orgId':cl['orgId'],'classId':cl['id'],'realName':name,'userNo':no,'password':password,'role':'STUDENT'});users['student'+str(i)]=next(u for u in req('GET','/sys-user/all') if u['userNo']==no);accounts['student'+str(i)]={'userNo':no,'password':password}
for name,color,label,fmt in [('award.png','#f7edcf','ACADEMIC EXCELLENCE','PNG'),('replacement.jpg','#b9dcd5','GROWING TOGETHER','JPEG'),('alternate.webp','#cbd8ed','CREATIVE EXPLORER','WEBP')]:
 im=Image.new('RGB',(800,560),color);d=ImageDraw.Draw(im);d.rectangle((25,25,775,535),outline='#365c4f',width=5);d.rectangle((40,40,760,520),outline='#ae9354',width=2);d.text((260,130),'CERTIFICATE OF ACHIEVEMENT',fill='#284c43');d.text((280,250),label,fill='#284c43');d.text((315,400),'2026 / REWARD COLLECTION',fill='#284c43');im.save(fixtures/name,format=fmt)
(fixtures/'disguised.png').write_bytes(b'This is not an image')
(fixtures/'invalid.txt').write_text('invalid file type')
with (fixtures/'oversized.png').open('wb') as f:f.write(b'0'*(5*1024*1024+1))
for i in range(1,7):
 u=users['student'+str(i)];data={'awardType':'PERSONAL','studentUserId':u['id'],'awardName':['数学探索之星','阅读小达人','科技创新奖','艺术风采奖','体育新星奖','环保行动奖'][i-1],'issuingOrganization':'校园成长委员会','awardDate':'2026-02-28','activityName':'成长记录','awardLevel':'校级','awardGrade':'一等奖','description':'合成演示资料'}
 req('POST','/award-record/add',file={'data':(None,json.dumps(data),'application/json'),'file':('award.png',(fixtures/'award.png').read_bytes(),'image/png')})
for key in classes:
 data={'awardType':'CLASS','classId':classes[key]['id'],'awardName':classes[key]['className']+'文明班级','issuingOrganization':'校园评议组','awardDate':'2024-02-29','description':'集体荣誉 · 演示资料'}
 req('POST','/award-record/add',file={'data':(None,json.dumps(data),'application/json'),'file':('alternate.webp',(fixtures/'alternate.webp').read_bytes(),'image/webp')})
# Second personal award for A/B rapid navigation with visibly different bytes.
u=users['student1'];data={'awardType':'PERSONAL','studentUserId':u['id'],'awardName':'春日创作奖','issuingOrganization':'校园艺术社','awardDate':'2026-02-28'}
req('POST','/award-record/add',file={'data':(None,json.dumps(data),'application/json'),'file':('replacement.jpg',(fixtures/'replacement.jpg').read_bytes(),'image/jpeg')})
# Preserve ISO date encoding; replace only user IDs in original twelve-row mixed fixture.
ns={'x':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
source=OLD/'reward_collection_mysql_testpack/fixtures/excel/students_mixed.xlsx';target=fixtures/'students_mixed.xlsx'
change={'A2':'000'+prefix+'I01','A3':'000'+prefix+'I01','A4':prefix+'IBAD','A5':prefix+'IEMPTY','A6':prefix+'IOTHER','A9':BASE.name.removeprefix('frontend_run_').replace('_','')[2:],'A10':'000'+prefix+'I02'}
with zipfile.ZipFile(source) as src,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as dest:
 for entry in src.infolist():
  content=src.read(entry.filename)
  if entry.filename=='xl/worksheets/sheet1.xml':
   tree=ET.fromstring(content)
   for cell in tree.findall('.//x:c',ns):
    if cell.get('r') in change:
     v=cell.find('x:v',ns) if cell.get('t')!='inlineStr' else cell.find('x:is/x:t',ns);v.text=change[cell.get('r')]
   content=ET.tostring(tree,encoding='utf-8',xml_declaration=True)
  dest.writestr(entry,content)
manifest={'prefix':prefix,'orgA':a,'orgB':b,'classes':classes,'users':users,'awards':req('GET','/award-record/all'),'importIds':[change['A2'],change['A9'],change['A10']]}
# No password hashes are returned by these API entities. Keep account numbers private for tests.
(P/'accounts.json').write_text(json.dumps(accounts,ensure_ascii=False,indent=2));(P/'seed.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
for p in P.iterdir():
 if p.is_file():p.chmod(0o600)
(BASE/'results/seed_summary.json').write_text(json.dumps({'organizations':2,'teachers':3,'classes':4,'students':6,'awards':len(manifest['awards']),'fixture_sha256':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in fixtures.iterdir()}},indent=2))
print('Seeded real API demo data: 2 organizations, 3 teachers, 4 classes, 6 students, 11 awards. Credentials stored privately.')
