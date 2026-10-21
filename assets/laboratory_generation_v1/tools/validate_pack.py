from pathlib import Path
import json,argparse
from reference_generator import create,RNG

def validate(seeds=25):
 root=Path(__file__).resolve().parents[1];results=[]
 for difficulty in ['easy','normal','hard']:
  for template_index,path in enumerate(sorted((root/'data/layouts'/difficulty).glob('*.json'))):
   lengths=[];hashes=set();minprops=1000;retried=0;fallbacks=0;first=1001+template_index*seeds
   for seed in range(first,first+seeds):
    m=create(difficulty,seed,path.stem);again=create(difficulty,seed,path.stem) if seed==first else None
    assert again is None or again['contentHash']==m['contentHash']
    assert len(m['rooms'])=={'easy':16,'normal':25,'hard':36}[difficulty]
    assert len({r['theme'] for r in m['rooms']})==16
    assert all(x['type'] not in ['guard','manager'] for x in m['monsters']) if difficulty=='easy' else all(x['cornerValid'] for x in m['monsters'] if x['type']=='manager')
    assert all(m['validation'][k]>0 for k in ['roomsReachable','containersReachable','doorsOpenedByReachableApproach'])
    lengths.extend(c['length'] for c in m['corridors']);hashes.add(m['contentHash']);minprops=min(minprops,min(r['majorPropCount'] for r in m['rooms']));retried+=int(m['validation']['attemptCount']>1);fallbacks+=int(m['validation']['fallbackUsed'])
   results.append({'templateId':path.stem,'seedsTested':seeds,'seedRange':[first,first+seeds-1],'passed':True,'distinctHashes':len(hashes),'maxConnectorLength':max(lengths),'meanConnectorLength':round(sum(lengths)/len(lengths),2),'minimumMajorPropsInAnyRoom':minprops,'casesWithRetry':retried,'casesWithFallback':fallbacks})
 r=RNG(1001,'topology');golden=[r.next() for _ in range(8)]
 return {'status':'PASS','templates':len(results),'totalCases':len(results)*seeds,'uniqueSeedsPerDifficulty':seeds*4,'caseSeedRange':[1001,1000+seeds*4],'rngGoldenVector':{'seed':'1001','scope':'topology','first8Unsigned32':golden},'results':results,'limits':['Proxy footprints and tile-center path checks only.','Real sprite regions, anchors, actor AABB, Java game and runtime rendering are not tested.']}

if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--seeds-per-template',type=int,default=25);parser.add_argument('--output',default=str(Path(__file__).resolve().parents[1]/'VALIDATION_REPORT.json'));args=parser.parse_args();result=validate(args.seeds_per_template);Path(args.output).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(result['status'],result['totalCases'],'cases /',result['templates'],'templates')
