package app.mrd.q7m4;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.*;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 private static final String ORIGIN="https://appassets.androidplatform.net/";
 private WebView web;
 private ValueCallback<Uri[]> fileCallback;
 private byte[] pendingExport;
 private static final int PICK=10,SAVE=11;

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  getWindow().setStatusBarColor(Color.rgb(23,43,49));
  getWindow().setNavigationBarColor(Color.rgb(23,43,49));
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(245,246,243));
  root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
  web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,-1));setContentView(root);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setDefaultTextEncodingName("UTF-8");s.setMediaPlaybackRequiresUserGesture(true);
  web.addJavascriptInterface(new ExportBridge(),"MeridianNative");
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest req){Uri uri=req.getUrl();if(ORIGIN.startsWith(uri.getScheme()+"://"+uri.getHost()+"/"))return false;if("https".equals(uri.getScheme())||"http".equals(uri.getScheme())){try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){message("Nenhum navegador disponível.");}}return true;}
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){
    Uri u=req.getUrl();if(!"appassets.androidplatform.net".equals(u.getHost()))return null;
    String path=u.getPath();if(path==null||path.equals("/"))path="/index.html";
    if(path.contains(".."))return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
    try{String mime=path.endsWith(".js")||path.endsWith(".mjs")?"application/javascript":path.endsWith(".css")?"text/css":path.endsWith(".html")?"text/html":path.endsWith(".json")||path.endsWith(".webmanifest")?"application/json":path.endsWith(".svg")?"image/svg+xml":path.endsWith(".png")?"image/png":path.endsWith(".wasm")?"application/wasm":path.endsWith(".gz")?"application/gzip":path.endsWith(".pdf")?"application/pdf":"application/octet-stream";
     Map<String,String> headers=new HashMap<>();headers.put("Access-Control-Allow-Origin","https://appassets.androidplatform.net");headers.put("Cache-Control","no-cache");
     return new WebResourceResponse(mime,mime.startsWith("text")||mime.contains("javascript")||mime.contains("json")||mime.contains("svg")?"UTF-8":null,200,"OK",headers,getAssets().open("web"+path));
    }catch(IOException e){return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",Collections.emptyMap(),new ByteArrayInputStream("Not found".getBytes()));}
   }
   @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){root.removeView(v);v.destroy();new AlertDialog.Builder(MainActivity.this).setTitle("Memória do aparelho").setMessage("O leitor precisou ser reiniciado. Seus registros salvos permanecem. Tente um PDF menor ou uma página por vez.").setPositiveButton("Reabrir",(d,w)->recreate()).show();return true;}
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params){if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;try{Intent intent=params.createIntent();intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,PICK);return true;}catch(Exception e){fileCallback=null;message("Não foi possível abrir o seletor de arquivos.");return false;}}
  });
  web.loadUrl(ORIGIN+"index.html");
 }
 private void message(String text){runOnUiThread(()->Toast.makeText(this,text,Toast.LENGTH_LONG).show());}
 public class ExportBridge{
  @JavascriptInterface public void saveFile(String name,String mime,String encoded){
   if(encoded==null||encoded.length()>120*1024*1024){message("Arquivo acima do limite de exportação.");return;}
   final String safeName=name.replaceAll("[^a-zA-Z0-9_.-]","_");
   runOnUiThread(()->{if(web==null||web.getUrl()==null||!web.getUrl().startsWith(ORIGIN))return;try{pendingExport=Base64.decode(encoded,Base64.DEFAULT);Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(mime.split(";")[0]);intent.putExtra(Intent.EXTRA_TITLE,safeName);startActivityForResult(intent,SAVE);}catch(Exception e){pendingExport=null;message("Não foi possível preparar a exportação.");}});
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  if(request==PICK&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}
  if(request==SAVE){if(result==RESULT_OK&&data!=null&&data.getData()!=null&&pendingExport!=null){try(OutputStream stream=getContentResolver().openOutputStream(data.getData())){stream.write(pendingExport);message("Arquivo salvo.");}catch(Exception e){message("Falha ao salvar. Tente exportar novamente.");}}pendingExport=null;}
 }
 @Override public void onBackPressed(){if(web!=null){web.evaluateJavascript("(function(){var d=document.querySelector('dialog[open]');if(d){d.close();return 'closed';}return 'back';})()",result->{if("\"back\"".equals(result)){if(web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setMessage("Sair do Meridian?").setPositiveButton("Sair",(d,w)->finish()).setNegativeButton("Continuar",null).show();}});}else super.onBackPressed();}
 @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("MeridianNative");web.destroy();web=null;}super.onDestroy();}
}
