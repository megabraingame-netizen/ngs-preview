package com.namiwin.salesgateway;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceTestActivity extends Activity {
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private TextView status, transcript;
    private boolean running=false;
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
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,32,32,32);
        TextView title=new TextView(this);title.setText("NAMI WIN — تست مکالمه AI");title.setTextSize(24);title.setGravity(Gravity.CENTER_HORIZONTAL);root.addView(title);
        TextView sub=new TextView(this);sub.setText("این حالت برای تست تجربه مکالمه است: شما حرف می‌زنید، دستیار فروش فارسی جواب صوتی می‌دهد.");sub.setPadding(0,8,0,24);root.addView(sub);
        status=new TextView(this);status.setText("آماده");status.setTextSize(18);root.addView(status);
        Button start=new Button(this);start.setText("شروع مکالمه با AI");start.setOnClickListener(v->startConversation());root.addView(start);
        Button stop=new Button(this);stop.setText("توقف");stop.setOnClickListener(v->stopConversation());root.addView(stop);
        transcript=new TextView(this);transcript.setPadding(0,24,0,0);transcript.setText("متن مکالمه اینجا نمایش داده می‌شود.");root.addView(transcript);
        sc.addView(root);setContentView(sc);
    }

    private void initVoice(){
        tts=new TextToSpeech(this,s->{
            if(s==TextToSpeech.SUCCESS){
                Locale fa=new Locale("fa","IR");
                int r=tts.setLanguage(fa);
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.0f);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    @Override public void onStart(String id){runOnUiThread(()->status.setText("AI در حال صحبت..."));}
                    @Override public void onDone(String id){if(running)runOnUiThread(()->listen());}
                    @Override public void onError(String id){if(running)runOnUiThread(()->listen());}
                });
                if(r==TextToSpeech.LANG_MISSING_DATA||r==TextToSpeech.LANG_NOT_SUPPORTED)Toast.makeText(this,"صدای فارسی TTS روی گوشی کامل نیست؛ از تنظیمات Text-to-Speech یک صدای فارسی نصب کن.",Toast.LENGTH_LONG).show();
            }
        });
        if(SpeechRecognizer.isRecognitionAvailable(this)){
            recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener(){
                @Override public void onReadyForSpeech(Bundle p){status.setText("گوش می‌دهم... صحبت کن");}
                @Override public void onBeginningOfSpeech(){status.setText("صدای شما دریافت شد...");}
                @Override public void onRmsChanged(float r){}
                @Override public void onBufferReceived(byte[] b){}
                @Override public void onEndOfSpeech(){status.setText("در حال فهمیدن صحبت شما...");}
                @Override public void onError(int e){if(running){status.setText("دوباره گوش می‌دهم...");status.postDelayed(()->listen(),800);}}
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
        } else Toast.makeText(this,"Speech Recognition روی این گوشی در دسترس نیست.",Toast.LENGTH_LONG).show();
    }

    private void startConversation(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},77);return;}
        running=true;turn=0;transcript.setText("مکالمه شروع شد.");
        speak("سلام، وقت بخیر. من دستیار هوشمند فروش نامی وین هستم. اگر اجازه بدید چند سؤال کوتاه درباره پروژه‌تون بپرسم. در حال حاضر پروژه ساختمانی یا بازسازی در دست اجرا دارید؟");
    }

    private void stopConversation(){running=false;if(recognizer!=null)recognizer.cancel();if(tts!=null)tts.stop();status.setText("متوقف شد");}

    private void listen(){
        if(!running||recognizer==null)return;
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        try{recognizer.startListening(i);}catch(Exception e){status.setText("خطا در میکروفن: "+e.getMessage());}
    }

    private void speak(String s){
        if(tts==null)return;
        status.setText("AI آماده پاسخ است...");
        tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"turn-"+(turn++));
    }

    private String replyFor(String raw){
        String t=raw.replace('ي','ی').replace('ك','ک');
        if(t.contains("تماس نگیرید")||t.contains("مزاحم")||t.contains("نمیخوام")||t.contains("نمی‌خوام")){
            running=false;
            return "حتماً، مزاحم شما نمی‌شوم. ممنون از وقتی که گذاشتید. روز خوبی داشته باشید.";
        }
        if(t.contains("مدیر")||t.contains("خود امید")||t.contains("خودتون")||t.contains("مسئول فروش")){
            return "حتماً. این مشتری را به عنوان نیازمند تحویل مستقیم علامت می‌زنم. در نسخه متصل به CRM همین‌جا هشدار برای امید ارسال می‌شود تا ادامه گفتگو را خودش بگیرد.";
        }
        if(t.contains("قیمت")||t.contains("پیش فاکتور")||t.contains("پیش‌فاکتور")||t.contains("استعلام")){
            return "حتماً. برای اینکه قیمت دقیق بدهیم لطفاً بفرمایید پروژه کجاست و حدود متراژ در و پنجره، نما یا کرتین وال چقدر است؟ اگر نقشه دارید هم می‌توانیم بر اساس نقشه بررسی کنیم.";
        }
        if(t.contains("کرتین")||t.contains("نما")){
            return "بسیار خوب. برای کرتین وال و نما می‌خواهم مرحله فعلی پروژه، ارتفاع تقریبی، متراژ و اینکه نقشه اجرایی دارید یا نه را بدانم. پروژه در چه مرحله‌ای است؟";
        }
        if(t.contains("پنجره")||t.contains("آلومینیوم")||t.contains("درب")){
            return "عالی. برای در و پنجره آلومینیومی، اگر تعداد بازشوها یا متراژ تقریبی و محل پروژه را بفرمایید می‌توانیم سریع‌تر پیشنهاد مناسب بدهیم. محل پروژه کجاست؟";
        }
        if(t.contains("اصفهان")||t.contains("سپاهان")||t.contains("تهران")||t.contains("شیراز")||t.contains("کاشان")){
            return "خیلی خوب، محل پروژه ثبت شد. الان پروژه در چه مرحله‌ای است؛ اسکلت، سفت کاری، نما یا مرحله انتخاب در و پنجره؟";
        }
        if(t.contains("داریم")||t.contains("بله")||t.contains("آره")||t.contains("پروژه")){
            return "خیلی هم خوب. لطفاً بفرمایید پروژه چه نوعی است؛ مسکونی، تجاری، اداری یا بازسازی؟ و بیشتر برای کدام بخش نیاز دارید؛ در و پنجره، کرتین وال، نما، شیشه یا دکوراسیون؟";
        }
        if(t.contains("نداریم")||t.contains("فعلا")||t.contains("فعلاً")||t.contains("بعدا")||t.contains("بعداً")){
            return "متوجه شدم. چه زمانی احتمال دارد پروژه بعدی یا مرحله خرید شروع شود؟ اگر زمان تقریبی را بفرمایید، همان موقع پیگیری می‌کنیم و مزاحمت اضافه ایجاد نمی‌شود.";
        }
        return "متوجه شدم. برای اینکه درست راهنمایی کنم، لطفاً کمی بیشتر درباره نوع پروژه، محل آن و کاری که از نامی وین انتظار دارید توضیح بدهید.";
    }

    @Override protected void onDestroy(){running=false;if(recognizer!=null)recognizer.destroy();if(tts!=null)tts.shutdown();super.onDestroy();}
}
