package org.iandavies.android.flowrateexperiment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.ArrayList;


public class VolumeFragment extends DataDisplayFragment {

    protected View mView = null;

    public VolumeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_volume, container, false);
        return mView;
    }

    @Override
    public void updateDisplay() {
        Log.v("TABS", "Updating display of VOLUME fragment");

        if (mView == null)
            Log.v("TABS", "Abandoning update - mView null");

        if (activity.currentAnalysis == -1) {
            mView.findViewById(R.id.timePlot).setVisibility(View.INVISIBLE);
        } else {
            plotAnalysis(activity.analyses.get(activity.currentAnalysis));
        }
    }

    private void plotAnalysis(final Analysis a) {
        final XYPlot plot = (XYPlot) mView.findViewById(R.id.volumePlot);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                plot.setVisibility(View.INVISIBLE);
            }
        });

        new Thread(new Runnable() {
            public void run() {

                if (plot == null) {
                    Log.v("TABS", "Abandoning volume plot - couldn't find XYPlot");
                    return;
                }

                final float[] signal = a.signal;
                final int PTS = 2000; // Max number of x-axis samples to draw.

                // Calculate volume-flow curve

                final ArrayList<Float> volumes = new ArrayList<Float>(signal.length);
                final ArrayList<Float> flows = new ArrayList<Float>(signal.length);

                for (int i = 0; i < a.signal.length; i += a.signal.length / PTS) {
                    volumes.add(a.volume[i]);
                    flows.add(a.flow[i]);
                }


                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        plot.clear();
                        plot.addSeries(
                                new SimpleXYSeries(
                                        volumes,
                                        flows,
                                        "Flow"),
                                new LineAndPointFormatter(
                                        Color.rgb(80, 80, 255),
                                        null,
                                        null,
                                        null)
                        );

                        plot.setDomainLabel("Volume");
                        plot.redraw();
                        plot.setVisibility(View.VISIBLE);

                    }
                });
            }
        }).start();
    }
}
