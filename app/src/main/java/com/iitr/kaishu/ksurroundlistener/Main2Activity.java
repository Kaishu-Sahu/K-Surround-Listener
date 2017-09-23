package com.iitr.kaishu.ksurroundlistener;



import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.text.UnicodeSetSpanner;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;


public class Main2Activity extends AppCompatActivity {
Button startservice;
    SeekBar seekbar1,seekbar2;
    AdLayout amazonad;
    AudioManager am;
    BroadcastReceiver rec;

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(rec, new IntentFilter("runningstat"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(rec);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyhavePermission()) {
                requestForPermission();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        AdRegistration.setAppKey("a8a7e3931f1945768cb4e11691272f76");
        AdRegistration.enableTesting(true);
        AdRegistration.enableLogging(true);
        startservice = (Button) findViewById(R.id.startservice);
        amazonad = (AdLayout) findViewById(R.id.adview);
        amazonad.setListener(new Adlisten());
        if(isMyServiceRunning(Running.class)){
            startservice.setText("Stop");
        }
        Animation buttonanim = AnimationUtils.loadAnimation(this, R.anim.buttonfade);
        startservice.startAnimation(buttonanim);
        seekbar1 = (SeekBar)findViewById(R.id.seekBar1);
        seekbar2 = (SeekBar)findViewById(R.id.seekBar2);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        seekbar2.setMax(am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
        seekbar2.setProgress(am.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
        seekbar1.setTranslationX(-1*((getWindowManager().getDefaultDisplay().getWidth())/2));
        seekbar2.setTranslationX((getWindowManager().getDefaultDisplay().getWidth())/2);
        Animation seekbar1anim = AnimationUtils.loadAnimation(this,R.anim.initialbar);
        seekbar1.startAnimation(seekbar1anim);
        Animation seekbar2anim = AnimationUtils.loadAnimation(this,R.anim.initialbar2);
        seekbar2.startAnimation(seekbar2anim);
        SharedPreferences mic=getSharedPreferences("main",MODE_PRIVATE);
        final int micint = mic.getInt("MIC",MediaRecorder.AudioSource.CAMCORDER);
if(NoiseSuppressor.isAvailable()){
    Toast.makeText(this, "yesavailable", Toast.LENGTH_SHORT).show();
}
else{
    Toast.makeText(this, "notavailable", Toast.LENGTH_SHORT).show();
}


        if(isMyServiceRunning(Running.class)){
            Toast.makeText(this, "yesrunning", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "notrunning", Toast.LENGTH_SHORT).show();
        }
        startservice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {String action ;
                if(isMyServiceRunning(Running.class)){
                    action="STOP";
                    startservice.setText("Start");
                }
                else{action="START";
                    startservice.setText("Stop");}
                Intent startser = new Intent(Main2Activity.this,Running.class);
                startser.setAction(action);
                startService(startser);

            }
        });
 seekbar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
     @Override
     public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
         am.setStreamVolume(AudioManager.STREAM_VOICE_CALL,progress,0);
     }
     @Override
     public void onStartTrackingTouch(SeekBar seekBar) {}
     @Override
     public void onStopTrackingTouch(SeekBar seekBar) {}
 });

         rec = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                startservice.setText("Start");
                Toast.makeText(context, "stopped", Toast.LENGTH_SHORT).show();
            }
        };

amazonad.loadAd();



}
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    am.setStreamVolume(AudioManager.STREAM_VOICE_CALL,am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)+1,0);
                  seekbar2.setProgress(am.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if(am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)!=0){
                    am.setStreamVolume(AudioManager.STREAM_VOICE_CALL,am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)-1,0);
                    seekbar2.setProgress(am.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
                }}
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        amazonad.destroy();
    }

    public class Adlisten implements AdListener{
        @Override
        public void onAdLoaded(Ad ad, AdProperties adProperties) {
            Handler hand = new Handler();
            hand.postDelayed(new Runnable() {
                @Override
                public void run() {
                    amazonad.loadAd();
                }
            },30000);
        }

        @Override
        public void onAdFailedToLoad(Ad ad, AdError adError) {amazonad.loadAd();}

        @Override
        public void onAdExpanded(Ad ad) {}

        @Override
        public void onAdCollapsed(Ad ad) {}

        @Override
        public void onAdDismissed(Ad ad) {}
    }

    private boolean checkIfAlreadyhavePermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    private void requestForPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 10);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                   finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}