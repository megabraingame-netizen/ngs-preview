package com.namiwin.salesgateway;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText serverUrl, phone;
    private TextView status;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs=getSharedPreferences("namiwin",MODE_PRIVATE);
        requestNeededPermissions();
        buildUi();
        if (getIntent()!=null && Intent.ACTION_DIAL.equals(getIntent().getAction()) && getIntent().getData()!=null) {
            phone.setText(getIntent().getData().getSchemeSpecificPart());
        }
    }

    private void buildUi(){
        ScrollView sc=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,32,32,32);
        TextView title=new TextView(this); title.setText("NAMI WIN — A26 Sales Gateway"); title.setTextSize(24); title.setGravity(Gravity.CENTER_HORIZONTAL); root.addView(title);
        TextView sub=new TextView(this); sub.setText("تماس با سیم‌کارت + اتصال به CRM + تحویل مکالمه به امید"); sub.setGravity(Gravity.CENTER_HORIZONTAL); sub.setPadding(0,8,0,24); root.addView(sub);

        serverUrl=new EditText(this); serverUrl.setHint("آدرس CRM، مثال: http://192.168.1.10:8787"); serverUrl.setText(prefs.getString("server","")); root.addView(serverUrl);
        Button save=button("ذخیره و روشن کردن Gateway",v->{prefs.edit().putString("server",serverUrl.getText().toString().trim()).apply(); startGateway();}); root.addView(save);
        Button dialer=button("فعال‌سازی به عنوان برنامه تماس پیش‌فرض",v->requestDialerRole()); root.addView(dialer);

        phone=new EditText(this); phone.setHint("شماره برای تست تماس"); phone.setInputType(3); root.addView(phone);
        Button call=button("تماس با سیم‌کارت",v->placeCall(phone.getText().toString().trim())); root.addView(call);
        Button takeover=button("ادامه گفتگو را خودم می‌گیرم",v->{ boolean ok=NamiInCallService.takeOver(); Toast.makeText(this,ok?"کنترل تماس به شما داده شد":"تماس قابل کنترل پیدا نشد",Toast.LENGTH_LONG).show();}); root.addView(takeover);
        Button hold=button("Hold / Resume تماس",v->{ boolean ok=NamiInCallService.toggleHold(); Toast.makeText(this,ok?"فرمان انجام شد":"Hold در این تماس/اپراتور در دسترس نیست",Toast.LENGTH_LONG).show();}); root.addView(hold);

        status=new TextView(this); status.setPadding(0,24,0,0); status.setText("Device ID: "+deviceId()+"\nGateway: خاموش"); root.addView(status);
        TextView note=new TextView(this); note.setPadding(0,28,0,0); note.setText("نکته: شماره‌گیری سلولی از خود A26 انجام می‌شود. دسترسی AI به صدای دوطرف تماس سلولی توسط Android محدود است؛ برای AI صوتی کامل و Transfer واقعی باید GSM/LTE Gateway یا SIP Bridge متصل شود."); root.addView(note);
        sc.addView(root); setContentView(sc);
    }

    private Button button(String t, View.OnClickListener l){ Button b=new Button(this); b.setText(t); b.setOnClickListener(l); return b; }

    private void requestNeededPermissions(){
        if(Build.VERSION.SDK_INT>=23){
            java.util.ArrayList<String> p=new java.util.ArrayList<>();
            if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.CALL_PHONE);
            if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.READ_PHONE_STATE);
            if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)p.add(Manifest.permission.POST_NOTIFICATIONS);
            if(!p.isEmpty())requestPermissions(p.toArray(new String[0]),10);
        }
    }

    private void startGateway(){
        Intent i=new Intent(this,GatewayService.class);
        if(Build.VERSION.SDK_INT>=26)startForegroundService(i); else startService(i);
        status.setText("Device ID: "+deviceId()+"\nGateway: روشن — "+prefs.getString("server",""));
    }

    private void requestDialerRole(){
        if(Build.VERSION.SDK_INT>=29){
            RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);
            if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER),20);
            else Toast.makeText(this,"نقش Dialer فعال است یا در دسترس نیست",Toast.LENGTH_SHORT).show();
        } else {
            Intent i=new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER); i.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,getPackageName()); startActivity(i);
        }
    }

    private void placeCall(String n){
        if(n.isEmpty()){Toast.makeText(this,"شماره را وارد کن",Toast.LENGTH_SHORT).show();return;}
        if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){requestNeededPermissions();return;}
        TelecomManager tm=(TelecomManager)getSystemService(TELECOM_SERVICE);
        if(tm!=null) tm.placeCall(Uri.fromParts("tel",n,null),new Bundle());
    }

    public String deviceId(){ String id=Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID); return id==null?"a26":id; }
}
