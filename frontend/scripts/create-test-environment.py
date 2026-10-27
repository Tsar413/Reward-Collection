"""Create a new run folder; never reuse a database or stop an existing service."""
from pathlib import Path
import datetime,subprocess,os,sys
repo=Path(__file__).resolve().parents[2]
base=repo.parent/('frontend_run_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S'))
base.mkdir(exist_ok=False)
for part in ['logs','results','uploads']:(base/part).mkdir()
template=Path(__file__).resolve().parent/'environment'
s=(template/'application-it.example.yml').read_text().replace('reward_collection_it_frontend_20261027_153619','reward_collection_it_'+base.name.replace('frontend_run_','frontend_'))
import re
s=re.sub(r'    root: .*', '    root: '+str(base/'uploads'),s)
(base/'application-it.yml').write_text(s)
e=os.environ.copy();e['RC_RUN_DIR']=str(base)
print('New isolated environment:',base,flush=True)
# Run all commands from the same Linux VM; existing sockets cause a safe refusal.
for script in ['prepare.py','seed.py']:
 subprocess.run([sys.executable,str(template/script)],env=e,check=True)
for name in ['probe.py','stop.py']:
 (base/name).write_text("import os,runpy\nos.environ['RC_RUN_DIR']="+repr(str(base))+"\nimport sys\nsys.path.insert(0,"+repr(str(template))+")\nrunpy.run_path("+repr(str(template/name))+",run_name='__main__')\n")
print('Backend is ready. Start frontend with npm run dev. Set RUN_DIR='+str(base)+' for Playwright.')
