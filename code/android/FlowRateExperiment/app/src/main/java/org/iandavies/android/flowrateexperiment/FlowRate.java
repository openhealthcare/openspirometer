package org.iandavies.android.flowrateexperiment;

import android.app.Activity;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYSeriesFormatter;
import com.badlogic.gdx.audio.analysis.FFT;

import android.media.AudioRecord;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;

import biz.source_code.dsp.filter.FilterCharacteristicsType;
import biz.source_code.dsp.filter.FilterPassType;
import biz.source_code.dsp.filter.IirFilter;
import biz.source_code.dsp.filter.IirFilterCoefficients;
import biz.source_code.dsp.filter.IirFilterDesignFisher;

public class FlowRate extends Activity {

    final int SAMPLE_RATE = 22050;
    final int BUFFER_SECS = 10;

    AudioRecord audioRecord;
    short[] buffer = new short[SAMPLE_RATE * BUFFER_SECS];

    int samplesRead = 0;
    volatile boolean stopRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_rate);

        final Activity activity = this;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,22050, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 1024);

        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnStart_click();
            }
        });

        findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnStop_click();
            }
        });
    }

    private void btnStart_click() {
        Log.v("BUTTON", "Start");

        final Activity activity = this;
        audioRecord.startRecording();

        findViewById(R.id.plot).setVisibility(View.INVISIBLE);
        findViewById(R.id.btnStart).setEnabled(false);
        findViewById(R.id.btnStop).setEnabled(true);

        new Thread(new Runnable() {
            public void run() {

                for (int i = 0; i < buffer.length; i++)
                    buffer[i] = 0;
                samplesRead = 0;
                stopRecording = false;

                while (samplesRead < buffer.length && !stopRecording) {
                    samplesRead += audioRecord.read(buffer, samplesRead, buffer.length - samplesRead);
                }

                audioRecord.stop();
                Log.v("Stopping", "Stopped recording");

                float[] signalChunk = new float[samplesRead];
                for (int i = 0; i < signalChunk.length; i++)
                    signalChunk[i] = (float)buffer[i] / 32768f;

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.btnStart).setEnabled(true);
                        findViewById(R.id.btnStop).setEnabled(false);

                        Log.v("STOPPING", "STOPPED");
                    }
                });

                analyse(signalChunk);
            }
        }).start();
    }

    private void btnStop_click() {
        Log.v("BUTTON", "Stop");
        stopRecording = true;
    }

    private void analyse(final float[] signal) {
        Log.v("ANALYSING", "Analysing signal of length " + signal.length);

        final int PTS = 2000; // Max number of x-axis samples to draw.

        // Calculate signal vals to plot

        final ArrayList<Float> signalYs = new ArrayList<Float>(signal.length);
        final ArrayList<Float> signalXs = new ArrayList<Float>(signal.length);

        for (int i = 0; i < signal.length; i+= signal.length / PTS) {
            signalXs.add((float)i / SAMPLE_RATE);
            signalYs.add(signal[i]);
        }

        // Calculate spectrum.

        int paddedLength = (int)Math.pow(2, Math.ceil(Math.log(signal.length) / Math.log(2)));
        float[] signalPadded = new float[paddedLength];
        for (int i = 0; i < signal.length; i++)
            signalPadded[i] = signal[i];

        FFT fft = new FFT(paddedLength, SAMPLE_RATE);
        fft.noAverages();
        fft.forward(signalPadded);

        float[] real = fft.getRealPart();
        float[] imag = fft.getImaginaryPart();

        final ArrayList<Float> spectrumYs = new ArrayList<Float>(signalPadded.length);
        final ArrayList<Float> spectrumXs = new ArrayList<Float>(signalPadded.length);

        for (int i = 0; i < signalPadded.length / 2; i+= (signalPadded.length/2) / PTS) {
            spectrumXs.add((float)2*i / SAMPLE_RATE);
            // TODO: Scale the spectrum properly. Currently completely arbitrary...
            spectrumYs.add((float)Math.sqrt(real[i] * real[i] + imag[i] * imag[i]) / 100 - 1);
        }

        // Calculate envelope

        float[] mags = magnitudeOfTheAnalyticFunctionDefinitely(signalPadded);

        // Filter envelope

        IirFilterCoefficients coeff = IirFilterDesignFisher.design(FilterPassType.lowpass, FilterCharacteristicsType.butterworth, 4, 0, 26.0 / SAMPLE_RATE, 0);
        IirFilter filter = new IirFilter(coeff);

        float[] hFiltered = new float[paddedLength];
        for (int i = 0; i < paddedLength; i++) {
            hFiltered[i] = (float)filter.step(mags[i]);
        }

        final ArrayList<Float> hilbertYs = new ArrayList<Float>(signal.length);
        final ArrayList<Float> hilbertXs = new ArrayList<Float>(signal.length);

        for (int i = 0; i < signal.length; i+= signal.length / PTS) {
            hilbertXs.add((float)i / SAMPLE_RATE);
            hilbertYs.add(hFiltered[i]);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                XYPlot plot = (XYPlot) findViewById(R.id.plot);

                plot.clear();
                plot.addSeries(
                        new SimpleXYSeries(
                                signalXs,
                                signalYs,
                                "Signal"),
                        new LineAndPointFormatter(
                                Color.rgb(128, 255, 128),
                                null,
                                null,
                                null)
                );


                plot.addSeries(
                        new SimpleXYSeries(
                                hilbertXs,
                                hilbertYs,
                                "Envelope"),
                        new LineAndPointFormatter(
                                Color.rgb(255, 128, 128),
                                null,
                                null,
                                null)
                );

                plot.addSeries(
                        new SimpleXYSeries(
                                spectrumXs,
                                spectrumYs,
                                "Spectrum"),
                        new LineAndPointFormatter(
                                Color.rgb(255, 255, 80),
                                null,
                                null,
                                null)
                );
                plot.setRangeBoundaries(-1, 1, BoundaryMode.FIXED);

                plot.redraw();
                plot.setVisibility(View.VISIBLE);

            }
        });
    }

    public static Float[] box(float[] floatArray) {
        Float[] box = new Float[floatArray.length];
        for (int i = 0; i < box.length; i++) {
            box[i] = floatArray[i];
        }
        return box;
    }


    private float[] magnitudeOfTheAnalyticFunctionDefinitely(float[] signal) {

        int N = signal.length;

        FFT fft = new FFT(N, 0);
        fft.noAverages();
        fft.forward(signal);

        float[] real = fft.getRealPart();
        float[] imag = fft.getImaginaryPart();

        float[] h = new float[N];

        h[0] = 1;
        h[N/2 + 1] = 1;

        for (int i = 2; i <= N/2; i++)
            h[i] = 2;

        for (int i = 0; i < N; i++)
            real[i] *= h[i];

        float[] result = new float[N];

        fft.inverse(real, imag, result);

        real = fft.getRealPart();
        imag = fft.getImaginaryPart();
        for (int i = 0; i < N; i++)
            result[i] = (float)Math.sqrt(real[i] * real[i] + imag[i] * imag[i]) / N;

        return result;
    }



    /**
     * @author Roger Chappl
     * Calculates N-point fft on array
     *
     */
    private float[] fft(int N, int fs, int startIndex, float[] array) {
        float[] fft_cpx, tmpr, tmpi;
        float[] res = new float[N / 2];
        // float[] mod_spec =new float[array.length/2];
        float[] real_mod = new float[N];
        float[] imag_mod = new float[N];
        double[] real = new double[N];
        double[] imag = new double[N];
        float[] mag = new float[N];
        double[] phase = new double[N];
        float[] new_array = new float[N];
        // Zero Pad and signal
        for (int i = startIndex; i < N + startIndex; i++) {

            if (i < array.length) {
                new_array[i] = array[i];
            } else {
                new_array[i] = 0;
            }
        }

        FFT fft = new FFT(N, fs);

        fft.forward(new_array);
        fft_cpx = fft.getSpectrum();
        tmpi = fft.getImaginaryPart();
        tmpr = fft.getRealPart();
        for (int i = 0; i < new_array.length; i++) {
            real[i] = (double) tmpr[i];
            imag[i] = (double) tmpi[i];

            mag[i] = (float)Math.sqrt((real[i] * real[i]) + (imag[i] * imag[i]));
            //phase[i] = Math.atan2(imag[i], real[i]);

            /**** Reconstruction ****/
            //real_mod[i] = (float) (mag[i] * Math.cos(phase[i]));
            //imag_mod[i] = (float) (mag[i] * Math.sin(phase[i]));

        }
        //fft.inverse(real_mod, imag_mod, res);
        //return res;
        return mag;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.flow_rate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
