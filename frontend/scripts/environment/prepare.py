from runtime import *
import time,hashlib,requests,shutil
repo=REPO;expected='59c4d3d8df5d13e8748c2eb83601b6473bb8ca62'
sha=subprocess.check_output(['git','rev-parse','HEAD'],cwd=repo,text=True).strip()
assert sha==expected,'unexpected revision'
assert not subprocess.check_output(['git','diff','HEAD','--','src','pom.xml'],cwd=repo,text=True).strip(),'backend modified'
hashes={str(p.relative_to(repo)):hashlib.sha256(p.read_bytes()).hexdigest() for p in repo.rglob('*') if p.is_file() and '.git' not in p.parts and 'frontend' not in p.parts}
(BASE/'results/source_before.json').write_text(json.dumps({'commit':sha,'branch':'main','status':'clean','hashes':hashes},indent=2))
versions=[]
for cmd in [[str(JAVA/'bin/java'),'-version'],[str(MAVEN/'bin/mvn'),'-v'],[str(MYSQL/'bin/mysqld'),'--version']]:
 p=subprocess.run(cmd,env=env(),capture_output=True,text=True,check=True);versions.append({'cmd':cmd,'output':p.stdout+p.stderr})
(BASE/'logs/versions.json').write_text(json.dumps(versions,ensure_ascii=False,indent=2))
# Resume original dedicated instance; never reinitialize its data directory.
assert (OLD/'mysql-data/auto.cnf').exists()
assert not pathlib.Path(SOCKET).exists(),'socket already exists: inspect ownership before reuse'
args=[str(MYSQL/'bin/mysqld'),'--no-defaults','--basedir='+str(MYSQL),'--datadir='+str(OLD/'mysql-data'),'--socket='+SOCKET,'--port='+str(PORT),'--bind-address=127.0.0.1','--mysqlx=OFF','--skip-log-bin','--innodb-buffer-pool-size=128M','--pid-file='+str(BASE/'mysql.pid'),'--log-error='+str(BASE/'logs/mysql-server.log')]
p=subprocess.Popen(args,env=env(),stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,start_new_session=True)
for i in range(100):
 try:proof=sql('SELECT VERSION(),@@version_comment,@@server_uuid;',None);break
 except RuntimeError:
  if p.poll() is not None:raise RuntimeError('MySQL start failed')
  time.sleep(.2)
else:raise RuntimeError('MySQL not ready')
assert proof.startswith('8.0.44\t') and 'MariaDB' not in proof
old_db='reward_collection_it_20261027_104708'
old_counts=sql("SELECT 'sys_org',COUNT(*) FROM sys_org UNION ALL SELECT 'sys_user',COUNT(*) FROM sys_user UNION ALL SELECT 'sys_class',COUNT(*) FROM sys_class UNION ALL SELECT 'award_record',COUNT(*) FROM award_record UNION ALL SELECT 'award_image',COUNT(*) FROM award_image UNION ALL SELECT 'sys_login_token',COUNT(*) FROM sys_login_token;",old_db)
(BASE/'results/old_database_counts_before.json').write_text(json.dumps({'database':old_db,'counts':old_counts},indent=2))
assert not sql("SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='"+DB+"';",None),'refuse existing test DB'
sql('CREATE DATABASE `'+DB+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;',None)
(BASE/'results/mysql_identity.json').write_text(json.dumps({'identity_query':'SELECT VERSION(),@@version_comment,@@server_uuid','identity':proof,'database':sql('SELECT DATABASE();'),'datadir':str(OLD/'mysql-data'),'default_engine':sql('SELECT @@default_storage_engine;')},ensure_ascii=False,indent=2))
print('real_mysql_ready',proof.split('\t')[0],DB,flush=True)
for phase,args in [('tests',['test']),('package',['package','-DskipTests'])]:
 with (BASE/'logs'/f'{phase}.log').open('w') as log:
  r=subprocess.run([str(MAVEN/'bin/mvn'),'-B','-Dmaven.repo.local='+str(OLD/'maven-repository'),*args],cwd=repo,env=app_env(),stdout=log,stderr=log)
 print(phase,r.returncode,flush=True)
 if r.returncode:raise SystemExit(r.returncode)
classes=BASE/'initializer-classes';classes.mkdir()
shutil.copy2(pathlib.Path(__file__).with_name('Bootstrap.java'),BASE/'Bootstrap.java')
driver=next((OLD/'maven-repository/com/mysql/mysql-connector-j').rglob('mysql-connector-j-*.jar'))
cp=str(repo/'target/classes')+':'+str(driver)
import secrets
private=repo/'frontend/evidence/private';private.mkdir(parents=True,exist_ok=True);private.chmod(0o700)
demo_password=os.environ.get('RC_DEMO_PASSWORD') or secrets.token_urlsafe(18)
account_file=private/'accounts.json';account_file.write_text(json.dumps({'admin':{'userNo':'IT_ADM_A','password':demo_password}}));account_file.chmod(0o600)
e=app_env();e.update(IT_DB_NAME=DB,IT_JDBC_URL=f'jdbc:mysql://127.0.0.1:{PORT}/{DB}?useSSL=false&allowPublicKeyRetrieval=true',IT_TEST_PASSWORD=demo_password)
with (BASE/'logs/initialization.log').open('w') as log:
 subprocess.run([str(JAVA/'bin/javac'),'-proc:none','-cp',cp,'-d',str(classes),str(BASE/'Bootstrap.java')],env=e,stdout=log,stderr=log,check=True)
 subprocess.run([str(JAVA/'bin/java'),'-cp',cp+':'+str(classes),'Bootstrap'],env=e,stdout=log,stderr=log,check=True)
with (BASE/'logs/application.log').open('w') as log:p=subprocess.Popen([str(JAVA/'bin/java'),'-jar',str(repo/'target/reward-collection.jar')],cwd=BASE,env=app_env(),stdout=log,stderr=log,start_new_session=True)
(BASE/'app.pid').write_text(str(p.pid));s=requests.Session();s.trust_env=False
for i in range(120):
 try:
  r=s.get(f'http://127.0.0.1:{APP_PORT}/api/sys-login/check',timeout=1)
  if r.status_code==401:break
 except requests.RequestException:pass
 if p.poll() is not None:raise RuntimeError('Application exited')
 time.sleep(.25)
else:raise RuntimeError('app readiness timeout')
print('app_ready',r.status_code,flush=True)
(BASE/'results/table_engines.txt').write_text(sql('SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE();'))
