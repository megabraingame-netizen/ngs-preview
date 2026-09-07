package com.namiwin.salesgateway;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class VoiceTestActivity extends Activity {
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView status, transcript, engineInfo;
    private boolean running=false;
    private boolean ttsReady=false;
    private boolean pendingStart=false;
    private int turn=0;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        buildUi();
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},77);
        }
        initVoice();
    }

    private void buildUi(){
        ScrollView sc=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,32,32,32);
        TextView title=new TextView(this); title.setText("NAMI WIN — تست مکالمه AI v0.3"); title.setTextSize(24); title.setGravity(Gravity.CENTER_HORIZONTAL); root.addView(title);
        TextView sub=new TextView(this); sub.setText("ابتدا موتور صدا بررسی می‌شود. سپس AI فارسی صحبت می‌کند و بعد به صدای شما گوش می‌دهد."); sub.setPadding(0,8,0,18); root.addView(sub);
        engineInfo=new TextView(this); engineInfo.setText("موتور صدا: در حال آماده‌سازی..."); engineInfo.setPadding(0,0,0,16); root.addView(engineInfo);
        status=new TextView(this); status.setText("در حال آماده‌سازی صدا..."); status.setTextSize(18); root.addView(status);
        Button test=new Button(this); test.setText("🔊 تست صدا"); test.setOnClickListener(v->testVoice()); root.addView(test);
        Button start=new Button(this); start.setText("🎙 شروع مکالمه با AI"); start.setOnClickListener(v->startConversation()); root.addView(start);
        Button stop=new Button(this); stop.setText("توقف"); stop.setOnClickListener(v->stopConversation()); root.addView(stop);
        transcript=new TextView(this); transcript.setPadding(0,24,0,0); transcript.setText("متن مکالمه اینجا نمایش داده می‌شود."); root.addView(transcript);
        sc.addView(root); setContentView(sc);
    }

    private void initVoice(){
        ttsReady=false;
        tts=new TextToSpeech(this,s->{
            if(s!=TextToSpeech.SUCCESS){
                runOnUiThread(()->{
                    status.setText("خطا: موتور تبدیل متن به گفتار راه‌اندازی نشد.");
                    engineInfo.setText("TTS init error: "+s);
                });
                return;
            }
            try{
                if(Build.VERSION.SDK_INT>=21){
                    tts.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build());
                }
                Locale fa=new Locale("fa","IR");
                int lang=tts.setLanguage(fa);
                String selected="";
                if(Build.VERSION.SDK_INT>=21){
                    Set<Voice> voices=tts.getVoices();
                    if(voices!=null){
                        for(Voice v:voices){
                            Locale l=v.getLocale();
                            if(l!=null && "fa".equalsIgnoreCase(l.getLanguage()) && !v.isNetworkConnectionRequired()){
                                if(tts.setVoice(v)==TextToSpeech.SUCCESS){ selected=v.getName(); break; }
                            }
                        }
                        if(selected.isEmpty()){
                            for(Voice v:voices){
                                Locale l=v.getLocale();
                                if(l!=null && "fa".equalsIgnoreCase(l.getLanguage())){
                                    if(tts.setVoice(v)==TextToSpeech.SUCCESS){ selected=v.getName(); break; }
                                }
                            }
                        }
                    }
                }
                boolean faOk=lang!=TextToSpeech.LANG_MISSING_DATA && lang!=TextToSpeech.LANG_NOT_SUPPORTED;
                if(!faOk){
                    Locale def=Locale.getDefault();
                    int fallback=tts.setLanguage(def);
                    selected=selected.isEmpty()?"fallback: "+def.toLanguageTag():selected;
                    if(fallback==TextToSpeech.LANG_MISSING_DATA||fallback==TextToSpeech.LANG_NOT_SUPPORTED){
                        tts.setLanguage(Locale.US);
                        selected="fallback: en-US";
                    }
                }
                tts.setSpeechRate(0.92f);
                tts.setPitch(1.02f);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    @Override public void onStart(String id){ runOnUiThread(()->status.setText("AI در حال صحبت...")); }
                    @Override public void onDone(String id){ if(running) runOnUiThread(()->listen()); }
                    @Override public void onError(String id){ runOnUiThread(()->status.setText("خطا در پخش صدا. دکمه «تست صدا» را بزن.")); if(running) runOnUiThread(()->listen()); }
                    @Override public void onError(String id,int code){ runOnUiThread(()->status.setText("خطای TTS: "+code)); if(running) runOnUiThread(()->listen()); }
                });
                ttsReady=true;
                final String voiceName=selected.isEmpty()?"صدای پیش‌فرض سیستم":selected;
                runOnUiThread(()->{
                    engineInfo.setText("موتور صدا آماده است — "+voiceName);
                    status.setText(faOk?"آماده — صدای فارسی شناسایی شد":"آماده — فارسی اختصاصی پیدا نشد؛ حالت fallback فعال است");
                    if(pendingStart){ pendingStart=false; startConversation(); }
                });
            }catch(Exception e){
                runOnUiThread(()->status.setText("خطا در تنظیم موتور صدا: "+e.getMessage()));
            }
        });
    }

    private void testVoice(){
        if(!ttsReady){
            status.setText("صبر کن؛ موتور صدا هنوز آماده نشده است...");
            return;
        }
        int r=tts.speak("سلام. این یک تست صدا از دستیار هوشمند نامی وین است.",TextToSpeech.QUEUE_FLUSH,null,"voice-test");
        if(r==TextToSpeech.ERROR) status.setText("فرمان TTS خطا داد. موتور گفتار گوشی را بررسی کن.");
        else status.setText("فرمان تست صدا ارسال شد...");
    }

    private void startConversation(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},77); return;
        }
        if(!ttsReady){
            pendingStart=true;
            status.setText("موتور صدا هنوز آماده نیست؛ به‌محض آماده‌شدن مکالمه شروع می‌شود...");
            return;
        }
        running=true; turn=0; transcript.setText("مکالمه شروع شد.");
        speak("سلام، وقت بخیر. من دستیار هوشمند فروش نامی وین هستم. اگر اجازه بدید چند سؤال کوتاه درباره پروژه‌تون بپرسم. در حال حاضر پروژه ساختمانی یا بازسازی در دست اجرا دارید؟");
    }

    private void stopConversation(){ running=false; pendingStart=false; if(recognizer!=null)recognizer.cancel(); if(tts!=null)tts.stop(); status.setText("متوقف شد"); }

    private void listen(){
        if(!running)return;
        if(recognizer==null){ status.setText("Speech Recognition در دسترس نیست."); return; }
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        try{ recognizer.startListening(i); }catch(Exception e){ status.setText("خطا در میکروفن: "+e.getMessage()); }
    }

    private void initRecognizer(){
        if(SpeechRecognizer.isRecognitionAvailable(this)){
            recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener(){
                @Override public void onReadyForSpeech(Bundle p){status.setText("گوش می‌دهم... صحبت کن");}
                @Override public void onBeginningOfSpeech(){status.setText("صدای شما دریافت شد...");}
                @Override public void onRmsChanged(float r){}
                @Override public void onBufferReceived(byte[] b){}
                @Override public void onEndOfSpeech(){status.setText("در حال فهمیدن صحبت شما...");}
                @Override public void onError(int e){if(running){status.setText("خطای تشخیص صدا: "+e+" — دوباره گوش می‌دهم...");status.postDelayed(()->listen(),1000);}}
                @Override public void onResults(Bundle b){
                    ArrayList<String> xs=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String text=(xs==null||xs.isEmpty())?"":xs.get(0);
                    if(text.isEmpty()){listen();return;}
                    transcript.append("\n\nشما: "+text);
                    String reply=replyFor(text);
                    transcript.append("\nAI: "+reply);
                    speak(reply);
                }
                @Override public void onPartialResults(Bundle b){}
                @Override public void onEvent(int t,Bundle b){}
            });
        } else status.setText("Speech Recognition روی این گوشی در دسترس نیست.");
    }

    private void speak(String s){
        if(!ttsReady||tts==null){ status.setText("TTS هنوز آماده نیست."); return; }
        int r=tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"turn-"+(turn++));
        if(r==TextToSpeech.ERROR) status.setText("فرمان پخش صدا ناموفق بود.");
    }

    private String replyFor(String raw){
        String t=raw.replace('ي','ی').replace('ك','ک');
        if(t.contains("تماس نگیرید")||t.contains("مزاحم")||t.contains("نمیخوام")||t.contains("نمی‌خوام")){
            running=false; return "حتماً، مزاحم شما نمی‌شوم. ممنون از وقتی که گذاشتید. روز خوبی داشته باشید.";
        }
        if(t.contains("مدیر")||t.contains("خود امید")||t.contains("خودتون")||t.contains("مسئول فروش")){
            return "حتماً. این مشتری را برای تحویل مستقیم به امید علامت می‌زنم تا ادامه گفتگو را خودش انجام دهد.";
        }
        if(t.contains("قیمت")||t.contains("پیش فاکتور")||t.contains("پیش‌فاکتور")||t.contains("استعلام")){
            return "حتماً. برای قیمت دقیق لطفاً بفرمایید پروژه کجاست و حدود متراژ در و پنجره، نما یا کرتین وال چقدر است؟";
        }
        if(t.contains("کرتین")||t.contains("نما")){
            return "بسیار خوب. برای کرتین وال و نما، مرحله فعلی پروژه، ارتفاع تقریبی، متراژ و داشتن نقشه اجرایی مهم است. پروژه در چه مرحله‌ای است؟";
        }
        if(t.contains("پنجره")||t.contains("آلومینیوم")||t.contains("درب")){
            return "عالی. لطفاً تعداد بازشوها یا متراژ تقریبی و محل پروژه را بفرمایید تا پیشنهاد مناسب بدهیم.";
        }
        if(t.contains("اصفهان")||t.contains("سپاهان")||t.contains("تهران")||t.contains("شیراز")||t.contains("کاشان")){
            return "خیلی خوب، محل پروژه ثبت شد. الان پروژه در چه مرحله‌ای است؛ اسکلت، سفت کاری، نما یا مرحله انتخاب در و پنجره؟";
        }
        if(t.contains("داریم")||t.contains("بله")||t.contains("آره")||t.contains("پروژه")){
            return "خیلی هم خوب. پروژه چه نوعی است؛ مسکونی، تجاری، اداری یا بازسازی؟ و بیشتر برای کدام بخش نیاز دارید؟";
        }
        if(t.contains("نداریم")||t.contains("فعلا")||t.contains("فعلاً")||t.contains("بعدا")||t.contains("بعداً")){
            return "متوجه شدم. چه زمانی احتمال دارد پروژه بعدی یا مرحله خرید شروع شود؟ همان موقع پیگیری می‌کنیم.";
        }
        return "متوجه شدم. لطفاً کمی بیشتر درباره نوع پروژه، محل آن و کاری که از نامی وین انتظار دارید توضیح بدهید.";
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==77 && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            status.setText("مجوز میکروفن داده شد.");
        }
    }

    @Override protected void onResume(){ super.onResume(); if(recognizer==null) initRecognizer(); }
    @Override protected void onDestroy(){ running=false; if(recognizer!=null)recognizer.destroy(); if(tts!=null)tts.shutdown(); super.onDestroy(); }
}
