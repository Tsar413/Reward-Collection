from runtime import *
import signal,time
for name in ['frontend.pid','app.pid','mysql.pid']:
 p=BASE/name
 if not p.exists():continue
 pid=int(p.read_text());proc=pathlib.Path('/proc')/str(pid)
 if not proc.exists():continue
 cmd=(proc/'cmdline').read_bytes().replace(b'\0',b' ').decode(errors='replace')
 if name=='frontend.pid':assert 'npm run dev' in cmd and (proc/'cwd').resolve()==REPO/'frontend'
 else:assert str(BASE) in cmd or str(REPO/'target/reward-collection.jar') in cmd
 if name=='frontend.pid':os.killpg(pid,signal.SIGTERM)
 else:os.kill(pid,signal.SIGTERM)
 print(name,'stop requested for owned PID',pid)
