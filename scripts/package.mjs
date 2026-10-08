import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..'),dist=path.join(root,'dist');
const files=[];function walk(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())walk(p);else if(e.name!=='sw.js')files.push('./'+path.relative(dist,p).replaceAll('\\','/'));}}
walk(dist);
// All engine assets are cached individually; first-use installation needs network.
const content=`const CACHE='meridian-v1-'+${JSON.stringify(new Date().toISOString().slice(0,19))};const ASSETS=${JSON.stringify(['./',...files])};
self.addEventListener('install',event=>event.waitUntil((async()=>{const cache=await caches.open(CACHE);for(const url of ASSETS){const response=await fetch(url,{cache:'reload'});if(!response.ok)throw new Error('Offline asset unavailable: '+url);await cache.put(url,response);}self.skipWaiting();})()));
self.addEventListener('activate',event=>event.waitUntil((async()=>{for(const key of await caches.keys())if(key.startsWith('meridian-')&&key!==CACHE)await caches.delete(key);await self.clients.claim();})()));
self.addEventListener('fetch',event=>{if(event.request.method!=='GET'||new URL(event.request.url).origin!==location.origin)return;event.respondWith((async()=>{if(new URL(event.request.url).pathname.endsWith('/catalog.json')){try{const r=await fetch(event.request);if(r.ok){const c=await caches.open(CACHE);await c.put(event.request,r.clone());return r;}}catch{}}const cached=await caches.match(event.request);return cached||fetch(event.request);})());});`;
fs.writeFileSync(path.join(dist,'sw.js'),content);
console.log(`Prepared ${files.length} offline assets.`);
