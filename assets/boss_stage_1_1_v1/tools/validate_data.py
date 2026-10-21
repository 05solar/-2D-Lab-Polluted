"""Validate data contracts and proxy arena paths; does NOT run the Java game."""
from pathlib import Path
from collections import deque
import json

ROOT=Path(__file__).resolve().parents[1]
def load(name):return json.loads((ROOT/name).read_text(encoding='utf-8'))
def rect(x,y,w,h):return {(a,b) for a in range(x,x+w) for b in range(y,y+h)}
def main():
 stage=load('data/stages.example.json')['stages'];assert [s['stageId'] for s in stage if s['implemented']]==['1-1'];assert [s['stageId'] for s in stage if s['initialProgressUnlocked']]==['1-1'];assert stage[0]['nextStage']=='1-2'
 boss=load('data/boss_patterns.example.json');assert len(boss['patterns'])==3
 for pattern in boss['patterns']:
  events=pattern['events'];assert events==sorted(events,key=lambda e:e['atSeconds']);assert len({e['eventId'] for e in events})==len(events)
  assert pattern['aimLockAt']<=pattern['telegraphSeconds'];assert all(e['atSeconds']>=pattern['telegraphSeconds'] for e in events)
 reward=boss['reward'];assert reward['count']==1 and reward['destination']=='headquartersStorage' and set(reward['receiptKey'])=={'runId','stageId'}
 arena=load('data/arena.example.json');floor=rect(*arena['floorRect'])|rect(*arena['entryReturnGate']['logicalPassageRect']);blocked=set()
 for item in arena['pillars']+arena['barriers']:blocked|=rect(*item['proxyFootprint'])
 start=tuple(arena['playerSpawn']);seen={start};q=deque([start])
 while q:
  x,y=q.popleft()
  for n in [(x+1,y),(x-1,y),(x,y+1),(x,y-1)]:
   if n in floor and n not in blocked and n not in seen:seen.add(n);q.append(n)
 assert tuple(arena['bossSpawn']) in seen;assert tuple(arena['entryReturnGate']['interactionAnchor']) in seen
 assert (16,0) in seen
 anim=load('animations.json')['animations'];total=0
 for id,a in anim.items():
  assert a['count']==a['columns']*a['rows'];assert (ROOT/a['file']).is_file();total+=a['count']
 assert total==34
 return {'stageData':'PASS','patternEventOrdering':'PASS','rewardContract':'PASS','proxyArenaPaths':'PASS','animationPathsAndCounts':'PASS','totalFrames':total,'limitations':['No real actor AABB, Java combat, save or rendering was executed.','Arena uses proxy obstacle footprints.']}
if __name__=='__main__':
 result=main();(ROOT/'DATA_VALIDATION.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print('PASS: stage / 3 patterns / reward contract / proxy paths / 34 animation frames')
