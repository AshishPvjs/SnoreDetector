package com.example.lenovo.snoredetect;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static int SAMPLE_RATE = 44000; //The Sampling Rate
    boolean mShouldContinue = false; // Indicates if recording / playback should stop
    private String LOG_TAG = "FFS";
    private Button start;
    private FFT fft;
    private TextView snore;
    private int snoretime =0;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fft = new FFT(1024);
        snore = (TextView)findViewById(R.id.snore);
        start = (Button)findViewById(R.id.start_button);
        requestPermission();
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mShouldContinue = !mShouldContinue;
                if(mShouldContinue){
                    Audio_Recording();
                    image = (ImageView)findViewById(R.id.sleeping_pic);
                }
            }
        });


    }
    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    void Audio_Recording() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    bufferSize = SAMPLE_RATE * 2;
                }
                short[] audioBuffer = new short[bufferSize / 2];
                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
                if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(LOG_TAG, "Audio Cannot be Recorded");
                    return;
                }
                record.startRecording();

                Log.v(LOG_TAG, "Recording has started");

                long shortsRead = 0;
                while (mShouldContinue) {
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                    shortsRead += numberOfShort;
                    double[] y = new double[1024];
                    double[] x = short_To_Double(audioBuffer);
                    fft.fast_fourier(x,y);
                    int j = PES(x);
                    if(j ==1){
                        snoretime++;
                        if(snoretime>5) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    snore.setText("Please stop snoring!!!");
                                }
                            });
                            snoretime =0;
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }else {
                        snoretime = 0;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                snore.setText("Have a Healthy Sleep");
                            }
                        });

                    }
                }

                record.stop();
                record.release();
                Log.v(LOG_TAG, String.format("Recording  has stopped. Samples read: %d", shortsRead));
            }
        }).start();
    }
    public int PES(double[] a){
        double E_L =0,E_H =0;
        for(int i=0;i<52;i++){
            E_L = E_L + a[i]*a[i];
        }
        for(int i=53;i<512;i++){
            E_H = E_H + a[i]*a[i];
        }
        double pes = E_L/(E_L+E_H);
        if(pes <0.65)
            return  1;
        else
            return 0;
    }
    public double[] short_To_Double(short[] x){
        double y[] =  new double[x.length];
        for(int  i=0;i<x.length;i++){
            y[i] = (double)x[i];
        }
        return  y;
    }

}