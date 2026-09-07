package com.namiwin.salesgateway;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.telecom.TelecomManager;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GatewayService extends Service {
    private static final String CH="namiwin_gateway";
    private ScheduledExecutorService exec;

    @Override public void onCreate(){ super.onCreate(); createChannel(); startForeground(1001,notification("NamiWin Gateway روشن است","در انتظار فرمان CRM")); exec=Executors.newSingleThreadScheduledExecutor(); exec.scheduleWithFixedDelay(this::poll,1,4,TimeUnit.SECONDS); }
    @Override public int onStartCommand(Intent i,int flags,int startId){ return START_STICKY; }
    @Override public void onDestroy(){ if(exec!=null)exec.shutdownNow(); super.onDestroy(); }
    @Override public IBinder onBind(Intent i){ return null; }

    private String server(){ SharedPreferences p=getSharedPreferences("namiwin",MODE_PRIVATE); String s=p.getString("server","").trim(); while(s.endsWith("/"))s=s.substring(0,s.length()-1); return s; }
    private String deviceId(){ String id=Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID); return id==null?"a26":id; }

    private void poll(){
        String base=server(); if(base.isEmpty())return;
        HttpURLConnection c=null;
        try{
            URL u=new URL(base+"/api/mobile/next-command?deviceId="+Uri.encode(deviceId()));
            c=(HttpURLConnection)u.openConnection(); c.setConnectTimeout(5000); c.setReadTimeout(5000); c.setRequestMethod("GET");
            if(c.getResponseCode()!=200)return;
            String text=read(c); if(text==null||text.trim().isEmpty())return;
            JSONObject x=new JSONObject(text); if(x.optBoolean("empty",false))return;
            String id=x.optString("id",""); String type=x.optString("type","");
            if("dial".equals(type)){
                if(!x.optBoolean("consent",false)){ ack(base,id,"blocked_no_consent","تماس به علت نبود مجوز تماس انجام نشد"); return; }
                String phone=x.optString("phone",""); boolean ok=placeCellularCall(phone); ack(base,id,ok?"dialing":"dial_failed",ok?"تماس با سیم‌کارت آغاز شد":"مجوز CALL_PHONE یا TelecomManager در دسترس نیست");
            } else if("handoff".equals(type)){
                showHandoff(x.optString("leadName","مشتری"),x.optString("reason","مشتری آماده ادامه گفتگو با شماست"));
                ack(base,id,"handoff_alerted","هشدار روی A26 نمایش داده شد");
            } else ack(base,id,"ignored","نوع فرمان ناشناخته");
        }catch(Exception ignored){} finally{if(c!=null)c.disconnect();}
    }

    private boolean placeCellularCall(String phone){
        try{
            if(phone==null||phone.trim().isEmpty())return false;
            if(checkSelfPermission(Manifest.permission.CALL_PHONE)!= PackageManager.PERMISSION_GRANTED)return false;
            TelecomManager tm=(TelecomManager)getSystemService(TELECOM_SERVICE); if(tm==null)return false;
            tm.placeCall(Uri.fromParts("tel",phone.trim(),null),new Bundle()); return true;
        }catch(Exception e){return false;}
    }

    private void ack(String base,String id,String status,String message){
        if(id==null||id.isEmpty())return; HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(base+"/api/mobile/command-result").openConnection(); c.setConnectTimeout(5000); c.setReadTimeout(5000); c.setRequestMethod("POST"); c.setRequestProperty("Content-Type","application/json; charset=utf-8"); c.setDoOutput(true);
            byte[] b=new JSONObject().put("id",id).put("deviceId",deviceId()).put("status",status).put("message",message).toString().getBytes(StandardCharsets.UTF_8);
            try(OutputStream o=c.getOutputStream()){o.write(b);} c.getResponseCode();
        }catch(Exception ignored){} finally{if(c!=null)c.disconnect();}
    }

    private String read(HttpURLConnection c)throws Exception{ try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8))){StringBuilder s=new StringBuilder();String l;while((l=r.readLine())!=null)s.append(l);return s.toString();} }

    private void showHandoff(String lead,String reason){ NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE); if(nm!=null)nm.notify(2001,notification("🔴 مشتری آماده تحویل: "+lead,reason+" — برنامه را باز کن و «ادامه گفتگو را خودم می‌گیرم» را بزن.")); }
    private Notification notification(String title,String text){
        Intent i=new Intent(this,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CH):new Notification.Builder(this); b.setContentTitle(title).setContentText(text).setSmallIcon(android.R.drawable.sym_action_call).setContentIntent(pi).setOngoing(title.contains("Gateway")); return b.build();
    }
    private void createChannel(){ if(Build.VERSION.SDK_INT>=26){ NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE); if(nm!=null)nm.createNotificationChannel(new NotificationChannel(CH,"NamiWin Sales Gateway",NotificationManager.IMPORTANCE_HIGH)); } }
}
