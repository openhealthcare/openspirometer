package org.iandavies.android.flowrateexperiment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.ArrayList;

public class TimeFragment extends DataDisplayFragment {

    protected View mView = null;

    public TimeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_time, container, false);
        return mView;
    }

    @Override
    public void updateDisplay() {
        Log.v("TABS", "Updating display of TIME fragment");

        if (mView == null)
            Log.v("TABS", "Abandoning update - mView null");

        if (activity.currentAnalysis == -1) {
            mView.findViewById(R.id.timePlot).setVisibility(View.INVISIBLE);
        } else {
            plotAnalysis(activity.analyses.get(activity.currentAnalysis));
        }
    }

    private void plotAnalysis(final Analysis a) {
        final XYPlot plot = (XYPlot) mView.findViewById(R.id.timePlot);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                plot.setVisibility(View.INVISIBLE);
            }
        });

        new Thread(new Runnable() {
            public void run() {

                if (plot == null) {
                    Log.v("TABS", "Abandoning time plot - couldn't find XYPlot");
                    return;
                }

                final float[] signal = a.signal;
                final int PTS = 2000; // Max number of x-axis samples to draw.

                // Calculate signal vals to plot

                final ArrayList<Float> signalYs = new ArrayList<Float>(signal.length);
                final ArrayList<Float> signalXs = new ArrayList<Float>(signal.length);

                for (int i = 0; i < a.signal.length; i += a.signal.length / PTS) {
                    signalXs.add((float) i / a.sampleRate);
                    signalYs.add(a.signal[i]);
                }

                // Calculate spectrum.

                final ArrayList<Float> spectrumYs = new ArrayList<Float>(a.spectrum.length);
                final ArrayList<Float> spectrumXs = new ArrayList<Float>(a.spectrum.length);

                for (int i = 0; i < a.spectrum.length / 2; i += (a.spectrum.length / 2) / PTS) {
                    spectrumXs.add(((float) 2 * i / a.sampleRate) * ((float) signal.length / a.spectrum.length));
                    // TODO: Scale the spectrum properly. Currently completely arbitrary...
                    spectrumYs.add(a.spectrum[i] / 100 - 1);
                }

                // Plot envelope

                final ArrayList<Float> hilbertYs = new ArrayList<Float>(signal.length);
                final ArrayList<Float> hilbertXs = new ArrayList<Float>(signal.length);

                for (int i = 0; i < signal.length; i += signal.length / PTS) {
                    hilbertXs.add((float) i / a.sampleRate);
                    hilbertYs.add(a.envelope[i]);
                }

                // Plot volume

                final ArrayList<Float> volumeYs = new ArrayList<Float>(signal.length);
                final ArrayList<Float> volumeXs = new ArrayList<Float>(signal.length);

                for (int i = 0; i < signal.length; i += signal.length / PTS) {
                    volumeXs.add((float) i / a.sampleRate);
                    volumeYs.add(2 * a.volume[i] / a.volume[a.volume.length - 1] - 1);
                }

                // Plot calculated start time

                final ArrayList<Float> t0Xs = new ArrayList<Float>(2);
                final ArrayList<Float> t0Ys = new ArrayList<Float>(2);

                t0Xs.add((float)a.peakFlowTimeSamples / a.sampleRate);
                t0Xs.add((float)a.startTimeSamples / a.sampleRate);
                t0Xs.add((float)a.startTimeSamples / a.sampleRate);

                t0Ys.add(2 * a.volume[a.peakFlowTimeSamples] / a.volume[a.volume.length - 1] - 1);
                t0Ys.add(-1f);
                t0Ys.add(1f);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

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

                        plot.addSeries(
                                new SimpleXYSeries(
                                        volumeXs,
                                        volumeYs,
                                        "Volume"),
                                new LineAndPointFormatter(
                                        Color.rgb(80, 80, 255),
                                        null,
                                        null,
                                        null)
                        );

                        plot.addSeries(
                                new SimpleXYSeries(
                                        t0Xs,
                                        t0Ys,
                                        "t0"),
                                new LineAndPointFormatter(
                                        Color.rgb(230, 190, 255),
                                        null,
                                        null,
                                        null)
                        );

                        plot.setRangeBoundaries(-1, 1, BoundaryMode.FIXED);
                        plot.setDomainLabel("Time");

                        plot.redraw();
                        plot.setVisibility(View.VISIBLE);

                    }
                });
            }
        }).start();
    }
}
