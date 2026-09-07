package com.namiwin.salesgateway;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class LiveCallLabActivity extends Activity {
    private EditText phone;
    private TextView status, log;
    private TextToSpeech tts;
    private SpeechRecognizer recognizer;
    private boolean ttsReady=false, running=false, introStarted=false;
    private final Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b){
        super.onCreate(b); buildUi(); initVoice();
        if(Build.VERSION.SDK_INT>=23){
            ArrayList<String> p=new ArrayList<>();
            if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.CALL_PHONE);
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.RECORD_AUDIO);
            if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),88);
        }
    }

    private void buildUi(){
        ScrollView sc=new ScrollView(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,28);
        TextView title=new TextView(this); title.setText("NAMI WIN — آزمایش تماس واقعی AI"); title.setTextSize(23); title.setGravity(Gravity.CENTER_HORIZONTAL); root.addView(title);
        TextView note=new TextView(this); note.setText("شماره را وارد کن. برنامه تماس می‌گیرد؛ بعد از پاسخ مشتری، اسپیکر را روشن می‌کند و مکالمه آزمایشی AI شروع می‌شود. این حالت برای کشف اکو، تأخیر و محدودیت‌های A26 است."); note.setPadding(0,12,0,18); root.addView(note);
        phone=new EditText(this); phone.setHint("شماره مشتری، مثال 0912..."); phone.setInputType(3); root.addView(phone);
        Button start=new Button(this); start.setText("☎ تماس آزمایشی AI"); start.setOnClickListener(v->startLiveTest()); root.addView(start);
        Button voice=new Button(this); voice.setText("🔊 فقط تست صدای AI"); voice.setOnClickListener(v->speak("سلام. صدای دستیار هوشمند نامی وین در حال تست است.")); root.addView(voice);
        Button stop=new Button(this); stop.setText("■ توقف AI"); stop.setOnClickListener(v->stopAi()); root.addView(stop);
        status=new TextView(this); status.setText("وضعیت: آماده"); status.setTextSize(18); status.setPadding(0,20,0,0); root.addView(status);
        log=new TextView(this); log.setText("گزارش آزمایش:"); log.setPadding(0,18,0,0); root.addView(log);
        sc.addView(root); setContentView(sc);
    }

    private void initVoice(){
        tts=new TextToSpeech(this,r->{
            if(r==TextToSpeech.SUCCESS){
                int lang=tts.setLanguage(new Locale("fa","IR"));
                if(lang==TextToSpeech.LANG_MISSING_DATA||lang==TextToSpeech.LANG_NOT_SUPPORTED)tts.setLanguage(Locale.getDefault());
                tts.setSpeechRate(0.92f); tts.setPitch(1.02f); ttsReady=true;
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    @Override public void onStart(String id){runOnUiThread(()->status.setText("AI در حال صحبت با مشتری..."));}
                    @Override public void onDone(String id){if(running)runOnUiThread(()->handler.postDelayed(()->listen(),350));}
                    @Override public void onError(String id){runOnUiThread(()->{append("TTS error"); if(running)listen();});}
                });
                runOnUiThread(()->status.setText("وضعیت: موتور صدا آماده"));
            } else runOnUiThread(()->status.setText("خطا در راه‌اندازی TTS: "+r));
        });
        if(SpeechRecognizer.isRecognitionAvailable(this)){
            recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener(){
                @Override public void onReadyForSpeech(Bundle b){status.setText("AI گوش می‌دهد... مشتری صحبت کند");}
                @Override public void onBeginningOfSpeech(){status.setText("صدای محیط/مشتری دریافت شد...");}
                @Override public void onRmsChanged(float r){}
                @Override public void onBufferReceived(byte[] b){}
                @Override public void onEndOfSpeech(){status.setText("در حال تحلیل پاسخ...");}
                @Override public void onError(int e){append("Speech error="+e); if(running)handler.postDelayed(()->listen(),900);}
                @Override public void onResults(Bundle b){
                    ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String text=(xs==null||xs.isEmpty())?"":xs.get(0);
                    if(text.isEmpty()){listen();return;}
                    append("مشتری/محیط: "+text); String reply=replyFor(text); append("AI: "+reply); speak(reply);
                }
                @Override public void onPartialResults(Bundle b){}
                @Override public void onEvent(int t,Bundle b){}
            });
        }
    }

    private void startLiveTest(){
        String n=phone.getText().toString().trim(); if(n.isEmpty()){Toast.makeText(this,"شماره را وارد کن",Toast.LENGTH_SHORT).show();return;}
        if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){Toast.makeText(this,"مجوز تماس لازم است",Toast.LENGTH_SHORT).show();return;}
        if(!ttsReady){Toast.makeText(this,"موتور صدا هنوز آماده نیست",Toast.LENGTH_SHORT).show();return;}
        running=true; introStarted=false; append("شماره‌گیری: "+n); status.setText("در حال شماره‌گیری...");
        try{
            TelecomManager tm=(TelecomManager)getSystemService(TELECOM_SERVICE); tm.placeCall(Uri.fromParts("tel",n,null),new Bundle());
            handler.postDelayed(this::waitForActiveCall,700);
        }catch(Exception e){running=false;status.setText("خطای تماس: "+e.getMessage());append("Dial error: "+e);}
    }

    private void waitForActiveCall(){
        if(!running)return;
        int state=NamiInCallService.currentState();
        status.setText("وضعیت تماس: "+NamiInCallService.stateName(state));
        if(state==android.telecom.Call.STATE_ACTIVE){
            if(!introStarted){introStarted=true; enableSpeaker(); append("تماس ACTIVE شد؛ شروع AI"); handler.postDelayed(()->speak("سلام وقت بخیر. من دستیار هوشمند فروش نامی وین هستم. برای تست کوتاه تماس گرفتم. آیا در حال حاضر پروژه ساختمانی، نما، در و پنجره یا بازسازی در دست اجرا دارید؟"),800);}
        }else if(state==android.telecom.Call.STATE_DISCONNECTED){ running=false; append("تماس قطع شد"); }
        else handler.postDelayed(this::waitForActiveCall,700);
    }

    private void enableSpeaker(){
        try{
            boolean routed=NamiInCallService.routeSpeaker();
            AudioManager am=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
            if(am!=null){am.setMode(AudioManager.MODE_IN_COMMUNICATION);am.setSpeakerphoneOn(true);}
            append("Speaker route="+routed);
        }catch(Exception e){append("Speaker error: "+e.getMessage());}
    }

    private void speak(String s){
        if(!ttsReady){status.setText("TTS آماده نیست");return;}
        if(recognizer!=null)try{recognizer.cancel();}catch(Exception ignored){}
        int r=tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"live-"+System.nanoTime());
        if(r==TextToSpeech.ERROR){status.setText("TTS speak ERROR");append("TTS speak returned ERROR");}
    }

    private void listen(){
        if(!running||recognizer==null)return;
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR"); i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        try{recognizer.startListening(i);}catch(Exception e){append("Recognizer exception: "+e.getMessage());handler.postDelayed(()->listen(),1000);}
    }

    private String replyFor(String raw){
        String t=raw.replace('ي','ی').replace('ك','ک');
        if(t.contains("مدیر")||t.contains("امید"))return "حتماً. یک لحظه لطفاً. تماس را برای ادامه گفتگو با مدیر فروش علامت می‌زنم.";
        if(t.contains("قیمت")||t.contains("استعلام"))return "حتماً. لطفاً محل پروژه و متراژ تقریبی را بفرمایید تا استعلام دقیق‌تری آماده کنیم.";
        if(t.contains("پنجره")||t.contains("آلومینیوم"))return "بسیار خوب. محل پروژه و حدود متراژ در و پنجره را می‌فرمایید؟";
        if(t.contains("کرتین")||t.contains("نما"))return "عالی. پروژه در چه مرحله‌ای است و حدود متراژ نما یا کرتین وال چقدر است؟";
        if(t.contains("نه")||t.contains("نداریم")||t.contains("فعلاً"))return "متوجه شدم. اگر اجازه بدهید زمان مناسب‌تری برای پیگیری ثبت می‌کنم. حدوداً چه زمانی مناسب است؟";
        if(t.contains("بله")||t.contains("داریم")||t.contains("پروژه"))return "خیلی خوب. لطفاً نوع پروژه، محل پروژه و بخشی که برایش نیاز به اجرا یا قیمت دارید را بفرمایید.";
        return "ممنون. لطفاً کمی بیشتر درباره پروژه و نیازتان توضیح بدهید.";
    }

    private void stopAi(){running=false;introStarted=false;if(recognizer!=null)try{recognizer.cancel();}catch(Exception ignored){}if(tts!=null)tts.stop();status.setText("AI متوقف شد؛ تماس در اختیار شماست");append("AI stopped / manual takeover");}
    private void append(String s){runOnUiThread(()->log.append("\n"+s));}

    @Override protected void onDestroy(){running=false;handler.removeCallbacksAndMessages(null);if(recognizer!=null)recognizer.destroy();if(tts!=null)tts.shutdown();super.onDestroy();}
}
