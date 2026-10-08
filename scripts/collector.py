#!/usr/bin/env python3
"""Small, source-aware collector. Public official sources; no login or paywall handling.

The output is a document catalogue. Dates found in text are unverified candidates.
Usage: python3 scripts/collector.py --output dist/catalog.json [--download-dir ./collected]
"""
import argparse, datetime as dt, hashlib, html, ipaddress, json, pathlib, re, socket, time
import urllib.error, urllib.parse, urllib.request, urllib.robotparser
from html.parser import HTMLParser

ALLOWED={'apis.cebraspe.org.br','cdn.cebraspe.org.br','www.cebraspe.org.br','www.gov.br','download.inep.gov.br','www.concursosfcc.com.br','portal.tce.go.gov.br'}
USER_AGENT='MeridianStudy/1.0 (personal public exam document catalogue; no commercial redistribution)'
SOURCES=[
 {'id':'tcdf-2026','kind':'cebraspe','url':'https://apis.cebraspe.org.br/cebraspe/eventos/TC_DF_26_ANALISTA','base':'https://cdn.cebraspe.org.br/concursos/TC_DF_26_ANALISTA/arquivos/'},
 {'id':'unb-2027','kind':'cebraspe','url':'https://apis.cebraspe.org.br/cebraspe/eventos/VESTUNB_27','base':'https://cdn.cebraspe.org.br/vestibulares/VESTUNB_27/arquivos/'},
 {'id':'tcego-2026','kind':'html','url':'https://www.concursosfcc.com.br/concursos/tcego125/index.html'},
 {'id':'enem-2026','kind':'html','url':'https://www.gov.br/inep/pt-br/areas-de-atuacao/avaliacao-e-exames-educacionais/enem/provas-e-gabaritos/2025'},
]

def validate_url(url, resolve=False):
 p=urllib.parse.urlparse(url)
 if p.scheme!='https' or p.hostname not in ALLOWED or p.username or p.password or p.port not in (None,443):
  raise ValueError('URL outside approved official sources')
 # Fixed official-host allowlist + HTTPS on every redirect is the security boundary.
 # Optional DNS checking is disabled with ordinary proxy-based deployments: the
 # client may intentionally have no direct DNS while HTTPS is handled by a proxy.
 if resolve:
  for addr in socket.getaddrinfo(p.hostname,443,type=socket.SOCK_STREAM):
   if not ipaddress.ip_address(addr[4][0]).is_global: raise ValueError('Non-public address rejected')
 return url

class SafeRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,req,fp,code,msg,headers,newurl):
  validate_url(newurl)
  return super().redirect_request(req,fp,code,msg,headers,newurl)

OPENER=urllib.request.build_opener(SafeRedirect())
ROBOTS={}
LAST_REQUEST={}
def raw_fetch(url,limit=4*1024*1024,headers=None):
 validate_url(url)
 host=urllib.parse.urlparse(url).hostname
 elapsed=time.monotonic()-LAST_REQUEST.get(host,0)
 if elapsed<1.5:time.sleep(1.5-elapsed)
 req=urllib.request.Request(url,headers={'User-Agent':USER_AGENT,**(headers or {})})
 LAST_REQUEST[host]=time.monotonic()
 with OPENER.open(req,timeout=20) as response:
  if int(response.headers.get('Content-Length','0'))>limit:raise ValueError('File exceeds byte limit')
  content=response.read(limit+1)
  if len(content)>limit:raise ValueError('File exceeds byte limit')
  return content,dict(response.headers),response.geturl()

def allowed_by_robots(url):
 origin=urllib.parse.urlsplit(url);key=origin.scheme+'://'+origin.netloc
 if key not in ROBOTS:
  parser=urllib.robotparser.RobotFileParser()
  try:
   data,_,_=raw_fetch(key+'/robots.txt',512000)
   parser.parse(data.decode('utf-8','replace').splitlines());ROBOTS[key]=parser
  except urllib.error.HTTPError as ex:
   if ex.code==404:ROBOTS[key]=True
   else:ROBOTS[key]=False
  except Exception:ROBOTS[key]=False
 rule=ROBOTS[key]
 return rule if isinstance(rule,bool) else rule.can_fetch(USER_AGENT,url)

