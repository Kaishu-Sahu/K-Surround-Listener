package com.iitr.kaishu.ksurroundlistener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.util.Log;
import android.widget.Toast;

public class Running extends Service {
    AudioRecord record;
//    AudioRecord record1;
    AudioTrack track;
    Runnable thread;
    AudioManager seram;

    int SAMPLE_RATE = 8000;
    int BUF_SIZE = 500;
    byte[] buffer = new byte[BUF_SIZE];
   // byte[] buffer1 = new byte[BUF_SIZE];
   // byte[] buffer2 = new byte[BUF_SIZE];
    int buffersize;
    int sessionid;
    Boolean state = true;
    Recording task = new Recording();
    public Running() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }



    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        Intent stopintent = new Intent(this,Running.class);
        stopintent.setAction("STOP");
        PendingIntent pi = PendingIntent.getService(this, 0, stopintent, 0);
        Notification.Builder nnb= new Notification.Builder(this)
                .setContentTitle("K Surround Listener")
                .setContentText("Tap to Stop")
                .setSmallIcon(R.drawable.blackear)
                .setContentIntent(pi)
                .setAutoCancel(true);
        seram = (AudioManager) getSystemService(AUDIO_SERVICE);
        if(Build.VERSION.SDK_INT > 21){nnb.setColor(getResources().getColor(R.color.colorPrimaryDark));}
        Notification nn = nnb.build();
        startForeground(50,nn);
        if (intent.getAction().equals("STOP")){
            task.cancel(true);
            Intent toactivity = new Intent("runningstat");
            sendBroadcast(toactivity);
            stopSelf();
            return START_REDELIVER_INTENT;
        }
        else{
        task.execute();}

    return START_STICKY;
}


public class Recording extends AsyncTask {
Boolean headphones = true;

    @Override
    protected Object doInBackground(Object[] params) {

        try {
            buffersize = AudioRecord
                    .getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);


        } catch (Throwable t) {
            Log.e("Audio", "Buffer size failed");
        }

        if (buffersize <= BUF_SIZE) {
            buffersize = BUF_SIZE;
        }
        try {
            //MediaRecorder.AudioSource.CAMCORDER
            // MediaRecorder.AudioSource.MIC
            record = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, buffersize * 2);
            /*record1 = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, buffersize * 2);*/
            sessionid=record.getAudioSessionId();
           // NoiseSuppressor.create(sessionid);
            track = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, buffersize * 2,
                    AudioTrack.MODE_STREAM);


            track.setPlaybackRate(SAMPLE_RATE);
        } catch (Throwable t) {
            Log.e("Audio", "Audio Failed");
        }

        record.startRecording();
     //   record1.startRecording();
        track.play();
        try {
            thread = new Runnable() {
                public void run() {
                    while (state) {
                        if(!isCancelled()){
                            if(seram.isWiredHeadsetOn()){
                        record.read(buffer, 0, BUF_SIZE);

                            /*record1.read(buffer2,0,BUF_SIZE);
                            for (int a=0;a<BUF_SIZE;a++){
                                buffer[a]=(byte) (*//*buffer1[a]-*//*buffer2[a]);
                            }*/
                        track.write(buffer, 0, BUF_SIZE);}
                            else {headphones=false;
                                Intent toactivity = new Intent("runningstat");
                                sendBroadcast(toactivity);
                                state=false;}

                            }
                        else{


                            record.stop();
                            record.release();
                          /*  record1.stop();
                            record1.release();*/
                            track.stop();
                            state = false;


                        }
                    }
                }
            };

            thread.run();
        } catch (Exception e) {
            Log.e("ERROR", "exception: " + e.getLocalizedMessage());
        }

        Log.i("D2Record", "loopback exit");


        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        if(!headphones){
            Toast.makeText(Running.this, "Headphones ??", Toast.LENGTH_SHORT).show();
        }
        stopSelf();

    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
      try{  record.stop();
        record.release();
        track.stop();}
      catch (Exception e){}
    }
}

}
