#!/usr/bin/env python3
"""One-off probe: which public exam-board pages respond, and what do they link to?
Respects robots.txt, one GET per page, prints only a short summary."""
import re, socket, sys, urllib.request, urllib.robotparser, urllib.parse
from html.parser import HTMLParser
UA='MeridianStudy/1.0 (personal public exam document catalogue; no commercial redistribution)'
socket.setdefaulttimeout(15)
URLS=sys.argv[1:]
class P(HTMLParser):
 def __init__(s):super().__init__();s.title='';s.t=False;s.links=[];s.cur=None;s.txt=[]
 def handle_starttag(s,tag,a):
  a=dict(a)
  if tag=='title':s.t=True
  if tag=='a' and a.get('href'):s.cur=a['href'];s.txt=[]
 def handle_endtag(s,tag):
  if tag=='title':s.t=False
  if tag=='a' and s.cur:s.links.append((s.cur,' '.join(s.txt).strip()));s.cur=None
 def handle_data(s,d):
  if s.t:s.title+=d
  if s.cur:s.txt.append(d)
for url in URLS:
 try:
  o=urllib.parse.urlsplit(url);rp=urllib.robotparser.RobotFileParser();rp.set_url(f'{o.scheme}://{o.netloc}/robots.txt')
  try:rp.read()
  except Exception:pass
  if not rp.can_fetch(UA,url):print(url,'-> robots.txt disallows',flush=True);continue
  r=urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':UA}),timeout=25);b=r.read(2_000_000).decode('utf-8','replace')
  p=P();p.feed(b);keep=[(urllib.parse.urljoin(url,h),re.sub(r'\s+',' ',t)[:70]) for h,t in p.links if re.search(r'edital|concurso|prova|gabarito|\.pdf',h+t,re.I)]
  print(f'\n== {url} -> {r.status} {r.geturl()}\n   title: {re.sub(chr(10)," ",p.title).strip()[:100]}  bytes={len(b)} links={len(p.links)} relevant={len(keep)}')
  for h,t in keep[:25]:print('   ',t,'|',h[:150])
  sys.stdout.flush()
 except Exception as e:print(f'\n== {url} -> ERROR {str(e)[:120]}')
