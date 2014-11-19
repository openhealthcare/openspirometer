package org.iandavies.android.flowrateexperiment;



import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 *
 */
public class StatsFragment extends DataDisplayFragment {

    View mView = null;

    public StatsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_stats, container, false);
        return mView;
    }

    @Override
    public void updateDisplay() {
        if (mView == null)
            return;

        TableLayout table = (TableLayout) mView.findViewById(R.id.statsTable);

        if (table == null)
            return;

        TableRow tr = new TableRow(mView.getContext());

        tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        
        TextView tv = new TextView(mView.getContext());
        tv.setText("Dynamic Button");
        tv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        tr.addView(tv);
        table.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

    }
}
