package com.namiwin.salesgateway;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class VoipAiCallActivity extends Activity {
    private EditText bridgeUrl, apiToken, phone;
    private TextView status, log;
    private SharedPreferences prefs;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private volatile String callId="";
    private volatile boolean polling=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("namiwin",MODE_PRIVATE);
        buildUi();
    }

    private void buildUi(){
        ScrollView sc=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,28);
        TextView title=new TextView(this); title.setText("NAMI WIN — VoIP AI Call Test v0.5"); title.setTextSize(23); title.setGravity(Gravity.CENTER_HORIZONTAL); root.addView(title);
        TextView note=new TextView(this); note.setText("این حالت تماس را از پل VoIP/SIP انجام می‌دهد تا صدای AI واقعاً داخل مسیر تماس باشد. آدرس پل تماس را یک‌بار وارد کن، سپس شماره را دستی بزن و تماس را شروع کن."); note.setPadding(0,10,0,18); root.addView(note);

        bridgeUrl=new EditText(this); bridgeUrl.setHint("آدرس AI Call Bridge، مثال: https://call.example.com"); bridgeUrl.setText(prefs.getString("voip_bridge","")); root.addView(bridgeUrl);
        apiToken=new EditText(this); apiToken.setHint("توکن پل تماس (در صورت نیاز)"); apiToken.setText(prefs.getString("voip_token","")); root.addView(apiToken);
        phone=new EditText(this); phone.setHint("شماره مشتری، مثال 0912..."); phone.setInputType(3); root.addView(phone);

        Button health=new Button(this); health.setText("بررسی اتصال پل تماس"); health.setOnClickListener(v->checkHealth()); root.addView(health);
        Button start=new Button(this); start.setText("☎ شروع تماس واقعی AI"); start.setOnClickListener(v->startCall()); root.addView(start);
        Button stop=new Button(this); stop.setText("■ قطع AI / تحویل به اپراتور"); stop.setOnClickListener(v->takeover()); root.addView(stop);

        status=new TextView(this); status.setText("وضعیت: آماده"); status.setTextSize(18); status.setPadding(0,20,0,0); root.addView(status);
        log=new TextView(this); log.setText("گزارش:"); log.setPadding(0,16,0,0); root.addView(log);
        sc.addView(root); setContentView(sc);
    }

    private String base(){String s=bridgeUrl.getText().toString().trim(); while(s.endsWith("/"))s=s.substring(0,s.length()-1); return s;}
    private String token(){return apiToken.getText().toString().trim();}
    private void save(){prefs.edit().putString("voip_bridge",base()).putString("voip_token",token()).apply();}

    private void checkHealth(){
        save(); if(base().isEmpty()){status.setText("آدرس پل تماس را وارد کن");return;}
        status.setText("در حال بررسی اتصال...");
        new Thread(()->{
            try{String r=request("GET",base()+"/health",null); runOnUiThread(()->{status.setText("پل تماس متصل است");append(r);});}
            catch(Exception e){runOnUiThread(()->{status.setText("اتصال ناموفق");append("Health error: "+e.getMessage());});}
        }).start();
    }

    private void startCall(){
        save(); String n=phone.getText().toString().trim();
        if(base().isEmpty()){status.setText("ابتدا آدرس پل تماس را وارد کن");return;}
        if(n.isEmpty()){status.setText("شماره مشتری را وارد کن");return;}
        status.setText("در حال ارسال فرمان تماس..."); append("Dial request: "+n);
        new Thread(()->{
            try{
                JSONObject j=new JSONObject(); j.put("phone",n); j.put("language","fa-IR"); j.put("campaign","namiwin-sales-test"); j.put("handoff","operator");
                String r=request("POST",base()+"/api/ai-call/start",j.toString());
                JSONObject o=new JSONObject(r); callId=o.optString("callId","");
                runOnUiThread(()->{status.setText(callId.isEmpty()?"پاسخ پل تماس ناقص است":"تماس در صف/در حال برقراری");append(r);});
                if(!callId.isEmpty()){polling=true; poll();}
            }catch(Exception e){runOnUiThread(()->{status.setText("شروع تماس ناموفق");append("Start error: "+e.getMessage());});}
        }).start();
    }

    private void poll(){
        if(!polling||callId.isEmpty())return;
        new Thread(()->{
            try{
                String r=request("GET",base()+"/api/ai-call/"+callId,null); JSONObject o=new JSONObject(r);
                String s=o.optString("status","unknown"), summary=o.optString("summary","");
                runOnUiThread(()->{status.setText("وضعیت تماس: "+s); if(!summary.isEmpty())append("AI: "+summary);});
                if(s.equals("completed")||s.equals("failed")||s.equals("cancelled")){polling=false;}
            }catch(Exception e){runOnUiThread(()->append("Poll error: "+e.getMessage()));}
            if(polling)handler.postDelayed(this::poll,1500);
        }).start();
    }

    private void takeover(){
        if(callId.isEmpty()){status.setText("تماس فعالی وجود ندارد");return;}
        polling=false;
        new Thread(()->{
            try{JSONObject j=new JSONObject();j.put("action","handoff");String r=request("POST",base()+"/api/ai-call/"+callId+"/control",j.toString());runOnUiThread(()->{status.setText("فرمان تحویل تماس ارسال شد");append(r);});}
            catch(Exception e){runOnUiThread(()->{status.setText("خطای تحویل تماس");append(e.getMessage());});}
        }).start();
    }

    private String request(String method,String url,String body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(8000); c.setReadTimeout(15000); c.setRequestProperty("Accept","application/json");
        if(!token().isEmpty())c.setRequestProperty("Authorization","Bearer "+token());
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");try(OutputStream os=c.getOutputStream()){os.write(body.getBytes("UTF-8"));}}
        int code=c.getResponseCode(); BufferedReader br=new BufferedReader(new InputStreamReader(code>=200&&code<300?c.getInputStream():c.getErrorStream(),"UTF-8")); StringBuilder sb=new StringBuilder(); String line; while((line=br.readLine())!=null)sb.append(line); br.close(); if(code<200||code>=300)throw new Exception("HTTP "+code+" "+sb); return sb.toString();
    }

    private void append(String s){log.append("\n"+s);}
    @Override protected void onDestroy(){polling=false;handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
