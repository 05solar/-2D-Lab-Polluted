"""Pure standard-library reference for the supplied abstract layout assets.
Uses proxy footprints, not the real game's sprite metadata/actor collider.
Run validate_pack.py for checks. The game should implement its own Java adapter.
"""
from pathlib import Path
from collections import deque
import hashlib,json

ROOT=Path(__file__).resolve().parents[1]
DIRS=[(1,0),(-1,0),(0,1),(0,-1)]
def read(name):return json.loads((ROOT/name).read_text(encoding='utf-8'))
class RNG:
 def __init__(self,seed,scope):
  h=2166136261
  for b in (str(seed)+'|'+scope).encode('utf-8'):h=((h^b)*16777619)&0xffffffff
  self.state=h or 0x6d2b79f5
 def next(self):
  x=self.state;x^=(x<<13)&0xffffffff;x^=x>>17;x^=(x<<5)&0xffffffff;self.state=x&0xffffffff;return self.state
 def pick(self,items):return items[self.next()%len(items)]
 def shuffle(self,items):
  for i in range(len(items)-1,0,-1):j=self.next()%(i+1);items[i],items[j]=items[j],items[i]
  return items
 def weighted(self,values):
  n=self.next()%sum(w for _,w in values)
  for name,w in values:
   if n<w:return name
   n-=w

def rect(x,y,w,h):return {(a,b) for a in range(x,x+w) for b in range(y,y+h)}
def neighbors(p):return [(p[0]+dx,p[1]+dy) for dx,dy in DIRS]
def route(floor,blocked,start,goal):
 if start in blocked or goal in blocked:return None
 prev={start:None};q=deque([start])
 while q:
  p=q.popleft()
  if p==goal:
   path=[]
   while p is not None:path.append(p);p=prev[p]
   return path
  for n in neighbors(p):
   if n in floor and n not in blocked and n not in prev:prev[n]=p;q.append(n)
 return None
def flood(floor,blocked,start,doors=()):
 doorcells=set(doors);opened=[];seen={start};q=deque([start])
 if start not in floor or start in blocked:return set(),[]
 while q:
  p=q.popleft()
  for n in neighbors(p):
   if n in floor and n not in blocked and n not in seen:
    # An adjacent reachable tile permits opening this ordinary Space door.
    if n in doorcells:opened.append(n)
    seen.add(n);q.append(n)
 return seen,opened

