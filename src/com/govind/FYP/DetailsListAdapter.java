package com.govind.FYP;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DetailsListAdapter extends ArrayAdapter<RoadData> {
	
	public final String TAG = DetailsListAdapter.class.getSimpleName();
	private ArrayList <RoadData> tempRoadDetails;
	private ArrayList <RoadDataAggregate> roadDataAggregate;

	// Constructor
    public DetailsListAdapter(Context context, int textViewResourceId, ArrayList<RoadData> tempRoadDetails, ArrayList<RoadDataAggregate> roadDataAggregate) {
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
            v = vi.inflate(R.layout.detailslist, null);
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
            Log.d(TAG, rd.getRoadName() + " " + rda.getRoadName());
            
            // Get text views
        	TextView rd1 = (TextView) v.findViewById(R.id.detailsList1);
			TextView rd2 = (TextView) v.findViewById(R.id.detailsList2);
			TextView rd3 = (TextView) v.findViewById(R.id.detailsList3);
			TextView rd4 = (TextView) v.findViewById(R.id.detailsList4);
			TextView rd5 = (TextView) v.findViewById(R.id.detailsList5);
			TextView rd6 = (TextView) v.findViewById(R.id.detailsList6);
			TextView rd7 = (TextView) v.findViewById(R.id.detailsList7);
			
			if (rd1 != null) {
				String congestion = rda.getCongestion();
				rd1.setText (rd.getRoadName());
				
				if (congestion.equals("Very Congested")) {
					rd1.setTextColor(Color.RED);
				}
				if (congestion.equals("Slightly Congested")) {
					rd1.setTextColor(Color.CYAN);
				}
				if (congestion.equals("Traffic Moving Freely")) {
					rd1.setTextColor(Color.GREEN);
				}
				if (congestion.equals("Congestion Unknown")) {
					rd1.setTextColor(Color.WHITE);
				}
			}
			if (rd2 != null) {
				rd2.setText("Avg. Speed: " + rda.getAvgSpeed());
			}
			if (rd3 != null) {
				rd3.setText("Estimated Time: " + rda.getExpectedTime());
			}
			if (rd4 != null) {
				rd4.setText("Speed Limit: " + rda.getSpeedLimit());
			}
			if (rd5 != null){

				String confidence = rda.getConfidence();
				rd5.setText ("Prediction: " + confidence);
				
				/*if (confidence.equals("Not Confident")) {
					rd5.setTextColor(Color.RED);
				}
				if (confidence.equals("Confident")) {
					rd5.setTextColor(Color.YELLOW);
				}
				if (confidence.equals("Very Confident")) {
					rd5.setTextColor(Color.GREEN);
				}
				if (confidence.equals("Confidence Unknown")) {
					rd5.setTextColor(Color.WHITE);
				}*/
			}
			if (rd6 != null) {
				rd6.setText(rda.getCongestion());
			}
			if (rd7 != null) {
				rd7.setText("Distance: " + rda.getDistance());
			}
        }
        return v;
   }
}
