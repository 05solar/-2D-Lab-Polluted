"""Asset/data validation only. Requires Python 3 + Pillow. Not gameplay tests."""
from pathlib import Path
from collections import deque
import json,hashlib,sys
from PIL import Image

BASE=Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parents[1]
def read(f):return json.loads((BASE/f).read_text())
stages=read('data/stage_catalog.json')['stages'];patterns=read('data/patterns.draft.json')['patterns'];anims=read('animations.json')['animations'];arenas=read('data/arena_layouts.json')['arenas']
ids=['1-1','1-2','1-3','1-4','1-5','1-보스','2-1','2-2','2-3']
assert [s['id'] for s in stages]==ids
assert len({s['bossId'] for s in stages})==9
assert all(not s['integrationReady'] for s in stages),'Reference pack cannot claim Java integration'
ps={p['id'] for p in patterns};ars={a['id'] for a in arenas}
hashes=[];checks=0
for i,s in enumerate(stages):
    assert s['previousStage']==(ids[i-1] if i else None)
    assert s['nextStage']==(ids[i+1] if i<8 else None)
    assert set(s['patterns'])<=ps and s['arenaId'] in ars
    assert s['reward']['count']==1 and s['reward']['destination']=='HQ_STORAGE'
    for kind,path in s['portrait'].items():
        im=Image.open(BASE/path);assert im.mode=='RGBA' and im.size==(128,128)
        a=im.getchannel('A');assert a.crop((0,0,128,1)).getextrema()==(0,0) and a.crop((0,127,128,128)).getextrema()==(0,0)
        if kind=='silhouette':hashes.append(hashlib.sha256(im.tobytes()).hexdigest())
    checks+=1
assert len(set(hashes))==9,'Silhouettes must be nine different bosses'
for name,a in anims.items():
    im=Image.open(BASE/a['file']);fw,fh=a['frameSize'];assert im.size==(a['columns']*fw,a['rows']*fh),(name,im.size)
    assert a['count']<=a['columns']*a['rows']
    for seg,indices in a.get('segments',{}).items():assert all(0<=n<a['count'] for n in indices)
    checks+=1
for p in patterns:
    assert p['windupSeconds']>0 and p['activeSeconds']>=0 and p['recoverySeconds']>0
    assert p['cancelOnBossDeath']
    assert 0<=p['targetLockAtSeconds']<=p['windupSeconds']
    if 'subpatterns' in p:assert set(p['subpatterns'])<=ps
    checks+=1
for a in arenas:
    w,h=a['sizeTiles'];b=a['boundaryThickness'];blocked={(x,y) for x in range(w) for y in range(h) if x<b or y<b or x>=w-b or y>=h-b}
    for x,y,cw,ch in a['pillars']:blocked.update((xx,yy) for xx in range(x,x+cw) for yy in range(y,y+ch))
    start=tuple(a['playerSpawn']);goal=tuple(a['southGateInteraction']);boss=tuple(a['bossSpawn'])
    assert start not in blocked and goal not in blocked and boss not in blocked
    q=deque([start]);seen={start}
    while q:
        x,y=q.popleft()
        for t in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)]:
            if 0<=t[0]<w and 0<=t[1]<h and t not in blocked and t not in seen:seen.add(t);q.append(t)
    assert boss in seen and goal in seen
    checks+=1
print(json.dumps({'status':'PASS','checks':checks,'stages':9,'uniqueSilhouettes':9,'arenaPointBfs':9,'scope':'PNG + definition references + draft point-grid reachability only; actual footprint and combat require game integration'},ensure_ascii=False))
