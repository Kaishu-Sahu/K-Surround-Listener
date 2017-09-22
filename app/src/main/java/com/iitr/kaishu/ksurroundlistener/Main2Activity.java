package com.iitr.kaishu.ksurroundlistener;



import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.UnicodeSetSpanner;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
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
         SharedPreferences.Editor volume = getSharedPreferences("volumes", MODE_PRIVATE).edit();
         volume.putFloat("speaker", (float) progress/100);
         volume.apply();
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
}