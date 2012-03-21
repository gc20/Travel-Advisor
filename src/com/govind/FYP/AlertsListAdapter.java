package com.govind.FYP;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AlertsListAdapter extends ArrayAdapter<RoadAlerts> {
	
	private ArrayList <RoadAlerts> items;
	private Context context;

    public AlertsListAdapter(Context context, int textViewResourceId, ArrayList<RoadAlerts> items) {
    	super(context, textViewResourceId, items);
    	this.context = context;
        this.items = items;
    }

    @Override
    public View getView (int position, View convertView, ViewGroup parent) {
    	View v = convertView;
        if (v == null) {
        	LayoutInflater vi = (LayoutInflater) (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
            v = vi.inflate(R.layout.alertslist, null);
        }
        
        RoadAlerts rd = items.get(position);
        if (rd != null) {
			
        	TextView rd1 = (TextView) v.findViewById(R.id.alertsList1);
			TextView rd2 = (TextView) v.findViewById(R.id.alertsList2);
			
			if (rd1 != null) {
				rd1.setText(rd.getAlertTime());
			}
			if (rd2 != null) {
				rd2.setText(rd.getAlertMessage());
			}
        }
        return v;
   }
}
