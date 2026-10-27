import os,pathlib,json,subprocess,yaml
BASE=pathlib.Path(os.environ['RC_RUN_DIR']).resolve()
REPO=pathlib.Path(__file__).resolve().parents[3]
assert BASE.name.startswith('frontend_run_'), 'dedicated run directory required'
OLD=BASE.parent/'acceptance_20261027_104708'
JAVA=pathlib.Path('/home/tsar413/tools/opt/jdk-21.0.12.1+1')
MAVEN=pathlib.Path('/home/tsar413/tools/opt/apache-maven-3.9.16')
MYSQL=pathlib.Path('/home/tsar413/tools/opt/mysql-8.0.44-linux-glibc2.17-x86_64')
DB='reward_collection_it_'+BASE.name.replace('frontend_run_','frontend_');PORT=13327;APP_PORT=18127
SOCKET='/tmp/reward_collection_it_20261027_104708.sock'
CREDENTIAL_SOURCE=pathlib.Path(os.environ.get('RC_CREDENTIAL_CONFIG','/home/tsar413/桌面/Java_Project/study-vue-practise-backend/src/main/resources/application.yml'))
def credentials():
 d=yaml.safe_load(CREDENTIAL_SOURCE.read_text())['spring']['datasource'];return str(d['username']),str(d['password'])
def env():
 e=os.environ.copy();e['JAVA_HOME']=str(JAVA);e['PATH']=f'{JAVA}/bin:{MAVEN}/bin:{MYSQL}/bin:'+e['PATH'];e['LD_LIBRARY_PATH']='/home/tsar413/tools/opt/lib';return e
def app_env():
 e=env();u,p=credentials();e.update(SPRING_CONFIG_LOCATION='file:'+str(BASE/'application-it.yml'),SPRING_DATASOURCE_USERNAME=u,SPRING_DATASOURCE_PASSWORD=p);return e
def sql(statement,database=DB):
 u,p=credentials();e=env();e['MYSQL_PWD']=p;args=[str(MYSQL/'bin/mysql'),'--no-defaults','--protocol=SOCKET','--socket='+SOCKET,'-u'+u,'--batch','--skip-column-names']
 if database:args.append(database)
 r=subprocess.run(args,input=statement,text=True,capture_output=True,env=e,timeout=30)
 if r.returncode:raise RuntimeError(r.stderr.replace(p,'[REDACTED]'))
 return r.stdout.strip()
