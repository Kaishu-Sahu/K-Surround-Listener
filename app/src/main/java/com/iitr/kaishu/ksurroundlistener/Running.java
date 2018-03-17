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
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.BassBoost;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.NoiseSuppressor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

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

        private double LOWPASS = 2000;
        private double FREQ = 8000;

        int buffersize;
        byte[] buffer;
        double[] doubleAudio;

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
            Log.d(TAG, "EnableNoiseSupprssor() called with: newstate = [" + newstate + "]");
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


            arec = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
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


//            LoudnessEnhancer enhancer = new LoudnessEnhancer(atrack.getAudioSessionId());
//            enhancer.setEnabled(true);
//            float curGain = enhancer.getTargetGain();
//            enhancer.setTargetGain(5000);
//            Log.d(TAG, "current gain is：" + curGain);

//            if (enable_suppressor) {
//                noise_suppressor = NoiseSuppressor.create(arec.getAudioSessionId());
//                if (noise_suppressor == null) {
//                    Log.d(TAG, "noise suppressor failed");
//                }
//                else
//                    Log.d(TAG, "noise suppressor called" + noise_suppressor.toString());
//
//                if (AutomaticGainControl.create(arec.getAudioSessionId()) == null) {
//                    Log.d(TAG, "AutomaticGainControl() faile");
//                } else {
//                    Log.d(TAG, "AutomaticGainControl() called");
//                }
//
//
//                if (AcousticEchoCanceler.create(arec.getAudioSessionId()) == null) {
//                    Log.d(TAG, "AcousticEchoCanceler() failed");
//                } else {
//                    Log.d(TAG, "AcousticEchoCanceler() calles");
//                }
//
//                Log.d(TAG, "InitAudioPassthrough() called" + noise_suppressor);
//            }

//            if (enable_bass) {
//                bass_boost = new BassBoost(1, atrack.getAudioSessionId());
//            }

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

            noise_suppressor = NoiseSuppressor.create(arec.getAudioSessionId());
            if (noise_suppressor == null) {
                Log.d(TAG, "noise suppressor failed");
            }
            else
                Log.d(TAG, "noise suppressor called" + noise_suppressor.toString());
//
//            if (AutomaticGainControl.create(arec.getAudioSessionId()) == null) {
//                Log.d(TAG, "AutomaticGainControl() faile");
//            } else {
//                Log.d(TAG, "AutomaticGainControl() called");
//            }





//            LoudnessEnhancer enhancer = new LoudnessEnhancer(atrack.getAudioSessionId());
//            enhancer.setEnabled(true);
//            float curGain = enhancer.getTargetGain();
//            enhancer.setTargetGain(1000);
//            Log.d(TAG, "current gain is：" + curGain);

            atrack.play();

            // tight loop play recorded buffer directly
            while (is_recording) {
//
//                noise_suppressor = NoiseSuppressor.create(arec.getAudioSessionId());
//                Log.d(TAG, "DoAudioPassthrough() called" + noise_suppressor.getEnabled() );
//                Log.d(TAG, "isavail() called" + NoiseSuppressor.isAvailable() );
//
//                noise_suppressor.setEnabled(true);
//                Log.d(TAG, "DoAudioPassthrough() called" + noise_suppressor.getEnabled() );
//
//                if (NoiseSuppressor.create(arec.getAudioSessionId()) == null) {
//                    Log.d(TAG, "noise suppressor failed");
//                }
//                else
//                    Log.d(TAG, "noise suppressor called");

//                if (EnvironmentalReverb.create(arec.getAudioSessionId()) == null) {
//                    Log.d(TAG, "noise suppressor failed");
//                }
//                else
//                    Log.d(TAG, "noise suppressor called");

//                if (AcousticEchoCanceler.create(arec.getAudioSessionId()) == null) {
//                    Log.d(TAG, "AcousticEchoCanceler() failed");
//                } else {
//                    Log.d(TAG, "AcousticEchoCanceler() calles");
//                }

//                LoudnessEnhancer enhancer = new LoudnessEnhancer(atrack.getAudioSessionId());
//                enhancer.setEnabled(true);
//                float curGain = enhancer.getTargetGain();
//                enhancer.setTargetGain(3000);
//                Log.d(TAG, "current gain is：" + curGain);


//                if (NoiseSuppressor.create(atrack.getAudioSessionId()) == null) {
//                    Log.d(TAG, "noise suppressor failed");
//                }
//                else
//                    Log.d(TAG, "noise suppressor called");



//                if (AcousticEchoCanceler.create(arec.getAudioSessionId()) == null) {
//                    Log.d(TAG, "AcousticEchoCanceler() failed");
//                } else {
//                    Log.d(TAG, "AcousticEchoCanceler() calles");
//                }

//                LoudnessEnhancer enhancer = new LoudnessEnhancer(atrack.getAudioSessionId());
//                enhancer.setEnabled(true);
//                float curGain = enhancer.getTargetGain();
//                enhancer.setTargetGain(2000);
//                Log.d(TAG, "current gain is：" + curGain);



                int count = arec.read(buffer, 0, buffersize);

//                doubleAudio = toDoubleArray(buffer);
//                doubleAudio = fourierLowPassFilter(doubleAudio, LOWPASS, FREQ );
//                buffer = toByteArray(doubleAudio);
                atrack.write(buffer, 0, count);
            }

            arec.stop();
            atrack.stop();


