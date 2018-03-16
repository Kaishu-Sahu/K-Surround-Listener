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
import android.media.audiofx.BassBoost;
import android.media.audiofx.NoiseSuppressor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.util.Log;
import android.widget.Toast;

public class Running extends Service {
//    AudioRecord record;
//    //    AudioRecord record1;
//    AudioTrack track;
//    Runnable thread;
//    AudioManager seram;

//    int SAMPLE_RATE = 8000;
//    int BUF_SIZE = 640;
//    byte[] buffer = new byte[BUF_SIZE];
//    // byte[] buffer1 = new byte[BUF_SIZE];
//    // byte[] buffer2 = new byte[BUF_SIZE];
//    int buffersize;
//    int sessionid;
//    Boolean state = true;
    RecordTask recordTask;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Intent stopintent = new Intent(this, Running.class);
//        stopintent.setAction("STOP");
//        PendingIntent pi = PendingIntent.getService(this, 0, stopintent, 0);
//        Notification.Builder nnb = new Notification.Builder(this)
//                .setContentTitle("K Surround Listener")
//                .setContentText("Tap to Stop")
//                .setSmallIcon(R.drawable.blackear)
//                .setContentIntent(pi)
//                .setAutoCancel(true);
////        seram = (AudioManager) getSystemService(AUDIO_SERVICE);
//        if (Build.VERSION.SDK_INT > 21) {
//            nnb.setColor(getResources().getColor(R.color.colorPrimaryDark));
//        }
//        Notification nn = nnb.build();
//        startForeground(50, nn);
        if (intent.getAction().equals("START")) {
            recordTask = new RecordTask();
            recordTask.EnableNoiseSupprssor(intent.getBooleanExtra("noise", true));
            recordTask.EnableBassBoost(intent.getBooleanExtra("bass", true));
            recordTask.Start();
            return START_REDELIVER_INTENT;
        } else if (intent.getAction().equals("STOP")) {
            if (recordTask != null) {
                recordTask.Stop();
                recordTask = null;
            }
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }


    private static class RecordTask extends AsyncTask<String, Integer, String> {
        AudioRecord arec;
        AudioTrack atrack;

        int buffersize;
        byte[] buffer;

        boolean is_recording = false;

        boolean enable_suppressor = true;
        NoiseSuppressor noise_suppressor;

        boolean enable_bass = true;
        BassBoost bass_boost;

        // todo: make these options
        private static final int RECORDER_SAMPLERATE = 8000;
        private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
        private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        private static final int PLAYBACK_CHANNELS = AudioFormat.CHANNEL_OUT_MONO;

        boolean Start() {
            if (this.InitAudioPassthrough()) {   // on main thread
                execute();
                return true;
            }
            return false;
        }

        /**
         * will cause DoAudioPassthrough() to stop looping and return
         **/
        void Stop() {
            is_recording = false;
        }

        void EnableNoiseSupprssor(boolean newstate) {
            enable_suppressor = newstate;
        }

        void EnableBassBoost(boolean newstate) {
            enable_bass = newstate;
        }

        @Override
        protected String doInBackground(String... params) {
            this.DoAudioPassthrough();
            return "this string is passed to postExecute";
        }


        /**
         * happens on main thread
         **/
        boolean InitAudioPassthrough() {
            is_recording = true;

            buffersize = AudioRecord.getMinBufferSize(
                    RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING);


            arec = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, buffersize);

            int state = arec.getState();
            if (state < 1) {
                is_recording = false;
                // todo: dynamically request permissions. For now we just fail reasonably.
                return false;
            }

            atrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    RECORDER_SAMPLERATE, PLAYBACK_CHANNELS, RECORDER_AUDIO_ENCODING,
                    buffersize, AudioTrack.MODE_STREAM);

            if (enable_suppressor) {
                noise_suppressor = NoiseSuppressor.create(arec.getAudioSessionId());
            }

            if (enable_bass) {
                bass_boost = new BassBoost(1, atrack.getAudioSessionId());
            }

            buffer = new byte[buffersize * 8];
            return true;
        }

        /**
         * this happens on a thread
         **/
        void DoAudioPassthrough() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // start the recording and playback
            arec.startRecording();
            atrack.play();

            // tight loop play recorded buffer directly
            while (is_recording) {
                int count = arec.read(buffer, 0, buffersize);
                atrack.write(buffer, 0, count);
            }

            arec.stop();
            atrack.stop();


            if (noise_suppressor != null) {
                noise_suppressor.release();
                noise_suppressor = null;
            }

            atrack.release();
            arec.release();

            atrack = null;
            arec = null;
            buffer = null;
        }

    }
}
