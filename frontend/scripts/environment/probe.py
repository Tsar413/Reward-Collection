from runtime import *
import sys,hashlib,json,shutil
mode=sys.argv[1]
assert sql('SELECT DATABASE();')==DB and DB.startswith('reward_collection_it_frontend_')
if mode=='query':
 q=sys.argv[2];assert q.lstrip().upper().startswith('SELECT ') and not any(k in q.lower() for k in ['password','salt','token_hash']);print(sql(q))
elif mode in ['image','hide','directory','restore']:
 aid=int(sys.argv[2]);raw=sql(f"SELECT JSON_OBJECT('id',id,'award_id',award_id,'storage_key',storage_key,'mime_type',mime_type,'file_size',file_size,'width',image_width,'height',image_height) FROM award_image WHERE award_id={aid};");assert raw,'image missing';d=json.loads(raw);p=(BASE/'uploads'/d['storage_key']).resolve();assert p.is_relative_to((BASE/'uploads').resolve());backup=BASE/f'fault-backup-{aid}'
 if mode in ['hide','directory']:
  assert p.is_file() and not backup.exists();p.rename(backup)
  if mode=='directory':p.mkdir()
 elif mode=='restore':
  if backup.exists():
   if p.is_dir():p.rmdir()
   backup.rename(p)
 d['exists']=p.is_file();d['sha256']=hashlib.sha256(p.read_bytes()).hexdigest() if p.is_file() else None;print(json.dumps(d))