//            if (noise_suppressor != null) {
//                noise_suppressor.release();
//                noise_suppressor = null;
//            }

            atrack.release();
            arec.release();

            atrack = null;
            arec = null;
            buffer = null;
        }

        public double[] fourierLowPassFilter(double[] data, double lowPass, double frequency){
            //data: input data, must be spaced equally in time.
            //lowPass: The cutoff frequency at which
            //frequency: The frequency of the input data.

            //The apache Fft (Fast Fourier Transform) accepts arrays that are powers of 2.
            int minPowerOf2 = 1;
            while(minPowerOf2 < data.length)
                minPowerOf2 = 2 * minPowerOf2;

            //pad with zeros
            double[] padded = new double[minPowerOf2];
            for(int i = 0; i < data.length; i++)
                padded[i] = data[i];


            FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
            Complex[] fourierTransform = transformer.transform(padded, TransformType.FORWARD);

            //build the frequency domain array
            double[] frequencyDomain = new double[fourierTransform.length];
            for(int i = 0; i < frequencyDomain.length; i++)
                frequencyDomain[i] = frequency * i / (double)fourierTransform.length;

            //build the classifier array, 2s are kept and 0s do not pass the filter
            double[] keepPoints = new double[frequencyDomain.length];
            keepPoints[0] = 1;
            for(int i = 1; i < frequencyDomain.length; i++){
                Log.d(TAG, "fourierLowPassFilter() called with: data = [" + frequencyDomain[i] + "], lowPass = [" + lowPass + "], frequency = [" + frequency + "]");
                if(frequencyDomain[i] < lowPass)
                    keepPoints[i] = 2;
                else
                    keepPoints[i] = 0;
            }

            //filter the fft
            for(int i = 0; i < fourierTransform.length; i++)
                fourierTransform[i] = fourierTransform[i].multiply((double)keepPoints[i]);

            //invert back to time domain
            Complex[] reverseFourier = transformer.transform(fourierTransform, TransformType.INVERSE);

            //get the real part of the reverse
            double[] result = new double[data.length];
            for(int i = 0; i< result.length; i++){
                result[i] = reverseFourier[i].getReal();
            }

            return result;
        }

        public static byte[] toByteArray(double[] doubleArray){
            int times = Double.SIZE / Byte.SIZE;
            byte[] bytes = new byte[doubleArray.length * times];
            for(int i=0;i<doubleArray.length;i++){
                ByteBuffer.wrap(bytes, i*times, times).putDouble(doubleArray[i]);
            }
            return bytes;
        }

        public static double[] toDoubleArray(byte[] byteArray){
            int times = Double.SIZE / Byte.SIZE;
            double[] doubles = new double[byteArray.length / times];
            for(int i=0;i<doubles.length;i++){
                doubles[i] = ByteBuffer.wrap(byteArray, i*times, times).getDouble();
            }
            return doubles;
        }
    }



}