def create_once(difficulty,seed,template_id=None):
 profiles=read('data/difficulty_profiles.json')['profiles'];profile=profiles[difficulty]
 paths=sorted((ROOT/'data/layouts'/difficulty).glob('*.json'))
 if template_id:paths=[p for p in paths if p.stem==template_id]
 if not paths:raise ValueError('Unknown template')
 layout=json.loads(RNG(seed,'template').pick(paths).read_text(encoding='utf-8'))
 themes=read('data/room_themes.json')['themes'];patterns=read('data/furniture_patterns.json');roles=patterns['roleProxyFootprints']
 slots=sorted(layout['roomSlots'],key=lambda p:(p['grid'][1],p['grid'][0]));lookup={tuple(s['grid']):s['roomId'] for s in slots}
 edges=[]
 for (x,y),id in sorted(lookup.items()):
  for dx,dy in [(1,0),(0,1)]:
   if (x+dx,y+dy) in lookup:edges.append([id,lookup[(x+dx,y+dy)]])
 rng=RNG(seed,'topology');order=rng.shuffle(edges.copy());parent={s['roomId']:s['roomId'] for s in slots}
 def find(a):
  while parent[a]!=a:a=parent[a]
  return a
 chosen=[];extra=[]
 for a,b in order:
  if find(a)!=find(b):parent[find(a)]=find(b);chosen.append([a,b])
  else:extra.append([a,b])
 for edge in extra:
  if rng.next()%100<profile['extraEdgePercent']:chosen.append(edge)
 if len({find(x) for x in parent})!=1:raise ValueError('Disconnected template grid')
 px,py=layout['gridPitch'];w,h=layout['roomInteriorSize'];maxgx=max(s['grid'][0] for s in slots);minx=(layout['mapSize'][0]-(maxgx*px+w))//2
 rooms={};floor=set();reserved=set()
 for s in slots:
  id=s['roomId'];gx,gy=s['grid'];r=RNG(seed,'shape:'+id);x=minx+gx*px+(r.next()%3-1);y=9+gy*py+(r.next()%3-1)
  shape=r.pick(layout['allowedShapes']);f=rect(x,y,w,h)
  if shape=='notch_ne':f-=rect(x+w-5,y+h-3,5,3)
  elif shape=='notch_sw':f-=rect(x,y,5,3)
  elif shape=='stepped':f-=rect(x,y+h-2,3,2);f-=rect(x+w-3,y,3,2)
  anchor=(x+w//2,y+h//2);rooms[id]={'id':id,'grid':[gx,gy],'origin':[x,y],'size':[w,h],'shape':shape,'floor':f,'anchor':anchor,'doors':[]};floor|=f;reserved|={p for p in f if p[0]==anchor[0] or p[1]==anchor[1]}
 doors={};corridors=[]
 def socket(room,side):
  x,y=room['origin'];a,b=room['size'];return {'N':(x+a//2,y+b),'S':(x+a//2,y-1),'W':(x-1,y+b//2),'E':(x+a,y+b//2)}[side]
 def add_door(room,side):
  p=socket(room,side);doors[p]={'roomId':room['id'],'side':side,'openableFromEitherSide':True};room['doors'].append(p)
  reserved.update({q for q in room['floor'] if abs(q[0]-p[0])+abs(q[1]-p[1])<=3});return p
 for a,b in chosen:
  ra,rb=rooms[a],rooms[b];horizontal=ra['grid'][1]==rb['grid'][1]
  if horizontal:
   if ra['grid'][0]>rb['grid'][0]:ra,rb=rb,ra
   p=add_door(ra,'E');q=add_door(rb,'W');mid=(p[0]+q[0])//2
   path=[(x,p[1]) for x in range(p[0],mid+1)]+[(mid,y) for y in range(min(p[1],q[1]),max(p[1],q[1])+1)]+[(x,q[1]) for x in range(mid,q[0]+1)]
  else:
   if ra['grid'][1]>rb['grid'][1]:ra,rb=rb,ra
   p=add_door(ra,'N');q=add_door(rb,'S');mid=(p[1]+q[1])//2
   path=[(p[0],y) for y in range(p[1],mid+1)]+[(x,mid) for x in range(min(p[0],q[0]),max(p[0],q[0])+1)]+[(q[0],y) for y in range(mid,q[1]+1)]
  carved=set()
  for point in path:carved|={point,*neighbors(point)}
  # Keep the boundary wall closed except for each explicit 1-cell door.
  for room,door in [(ra,p),(rb,q)]:
   shell={n for cell in room['floor'] for n in neighbors(cell)}-room['floor'];carved-=shell-{door}
  floor|=carved;reserved|=carved;corridors.append({'rooms':[ra['id'],rb['id']],'centerline':path,'length':len(set(path))})
 # A single south-facing headquarters gate connects to one southern entry room.
 lowest=min(r['grid'][1] for r in rooms.values());entry=min((r for r in rooms.values() if r['grid'][1]==lowest),key=lambda r:abs(r['anchor'][0]-layout['mapSize'][0]//2));dp=add_door(entry,'S');gate=(dp[0],3)
 passage={(x,y) for x in range(gate[0]-1,gate[0]+2) for y in range(3,dp[1]+1)}
 shell={n for c in entry['floor'] for n in neighbors(c)}-entry['floor'];passage-=shell-{dp};floor|=passage;reserved|=passage
 remaining=RNG(seed,'themes').shuffle([t for t in themes if t['id']!='entry']);room_list=sorted(rooms.values(),key=lambda r:r['id']);index=0
 for room in room_list:
  if room is entry:room['theme']='entry'
  else:room['theme']=remaining[index%len(remaining)]['id'];index+=1
 props=[];containers=[];blocked=set();manager_reserve=set()
 theme_by={t['id']:t for t in themes}
 def corner(p,f):
  axes=[]
  for dx,dy in DIRS:axes.append(any((p[0]+dx*d,p[1]+dy*d) not in f for d in [1,2]))
  return (axes[0] or axes[1]) and (axes[2] or axes[3])
 def wall_candidates(room):
  x,y=room['origin'];a,b=room['size'];out=[]
  for n in range(1,a-1):out.extend([(x+n,y,'S'),(x+n,y+b-1,'N')])
  for n in range(1,b-1):out.extend([(x,y+n,'W'),(x+a-1,y+n,'E')])
  return out
 for room in room_list:
  id=room['id'];f=room['floor'];r=RNG(seed,'loot:'+id)
  if difficulty!='easy':
   c=sorted(p for p in f-reserved if corner(p,f))
   if c:manager_reserve.add(RNG(seed,'manager_reserve:'+id).pick(c))
  candidates=wall_candidates(room);r.shuffle(candidates);candidates.sort(key=lambda c:0 if corner((c[0],c[1]),f) else 1)
  target=profile['cabinetsPerRoom']
  for x,y,side in candidates:
   if sum(c['roomId']==id for c in containers)>=target:break
   p=(x,y);dx,dy={'N':(0,-1),'S':(0,1),'W':(1,0),'E':(-1,0)}[side];approach=(x+dx,y+dy)
   if p not in f or approach not in f or p in reserved|blocked|manager_reserve or approach in blocked|manager_reserve:continue
   if any(abs(x-c['position'][0])+abs(y-c['position'][1])<3 for c in containers if c['roomId']==id):continue
   access_path=route(f,blocked|{p},approach,room['anchor'])
   if access_path is None:continue
   containers.append({'id':id+':loot:'+str(sum(c['roomId']==id for c in containers)),'roomId':id,'position':p,'side':side,'approach':approach,'drop':approach});blocked.add(p);reserved.update(access_path)
  for point in manager_reserve&f:
   path=route(f,blocked,point,room['anchor'])
   if path:reserved.update(path)
  r=RNG(seed,'furniture:'+id);pattern=r.pick(patterns['patterns']);room['furniturePattern']=pattern['id'];possible=r.shuffle(wall_candidates(room));count=0;theme_roles=theme_by[room['theme']]['wallRoles']
  for x,y,side in possible:
   if count>=pattern['majorPropTarget']:break
   role=r.pick(theme_roles);a,b=roles[role]
   if side=='N':fp=rect(x,y-b+1,a,b);support={(xx,y+1) for xx in range(x,x+a)}
   elif side=='S':fp=rect(x,y,a,b);support={(xx,y-1) for xx in range(x,x+a)}
   elif side=='W':fp=rect(x,y,b,a);support={(x-1,yy) for yy in range(y,y+a)}
   else:fp=rect(x-b+1,y,b,a);support={(x+1,yy) for yy in range(y,y+a)}
   if not fp<=f or fp&(blocked|reserved|manager_reserve) or support&f:continue
   props.append({'id':id+':prop:'+str(count),'roomId':id,'role':role,'side':side,'proxyCells':sorted(fp)});blocked|=fp;count+=1
  if pattern['islands'] or count<patterns['minimumMajorPropsPerRoom']:
   ax,ay=room['anchor']
   island_role=theme_by[room['theme']]['islandRole'];iw,ih=roles[island_role]
   added=0
   for off,dy in [(-5,-3),(2,-3),(-5,2),(2,2),(-4,-2),(3,-2)]:
    if added>=2 and count+added>=patterns['minimumMajorPropsPerRoom']:break
    fp=rect(ax+off,ay+dy,iw,ih)
    if fp<=f and not fp&(blocked|reserved|manager_reserve):props.append({'id':id+':island:'+str(added),'roomId':id,'role':island_role,'side':'N','proxyCells':sorted(fp)});blocked|=fp;added+=1
  room['majorPropCount']=sum(p['roomId']==id for p in props)
 # Above we only reserved future container sockets. Finalize container data after furniture.
 containers=[{**c,'tier':RNG(seed,'container:'+c['id']).weighted(profile['containerTierWeights'])} for c in containers]
 reached,opened=flood(floor,blocked,gate,doors)
 for room in room_list:
  if room['anchor'] not in reached:raise ValueError('room unreachable: '+room['id'])
  if room['majorPropCount']<patterns['minimumMajorPropsPerRoom']:raise ValueError('insufficient props: '+room['id']+' '+room['theme']+' count='+str(room['majorPropCount']))
 for c in containers:
  if c['approach'] not in reached or c['drop'] not in reached:raise ValueError('loot unreachable: '+c['id'])
 for p in doors:
  if p not in reached or not any(n in reached and n not in doors for n in neighbors(p)):raise ValueError('door unreachable')
 if len(opened)!=len(doors):raise ValueError('unopened door')
 if len(containers)!=len(room_list)*profile['cabinetsPerRoom']:raise ValueError('container target missed')
 monsters=[];mr=RNG(seed,'monsters');eligible=sorted(p for r in room_list for p in r['floor'] if p in reached and p not in blocked and min(abs(p[0]-gate[0])+abs(p[1]-gate[1]),1000)>=8 and not any(abs(p[0]-d[0])+abs(p[1]-d[1])<=2 for d in doors))
 manager_candidates=sorted(p for p in manager_reserve if p in eligible);mr.shuffle(eligible);mr.shuffle(manager_candidates);used=set()
 forced=profile['minimumSpecies'];species=forced+[mr.weighted(profile['monsterWeights']) for _ in range(profile['monsterCount']-len(forced))]
 for typ in species:
  pool=manager_candidates if typ=='manager' else eligible
  p=next((p for p in pool if p not in used and all(abs(p[0]-q[0])+abs(p[1]-q[1])>=2 for q in used)),None)
  if p is None:raise ValueError('monster pool exhausted: '+typ)
  used.add(p);room=next(r for r in room_list if p in r['floor']);monsters.append({'id':'monster:'+str(len(monsters)),'type':typ,'position':p,'roomId':room['id'],'cornerValid':corner(p,room['floor']) if typ=='manager' else None})
 walls={q for p in floor for q in neighbors(p) if q not in floor};maxw,maxh=layout['mapSize']
 if not all(0<=x<maxw and 0<=y<maxh for x,y in floor|walls):raise ValueError('map out of bounds')
 serial={'generatorVersion':1,'templateVersion':1,'difficulty':difficulty,'seed':str(seed),'templateId':layout['templateId'],'mapSize':layout['mapSize'],'gate':gate,'entryRoomId':entry['id'],'rooms':[{k:(sorted(v) if k=='floor' else v) for k,v in r.items()} for r in room_list],'floor':sorted(floor),'walls':sorted(walls),'doors':[{'position':p,**d} for p,d in sorted(doors.items())],'corridors':corridors,'props':props,'containers':containers,'monsters':monsters,'validation':{'roomsReachable':len(rooms),'containersReachable':len(containers),'doorsOpenedByReachableApproach':len(opened),'walkableCells':len(reached),'proxyFootprintsOnly':True}}
 serial['contentHash']=hashlib.sha256(json.dumps(serial,sort_keys=True,separators=(',',':')).encode()).hexdigest();return serial

def create(difficulty,seed,template_id=None):
 failures=[];model=None;actual_seed=seed
 for attempt in range(3):
  actual_seed=seed if attempt==0 else RNG(seed,'retry:'+str(attempt)).next()
  try:model=create_once(difficulty,actual_seed,template_id);break
  except ValueError as error:failures.append(str(error))
 fallback=model is None
 if fallback:
  actual_seed=1001;model=create_once(difficulty,actual_seed,difficulty+'_compact')
 model['seed']=str(seed);model['generationSeed']=str(actual_seed);model['validation'].update({'attemptCount':len(failures)+1,'fallbackUsed':fallback,'attemptFailures':failures,'requestedTemplateId':template_id})
 model.pop('contentHash',None);model['contentHash']=hashlib.sha256(json.dumps(model,sort_keys=True,separators=(',',':')).encode()).hexdigest();return model

if __name__=='__main__':
 import argparse
 parser=argparse.ArgumentParser();parser.add_argument('--difficulty',choices=['easy','normal','hard'],default='easy');parser.add_argument('--seed',default='1001');parser.add_argument('--template');parser.add_argument('--output',required=True);args=parser.parse_args();result=create(args.difficulty,args.seed,args.template);Path(args.output).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(result['templateId'],result['contentHash'])