def fetch(url,limit=4*1024*1024):
 if not allowed_by_robots(url):raise ValueError('robots.txt disallows collection or could not be verified')
 return raw_fetch(url,limit)

class Links(HTMLParser):
 def __init__(self):super().__init__();self.links=[];self.current=None;self.text=[]
 def handle_starttag(self,tag,attrs):
  if tag=='a':self.current=dict(attrs).get('href');self.text=[]
 def handle_data(self,data):
  if self.current:self.text.append(data)
 def handle_endtag(self,tag):
  if tag=='a' and self.current:self.links.append((self.current,' '.join(self.text)));self.current=None

def api_links(value):
 if isinstance(value,dict):
  if value.get('nomeArquivo'):yield value
  for v in value.values():yield from api_links(v)
 elif isinstance(value,list):
  for v in value:yield from api_links(v)

def collect(download_dir=None):
 now=dt.datetime.now(dt.timezone.utc).isoformat();materials=[];reports=[];seen=set()
 for source in SOURCES:
  report={'url':source['url'],'checkedAt':now}
  try:
   data,headers,actual=fetch(source['url']);report.update(status='collected',sha256=hashlib.sha256(data).hexdigest(),url=actual,bytes=len(data))
   if source['kind']=='cebraspe':
    payload=json.loads(data);candidates=[(urllib.parse.urljoin(source['base'],x['nomeArquivo']),x.get('descricaoArquivo','Documento oficial')) for x in api_links(payload)]
   else:
    parser=Links();parser.feed(data.decode('utf-8','replace'));candidates=[(urllib.parse.urljoin(actual,u),t) for u,t in parser.links]
   for url,title in candidates:
    if not urllib.parse.urlsplit(url).path.lower().endswith('.pdf') or url in seen:continue
    try:validate_url(url,False)
    except ValueError:continue
    seen.add(url)
    title=html.unescape(re.sub(r'\s+',' ',title)).strip()
    if title.lower() in ('prova','gabarito'):
     title='Enem 2025 · '+title+' · '+pathlib.PurePosixPath(urllib.parse.urlsplit(url).path).name.removesuffix('.pdf')
    m={'id':hashlib.sha256(url.encode()).hexdigest()[:18],'title':html.unescape(re.sub(r'\s+',' ',title)).strip()[:250] or pathlib.PurePosixPath(urllib.parse.urlsplit(url).path).name,'url':url,'kind':'Documento oficial · coletado','examIds':[source['id']],'collectedAt':now,'sourceUrl':actual,'status':'needs_review'}
    materials.append(m)
   report['documents']=sum(source['id'] in m['examIds'] for m in materials)
  except Exception as error:report.update(status='unavailable',error=str(error)[:250])
  reports.append(report)
 if download_dir:
  directory=pathlib.Path(download_dir);directory.mkdir(parents=True,exist_ok=True)
  # Explicit capped download; no mass crawl or credentials.
  for m in materials[:6]:
   try:
    data,headers,_=fetch(m['url'],15*1024*1024)
    if not data.startswith(b'%PDF-'):raise ValueError('Response is not a PDF')
    sha=hashlib.sha256(data).hexdigest();file=directory/(sha+'.pdf');file.write_bytes(data);m.update(sha256=sha,downloadedFile=file.name,bytes=len(data))
   except Exception as error:m['downloadError']=str(error)[:180]
 return {'format':'meridian-catalog','version':1,'collectedAt':now,'materials':materials,'candidates':[],'reports':reports,'notice':'Catalogue only. No exam date is verified or changed by this collector. Read each notice and its amendments.'}

def main():
 p=argparse.ArgumentParser();p.add_argument('--output',default='catalog-collected.json');p.add_argument('--download-dir');a=p.parse_args();result=collect(a.download_dir);out=pathlib.Path(a.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps({'output':str(out),'documents':len(result['materials']),'reports':result['reports']},ensure_ascii=False))
if __name__=='__main__':main()
