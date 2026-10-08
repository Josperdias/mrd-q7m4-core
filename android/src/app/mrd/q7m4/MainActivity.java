package app.mrd.q7m4;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.webkit.*;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import org.json.JSONObject;
import java.util.*;

public class MainActivity extends Activity {
 private static final String ORIGIN="https://appassets.androidplatform.net/";
 private WebView web;
 private ValueCallback<Uri[]> fileCallback;
 private byte[] pendingExport;
 private static final int PICK=10,SAVE=11,LINK=12;
 // Updates are read only from this fixed release; Android itself refuses an APK not signed with the installed key.
 private static final String UPDATE_BASE="https://github.com/Josperdias/mrd-q7m4-core/releases/download/apk-latest/";
 private static final String ACTION_INSTALL="app.mrd.q7m4.INSTALL_STATUS";
 private static final long MAX_APK=80L*1024*1024;
 private boolean updating;
 private BroadcastReceiver installReceiver;

 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  getWindow().setStatusBarColor(Color.rgb(23,43,49));
  getWindow().setNavigationBarColor(Color.rgb(23,43,49));
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(245,246,243));
  root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
  web=new WebView(this);web.setBackgroundColor(Color.rgb(245,246,243));web.setOverScrollMode(View.OVER_SCROLL_NEVER);root.addView(web,new LinearLayout.LayoutParams(-1,-1));setContentView(root);
  registerInstallReceiver();
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setDefaultTextEncodingName("UTF-8");s.setMediaPlaybackRequiresUserGesture(true);s.setSupportZoom(false);
  web.addJavascriptInterface(new ExportBridge(),"MeridianNative");web.addJavascriptInterface(new UpdateBridge(),"MeridianUpdater");web.addJavascriptInterface(new DriveBridge(),"MeridianDrive");
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

 // ---- In-app update ----
 private void emit(String type,Object... kv){try{JSONObject o=new JSONObject().put("type",type);for(int i=0;i+1<kv.length;i+=2)o.put(String.valueOf(kv[i]),kv[i+1]);final String js="window.meridianUpdate&&window.meridianUpdate("+o.toString()+")";runOnUiThread(()->{if(web!=null)web.evaluateJavascript(js,null);});}catch(Exception e){}}
 private PackageInfo selfInfo(){try{return getPackageManager().getPackageInfo(getPackageName(),0);}catch(Exception e){return null;}}
 private long currentCode(){PackageInfo p=selfInfo();return p==null?0:(Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode);}
 private String currentName(){PackageInfo p=selfInfo();return p==null||p.versionName==null?"":p.versionName;}
 private HttpURLConnection open(String url) throws IOException{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","Meridian-Android");if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());return c;}
 private JSONObject fetchInfo() throws Exception{HttpURLConnection c=open(UPDATE_BASE+"update.json");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0){out.write(buf,0,n);if(out.size()>65536)throw new IOException("update.json too large");}return new JSONObject(out.toString("UTF-8"));}finally{c.disconnect();}}
 private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format("%02x",x));return b.toString();}
 private void registerInstallReceiver(){
  installReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){int st=i.getIntExtra(PackageInstaller.EXTRA_STATUS,-1);
   if(st==PackageInstaller.STATUS_PENDING_USER_ACTION){Intent confirm=i.getParcelableExtra(Intent.EXTRA_INTENT);if(confirm!=null){confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);try{startActivity(confirm);}catch(Exception e){message("Não foi possível abrir a confirmação de instalação.");}}}
   else if(st!=PackageInstaller.STATUS_SUCCESS){emit("error","message","A instalação não foi concluída: "+String.valueOf(i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))+".");}}};
  IntentFilter f=new IntentFilter(ACTION_INSTALL);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(installReceiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(installReceiver,f);
 }
 private void downloadAndInstall() throws Exception{
  JSONObject u=fetchInfo();long remote=u.getLong("versionCode");
  if(remote<=currentCode()){emit("check","available",false,"versionCode",remote,"versionName",u.optString("versionName",""),"currentCode",currentCode(),"currentName",currentName());return;}
  String want=u.getString("sha256").toLowerCase(Locale.ROOT);
  File dir=new File(getCacheDir(),"updates");dir.mkdirs();File apk=new File(dir,"Meridian.apk");
  MessageDigest md=MessageDigest.getInstance("SHA-256");
  String file=u.optString("file","Meridian-1.0.0.apk");if(!file.matches("Meridian-[0-9.]+\\.apk"))file="Meridian-1.0.0.apk";
  HttpURLConnection c=open(UPDATE_BASE+file);
  try{long total=c.getContentLengthLong();if(total>MAX_APK)throw new IOException("APK too large");
   try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(apk)){byte[] buf=new byte[64*1024];long got=0;int last=-1,n;while((n=in.read(buf))>0){out.write(buf,0,n);md.update(buf,0,n);got+=n;if(got>MAX_APK)throw new IOException("APK too large");if(total>0){int pct=(int)(got*100/total);if(pct!=last&&pct%5==0){last=pct;emit("progress","pct",pct);}}}}
  }finally{c.disconnect();}
  if(!hex(md.digest()).equals(want)){apk.delete();emit("error","message","O arquivo baixado não confere com o publicado. Tente novamente.");return;}
  PackageInfo info=getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(),0);
  if(info==null||!getPackageName().equals(info.packageName)){apk.delete();emit("error","message","O arquivo baixado não é uma versão do Meridian.");return;}
  PackageInstaller pi=getPackageManager().getPackageInstaller();
  PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);params.setSize(apk.length());
  int id=pi.createSession(params);
  try(PackageInstaller.Session session=pi.openSession(id)){
   try(InputStream in=new FileInputStream(apk);OutputStream out=session.openWrite("Meridian.apk",0,apk.length())){byte[] buf=new byte[64*1024];int n;while((n=in.read(buf))>0)out.write(buf,0,n);session.fsync(out);}
   Intent intent=new Intent(ACTION_INSTALL).setPackage(getPackageName());
   PendingIntent pending=PendingIntent.getBroadcast(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=31?PendingIntent.FLAG_MUTABLE:0));
   emit("installing");session.commit(pending.getIntentSender());
  }
 }
 public class UpdateBridge{
  @JavascriptInterface public String appInfo(){try{return new JSONObject().put("versionCode",currentCode()).put("versionName",currentName()).toString();}catch(Exception e){return "{}";}}
  @JavascriptInterface public void checkUpdate(){new Thread(()->{try{JSONObject u=fetchInfo();long remote=u.getLong("versionCode");emit("check","available",remote>currentCode(),"versionCode",remote,"versionName",u.optString("versionName",""),"currentCode",currentCode(),"currentName",currentName());}catch(Exception e){emit("error","message","Não foi possível verificar agora. Confira a conexão e tente de novo.");}}).start();}
  @JavascriptInterface public void installUpdate(){runOnUiThread(()->{
   if(web==null||web.getUrl()==null||!web.getUrl().startsWith(ORIGIN))return;
   if(!getPackageManager().canRequestPackageInstalls()){try{startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));}catch(Exception e){}emit("need-permission");return;}
   if(updating)return;updating=true;
   new Thread(()->{try{downloadAndInstall();}catch(Exception e){emit("error","message","Não foi possível baixar a atualização. Confira a conexão e tente de novo.");}finally{updating=false;}}).start();
  });}
 }

 // ---- Link to a Drive file chosen once through the system file picker (no Google API keys) ----
 private void emitDrive(String type,Object... kv){try{JSONObject o=new JSONObject().put("type",type);for(int i=0;i+1<kv.length;i+=2)o.put(String.valueOf(kv[i]),kv[i+1]);final String js="window.meridianDrive&&window.meridianDrive("+o.toString()+")";runOnUiThread(()->{if(web!=null)web.evaluateJavascript(js,null);});}catch(Exception e){}}
 private SharedPreferences prefs(){return getSharedPreferences("meridian",MODE_PRIVATE);}
 private String driveName(Uri uri){try(Cursor c=getContentResolver().query(uri,null,null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)return c.getString(i);}}catch(Exception e){}return "arquivo do Drive";}
 public class DriveBridge{
  @JavascriptInterface public String driveInfo(){String u=prefs().getString("driveUri",null);try{JSONObject o=new JSONObject();if(u!=null){o.put("linked",true).put("name",prefs().getString("driveName",""));}else o.put("linked",false);return o.toString();}catch(Exception e){return "{\"linked\":false}";}}
  @JavascriptInterface public void linkDrive(){runOnUiThread(()->{
   if(web==null||web.getUrl()==null||!web.getUrl().startsWith(ORIGIN))return;
   try{Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/json");intent.putExtra(Intent.EXTRA_TITLE,"Meridian_Sync.json");intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(intent,LINK);}catch(Exception e){emitDrive("error","message","Não foi possível abrir o seletor de arquivos.");}
  });}
  @JavascriptInterface public void syncDrive(String json){
   if(json==null||json.length()>5*1024*1024){emitDrive("error","message","Resumo grande demais para sincronizar.");return;}
   final String data=json;final String u=prefs().getString("driveUri",null);
   if(u==null){emitDrive("error","message","Vincule um arquivo do Drive primeiro.");return;}
   new Thread(()->{try(OutputStream out=getContentResolver().openOutputStream(Uri.parse(u),"wt")){if(out==null)throw new IOException("sem acesso");out.write(data.getBytes("UTF-8"));emitDrive("synced","at",System.currentTimeMillis());}catch(Exception e){emitDrive("error","message","Não foi possível gravar no Drive. Vincule o arquivo de novo.");}}).start();
  }
  @JavascriptInterface public void unlinkDrive(){String u=prefs().getString("driveUri",null);if(u!=null){try{getContentResolver().releasePersistableUriPermission(Uri.parse(u),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception e){}}prefs().edit().remove("driveUri").remove("driveName").apply();emitDrive("unlinked");}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  if(request==PICK&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}
  if(request==LINK){if(result==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();boolean persisted=true;try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception e){persisted=false;}
   if(persisted){String name=driveName(uri);prefs().edit().putString("driveUri",uri.toString()).putString("driveName",name).apply();emitDrive("linked","name",name);}else emitDrive("error","message","Este local não permite vínculo permanente. Escolha uma pasta do Google Drive.");}}
  if(request==SAVE){if(result==RESULT_OK&&data!=null&&data.getData()!=null&&pendingExport!=null){try(OutputStream stream=getContentResolver().openOutputStream(data.getData())){stream.write(pendingExport);message("Arquivo salvo.");}catch(Exception e){message("Falha ao salvar. Tente exportar novamente.");}}pendingExport=null;}
 }
 @Override public void onBackPressed(){if(web!=null){web.evaluateJavascript("(function(){var d=document.querySelector('dialog[open]');if(d){d.close();return 'closed';}return 'back';})()",result->{if("\"back\"".equals(result)){if(web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setMessage("Sair do Meridian?").setPositiveButton("Sair",(d,w)->finish()).setNegativeButton("Continuar",null).show();}});}else super.onBackPressed();}
 @Override protected void onDestroy(){if(installReceiver!=null){try{unregisterReceiver(installReceiver);}catch(Exception e){}installReceiver=null;}if(web!=null){web.removeJavascriptInterface("MeridianNative");web.removeJavascriptInterface("MeridianUpdater");web.removeJavascriptInterface("MeridianDrive");web.destroy();web=null;}super.onDestroy();}
}
