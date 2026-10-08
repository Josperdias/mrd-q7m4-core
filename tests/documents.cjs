const path=require('node:path'),{pathToFileURL}=require('node:url');
const ROOT=path.resolve(__dirname,'..');
const fs=require('node:fs'),assert=require('node:assert/strict');
const {createCanvas,DOMMatrix,Path2D,ImageData}=require('@napi-rs/canvas');
global.DOMMatrix=DOMMatrix;global.Path2D=Path2D;global.ImageData=ImageData;
(async()=>{
 const lib=await import(pathToFileURL(path.join(ROOT,'dist/vendor/pdfjs/pdf.min.mjs')).href);
 lib.GlobalWorkerOptions.workerSrc=path.join(ROOT,'dist/vendor/pdfjs/pdf.worker.min.mjs');
 const task=lib.getDocument({data:new Uint8Array(fs.readFileSync(path.join(ROOT,'dist/resources/tcego-edital-2026.pdf'))),isEvalSupported:false,useSystemFonts:true});const pdf=await task.promise;
 const page=await pdf.getPage(1),content=await page.getTextContent(),text=content.items.map(x=>x.str).join(' ');
 assert.ok(text.toUpperCase().includes('CONTAS'));assert.ok(pdf.numPages>10);const viewport=page.getViewport({scale:0.6}),renderCanvas=createCanvas(viewport.width,viewport.height);await page.render({canvasContext:renderCanvas.getContext('2d'),viewport}).promise;console.log('PDF render passed.');console.log('PDF.js 6.4.299: official PDF loaded, '+pdf.numPages+' pages; first-page text extracted.');await task.destroy();
 const canvas=createCanvas(1000,240),ctx=canvas.getContext('2d');ctx.fillStyle='white';ctx.fillRect(0,0,1000,240);ctx.fillStyle='black';ctx.font='42px sans-serif';ctx.fillText('Estudar com clareza e revisar.',35,95);ctx.fillText('Prova em 22 de novembro.',35,175);
 const tess=require('tesseract.js');
 const worker=await tess.createWorker('por',1,{corePath:path.join(ROOT,'dist/vendor/ocr/tesseract-core-lstm.wasm.js'),langPath:path.join(ROOT,'dist/vendor/ocr'),cacheMethod:'none'});
 try{const result=await worker.recognize(canvas.toBuffer('image/png'));assert.match(result.data.text,/Estudar/);assert.match(result.data.text,/novembro/);console.log('Tesseract.js 7.0.0: bundled Portuguese data and WASM recognized synthetic text, confidence '+result.data.confidence+'.');}finally{await worker.terminate();}
})().catch(e=>{console.error(e);process.exitCode=1});
