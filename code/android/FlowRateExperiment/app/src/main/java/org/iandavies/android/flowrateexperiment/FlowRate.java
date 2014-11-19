package org.iandavies.android.flowrateexperiment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;

public class FlowRate extends FragmentActivity {

    // TODO: Decide what sample rate we want.
    final public int SAMPLE_RATE = 22050;

    // TODO: Use a dynamic buffer. For now, we're limited to capturing this many seconds in one go.
    final int BUFFER_SECS = 10;

    AudioRecord audioRecord;
    short[] buffer = new short[SAMPLE_RATE * BUFFER_SECS];

    int samplesRead = 0;
    volatile boolean stopRecording = false;

    ArrayList<Analysis> analyses = new ArrayList<Analysis>();
    int currentAnalysis = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_flow_rate);

        setupTabs();

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,22050, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 2048);

        updateButtons();
        updateTabs();
    }

    public void btnPrevious_click(View view) {
        if (currentAnalysis > 0)
            currentAnalysis--;
        updateButtons();
        updateTabs();
    }

    public void btnNext_click(View view) {
        if (currentAnalysis < analyses.size() - 1)
            currentAnalysis++;
        updateButtons();
        updateTabs();
    }

    public void btnDelete_click(View view) {

        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Deleting Recording")
            .setMessage("Are you sure you want to delete this recording?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (currentAnalysis > -1) {
                        analyses.remove(currentAnalysis);
                        currentAnalysis--;
                    }
                    updateButtons();
                    updateTabs();
                }
            })
            .setNegativeButton("No", null)
            .show();
}

    public void updateTitle() {
        if (currentAnalysis > -1) {
            getActionBar().setTitle("Recording " + (currentAnalysis + 1) + " of " + analyses.size());
        } else {
            getActionBar().setTitle("Open Peak Flow");
        }

    }

    public void updateTabs() {

        // Force all the data tabs to update their displays.
        String[] fragmentTags = {"time", "volume", "stats"};
        for (String tag : fragmentTags) {
            DataDisplayFragment f = (DataDisplayFragment) getFragmentManager().findFragmentByTag(tag);
            if (f != null) {
                f.updateDisplay();
            }
        }
    }

    public void updateButtons() {
        if (analyses.size() == 0) {
            findViewById(R.id.btnNext).setEnabled(false);
            findViewById(R.id.btnPrevious).setEnabled(false);
            findViewById(R.id.btnDelete).setEnabled(false);
        } else {
            findViewById(R.id.btnDelete).setEnabled(true);
            findViewById(R.id.btnPrevious).setEnabled(currentAnalysis > 0);
            findViewById(R.id.btnNext).setEnabled(currentAnalysis < analyses.size() - 1);
        }
        updateTitle();
    }

    private void setupTabs() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayShowTitleEnabled(true);

        ActionBar.Tab tab1 = actionBar
                .newTab()
                .setText("Time")
                .setTabListener(
                        new TabListener<TimeFragment>(this, "time",
                                TimeFragment.class));

        actionBar.addTab(tab1);
        actionBar.selectTab(tab1);

        ActionBar.Tab tab2 = actionBar
                .newTab()
                .setText("Volume")
                .setTabListener(
                        new TabListener<VolumeFragment>(this, "volume",
                                VolumeFragment.class));

        actionBar.addTab(tab2);

        ActionBar.Tab tab3 = actionBar
                .newTab()
                .setText("Stats")
                .setTabListener(
                        new TabListener<StatsFragment>(this, "stats",
                                StatsFragment.class));

        actionBar.addTab(tab3);
    }

    public void btnStart_click(View view) {

        final Activity activity = this;
        audioRecord.startRecording();

        findViewById(R.id.btnStart).setVisibility(View.INVISIBLE);
        findViewById(R.id.btnStop).setVisibility(View.VISIBLE);

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
                        findViewById(R.id.btnStart).setVisibility(View.VISIBLE);
                        findViewById(R.id.btnStop).setVisibility(View.INVISIBLE);

                        Log.v("STOPPING", "STOPPED");
                    }
                });

                analyse(signalChunk);
            }
        }).start();
    }

    public void btnStop_click(View view) {
        stopRecording = true;
    }

    private void analyse(final float[] signal) {
        Log.v("ANALYSING", "Analysing signal of length " + signal.length);

        Analysis a = new Analysis(signal, SAMPLE_RATE);

        analyses.add(a);

        currentAnalysis = analyses.size() - 1;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateButtons();
                updateTabs();
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.flow_rate, menu);

        //menu.findItem(R.id.action_settings).setEnabled(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_quit) {
            android.os.Process.killProcess(android.os.Process.myPid());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
