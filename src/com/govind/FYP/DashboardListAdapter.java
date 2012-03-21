package com.govind.FYP;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DashboardListAdapter extends ArrayAdapter<RoadData> {

	private ArrayList <RoadData> tempRoadDetails;
	private ArrayList <RoadDataAggregate> roadDataAggregate;

	// Constructor
    public DashboardListAdapter(Context context, int textViewResourceId, ArrayList<RoadData> tempRoadDetails, ArrayList<RoadDataAggregate> roadDataAggregate) {
    	super(context, textViewResourceId, tempRoadDetails);
        this.tempRoadDetails = tempRoadDetails;
        this.roadDataAggregate = roadDataAggregate;
    }

    
    @Override
    public View getView (int position, View convertView, ViewGroup parent) {
    	
    	// Inflate view
    	View v = convertView;
        if (v == null) {
        	LayoutInflater vi = (LayoutInflater) ((Context) getContext()).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.dashboardlist, null);
        }
        
        // Get relevant data objects
        RoadData rd = tempRoadDetails.get(position);
    	RoadDataAggregate rda = null;
        
        if (rd != null) {
        	
        	// Get aggregate string element
            int len = roadDataAggregate.size();
            for (int i=0; i<len; i++)
            {
            	if (roadDataAggregate.get(i).getSequenceID() == rd.getSequenceID())
            	{
            		rda = roadDataAggregate.get(i);
            		break;
            	}
            }
            
            // Get text views
            TextView rd1 = (TextView) v.findViewById(R.id.dashboardList1);
            TextView rd2 = (TextView) v.findViewById(R.id.dashboardList2);
            
            if (rd1 != null) {
            	rd1.setText(rd.getRoadName());                            
            }
            
            // Change colour of text based on nature of congestion
            if (rd2 != null){

				String congestion = rda.getCongestion();
				rd2.setText (congestion);
				
				if (congestion.equals("Very Congested")) {
					rd2.setTextColor(Color.RED);
				}
				if (congestion.equals("Slightly Congested")) {
					rd2.setTextColor(Color.YELLOW);
				}
				if (congestion.equals("Traffic Moving Freely")) {
					rd2.setTextColor(Color.GREEN);
				}
				if (congestion.equals("Congestion Unknown")) {
					rd2.setTextColor(Color.WHITE);
				}
			}
        }
        return v;
   }
}
