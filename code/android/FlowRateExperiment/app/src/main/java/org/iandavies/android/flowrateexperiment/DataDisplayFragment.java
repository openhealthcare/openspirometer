package org.iandavies.android.flowrateexperiment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Ian on 24/10/2014.
 */
public abstract class DataDisplayFragment extends Fragment {

    protected FlowRate activity = null;

    public abstract void updateDisplay();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = (FlowRate)activity;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDisplay();
    }

}
