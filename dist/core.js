/* Meridian core: deterministic, dependency-free, browser + Node. */
(function(root){
'use strict';
const day=86400000;
const isoDay=(date=new Date())=>new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).format(date);
const parseDay=s=>new Date(s.slice(0,10)+'T12:00:00-03:00');
const addDays=(s,n)=>isoDay(new Date(parseDay(s).getTime()+n*day));
const daysUntil=(s,t=isoDay())=>Math.round((parseDay(s)-parseDay(t))/day);
const normalize=s=>String(s||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
const uid=()=>typeof crypto!=='undefined'&&crypto.randomUUID?crypto.randomUUID():'id-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2);
function rankTopics(topics,state,exams,today=isoDay()){
 return topics.filter(t=>!state.hiddenTopics?.includes(t.id)).map(t=>{
  const p=state.progress[t.id]||{}; const targeted=t.exams.filter(e=>state.targets.includes(e));
  const dates=exams.filter(e=>targeted.includes(e.id)&&e.examDate&&daysUntil(e.examDate,today)>=0).map(e=>daysUntil(e.examDate,today));
  const urgency=dates.length?Math.max(0,25-Math.min(...dates)/6):0;
  const due=p.due&&p.due<=today?20+Math.min(10,-daysUntil(p.due,today)):0;
  const attempts=state.answers.filter(a=>a.topic===t.id); const wrong=attempts.filter(a=>!a.correct&&!a.blank).length;
  const hesitation=attempts.filter(a=>a.confidence==='hesitant').length;
  const need=p.level===6?35:30-(p.level||0)*4;
  const score=targeted.length?need+urgency+due+Math.min(20,wrong*4+hesitation*2)+targeted.length*5+(t.weight||1)*3:-1;
  const reasons=[due?'revisão pendente':!p.level?'ainda sem evidência de domínio':'aprofundar domínio',...(wrong?[wrong+' erro(s) registrado(s)']:[]),...(targeted.length>1?[targeted.length+' objetivos em comum']:[])];
  return {...t,score,reasons};
 }).filter(t=>t.score>=0).sort((a,b)=>b.score-a.score||a.name.localeCompare(b.name));
}
function reviewSchedule(card,grade,today=isoDay()){
 const previous=Number(card.interval)||0; const streak=grade===0?0:(card.streak||0)+1;
 const interval=grade===0?1:grade===1?Math.max(1,Math.min(3,Math.ceil(previous*1.2))):previous===0?3:Math.min(120,Math.max(7,Math.round(previous*2.2)));
 return {...card,interval,streak,reviews:(card.reviews||0)+1,lastReview:today,due:addDays(today,interval),lastGrade:grade};
}
function scoreAnswers(answers,penalty=0){
 const correct=answers.filter(x=>x.correct).length,wrong=answers.filter(x=>!x.correct&&!x.blank).length,blank=answers.filter(x=>x.blank).length;
 return {correct,wrong,blank,total:answers.length,score:correct-wrong*penalty,accuracy:correct+wrong?Math.round(correct/(correct+wrong)*100):0};
}
function conflicts(events){
 const out=[];const a=events.filter(e=>e.kind==='exam');
 for(let i=0;i<a.length;i++)for(let j=i+1;j<a.length;j++){
  if(a[i].examId===a[j].examId||a[i].start.slice(0,10)!==a[j].start.slice(0,10))continue;
  const hasTime=!a[i].allDay&&!a[j].allDay&&a[i].end&&a[j].end;
  const overlap=!hasTime||(new Date(a[i].start)<new Date(a[j].end)&&new Date(a[j].start)<new Date(a[i].end));
  if(overlap)out.push({first:a[i],second:a[j],certain:hasTime,date:a[i].start.slice(0,10)});
 }return out;
}
function generatePlan(topics,state,exams,start=isoDay()){
 const ranked=rankTopics(topics,state,exams,start);if(!ranked.length)return[];
 const used={},result=[];for(let n=0;n<7;n++){
  const date=addDays(start,n),weekday=parseDay(date).getUTCDay();
  let budget=Number(state.settings.availability[weekday])||0;
  if(exams.some(e=>state.targets.includes(e.id)&&(e.dates||[e.examDate]).includes(date)))continue;
  while(budget>=15){
   const topic=ranked.slice().sort((a,b)=>(b.score/(1+(used[b.id]||0)*.7))-(a.score/(1+(used[a.id]||0)*.7)))[0];
   const minutes=Math.min(state.settings.block||40,budget);result.push({id:uid(),date,topic:topic.id,minutes,done:false});budget-=minutes;used[topic.id]=(used[topic.id]||0)+1;
  }
 }return result;
}
const icsEsc=s=>String(s||'').replace(/\\/g,'\\\\').replace(/\r?\n/g,'\\n').replace(/,/g,'\\,').replace(/;/g,'\\;');
function calendarICS(events){
 const stamp=new Date().toISOString().replace(/[-:]/g,'').replace(/\.\d{3}/,'');
 const lines=['BEGIN:VCALENDAR','VERSION:2.0','PRODID:-//Meridian//Agenda//PT-BR','CALSCALE:GREGORIAN','METHOD:PUBLISH'];
 for(const e of events){lines.push('BEGIN:VEVENT','UID:'+e.id+'@meridian.local','DTSTAMP:'+stamp);
  if(e.allDay||!e.start.includes('T')){lines.push('DTSTART;VALUE=DATE:'+e.start.slice(0,10).replace(/-/g,''),'DTEND;VALUE=DATE:'+addDays((e.end||e.start).slice(0,10),1).replace(/-/g,''));}
  else{lines.push('DTSTART:'+new Date(e.start).toISOString().replace(/[-:]/g,'').replace(/\.\d{3}/,''));if(e.end)lines.push('DTEND:'+new Date(e.end).toISOString().replace(/[-:]/g,'').replace(/\.\d{3}/,''));}
  lines.push('SUMMARY:'+icsEsc(e.title),'DESCRIPTION:'+icsEsc([e.period,e.description,e.sourceUrl?'Fonte: '+e.sourceUrl:'Evento pessoal'].filter(Boolean).join('\n')),'TRANSP:TRANSPARENT','END:VEVENT');
 }lines.push('END:VCALENDAR');
 // RFC 5545: fold at 75 UTF-8 octets, preserving Unicode code points.
 return lines.map(l=>{let out='',part='',len=0;for(const c of l){const n=new TextEncoder().encode(c).length;if(len+n>74){out+=part+'\r\n ';part='';len=1;}part+=c;len+=n;}return out+part;}).join('\r\n')+'\r\n';
}
function searchPages(documents,query){
 const words=normalize(query).split(/\s+/).filter(w=>w.length>1);if(!words.length)return[];
 const result=[];for(const d of documents)for(const p of d.pages||[]){const txt=normalize(p.text),count=words.reduce((s,w)=>s+(txt.includes(w)?1:0),0);if(count){const at=txt.indexOf(words.find(w=>txt.includes(w)));result.push({doc:d.id,title:d.name,page:p.number,score:count/words.length,excerpt:p.text.slice(Math.max(0,at-90),at+290)});}}
 return result.sort((a,b)=>b.score-a.score).slice(0,30);
}
function extractCandidates(pages){
 const out=[];for(const p of pages){const matches=p.text.matchAll(/\b([0-3]?\d)[/.-]([01]?\d)[/.-](20\d{2})\b/g);for(const m of matches){const d=m[3]+'-'+m[2].padStart(2,'0')+'-'+m[1].padStart(2,'0');const check=parseDay(d);if(Number.isNaN(+check)||isoDay(check)!==d)continue;out.push({date:d,page:p.number,context:p.text.slice(Math.max(0,m.index-90),m.index+120)});}}
 return out.filter((x,i,a)=>a.findIndex(y=>y.date===x.date&&y.page===x.page)===i).slice(0,70);
}
function parseCSV(text){
 const rows=[];let row=[],cell='',quote=false;const delimiter=text.split(/\r?\n/)[0].includes(';')?';':',';
 for(let i=0;i<text.length;i++){const c=text[i];if(c==='"'){if(quote&&text[i+1]==='"'){cell+='"';i++;}else quote=!quote;}else if(c===delimiter&&!quote){row.push(cell);cell='';}else if(c==='\n'&&!quote){row.push(cell.replace(/\r$/,''));rows.push(row);row=[];cell='';}else cell+=c;}
 if(cell||row.length){row.push(cell.replace(/\r$/,''));rows.push(row);}if(quote)throw Error('Aspas não fechadas no CSV.');
 const header=(rows.shift()||[]).map(s=>normalize(s.trim()));return rows.filter(r=>r.some(Boolean)).map(r=>Object.fromEntries(header.map((h,i)=>[h,(r[i]||'').trim()])));
}
function validateBackup(x){
 if(!x||x.format!=='meridian-backup'||x.version!==1||!x.state)throw Error('Este arquivo não é um backup Meridian v1.');
 const s=x.state;for(const field of ['targets','sessions','answers','cards','plan','events','materials','notes','errors','documents','customTopics','questions','essays'])if(!Array.isArray(s[field]))throw Error('Backup incompleto: '+field);
 if(!s.progress||!s.settings||!Array.isArray(s.settings.availability)||s.settings.availability.length!==7)throw Error('Configurações inválidas.');
 for(const field of ['sessions','answers','cards','plan','events','materials','notes','errors','documents','customTopics','questions','essays'])if(s[field].length>50000)throw Error('Backup excede os limites.');
 const validId=value=>typeof value==='string'&&/^[a-zA-Z0-9_-]{1,160}$/.test(value);
 for(const field of ['sessions','cards','plan','events','materials','notes','errors','documents','customTopics','questions','essays'])for(const item of s[field]){if(!item||!validId(item.id))throw Error('Identificador inválido em '+field);if(item.topic!==undefined&&!validId(item.topic))throw Error('Tópico inválido.');}
 if(s.targets.some(v=>!validId(v))||Object.keys(s.progress).some(v=>!validId(v)))throw Error('Objetivos ou progresso inválidos.');
 if(typeof s.settings.name!=='string'||!Number.isFinite(s.settings.dailyGoal)||s.settings.dailyGoal<15||s.settings.dailyGoal>720||!Number.isFinite(s.settings.block)||s.settings.block<15||s.settings.block>120||s.settings.availability.some(v=>!Number.isFinite(v)||v<0||v>720))throw Error('Preferências fora dos limites.');
 for(const e of s.events)if(typeof e.start!=='string'||!/^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2}-03:00)?$/.test(e.start)||Number.isNaN(+parseDay(e.start)))throw Error('Evento com data inválida.');
 for(const q of s.questions)if(typeof q.prompt!=='string'||!Array.isArray(q.choices)||q.choices.length<2||q.choices.length>5||q.choices.some(v=>typeof v!=='string')||!Number.isInteger(q.answer)||q.answer<0||q.answer>=q.choices.length||typeof q.explanation!=='string')throw Error('Questão inválida.');
 for(const t of s.customTopics)if(typeof t.name!=='string'||typeof t.subject!=='string'||!Array.isArray(t.exams)||t.exams.some(e=>!validId(e)))throw Error('Tópico próprio inválido.');
 return true;
}
const api={isoDay,parseDay,addDays,daysUntil,normalize,uid,rankTopics,reviewSchedule,scoreAnswers,conflicts,generatePlan,calendarICS,searchPages,extractCandidates,parseCSV,validateBackup};
if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.MeridianCore=api;
})(typeof window!=='undefined'?window:globalThis);
