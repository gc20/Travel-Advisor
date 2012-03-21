package com.govind.FYP;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class DetailsActivity extends ListActivity {

	public static String TAG = DetailsActivity.class.getSimpleName();
	
	// List adapter used to display road names
	private static DetailsListAdapter dAdapter;
	
	// Variables containing data obtained from the parent activity
	ArrayList <RoadData> tempRoadDetails;
	ArrayList <RoadData> roadDetails;
	ArrayList <RoadDataAggregate> roadDataAggregate;
	
	// Semaphore to prevent concurrent occurrence of use action and data update
	Semaphore updateSem; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		// Initialize views
		super.onCreate(savedInstanceState);
		setContentView(R.layout.details);
        Log.d (TAG, "onCreate");
        
		// Populate list
		dAdapter = new DetailsListAdapter (this, R.layout.detailslist, 
				((MenuTabActivity) (this.getParent())).tempRoadDetails,
				((MenuTabActivity) (this.getParent())).roadDataAggregate);
        setListAdapter(dAdapter);
        
        // Setup semaphore
        updateSem = new Semaphore(1);
	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
        Log.d (TAG, "onStart");

        // Let tab activity handle updating of array list and associated UI
		((MenuTabActivity) (this.getParent())).manageArrayListData(false);
		   Log.d (TAG, "listdata");
		updateUI();
		   Log.d (TAG, "child UI");
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
        Log.d (TAG, "onResume");
        
        // Let tab activity handle updating of array list and associated UI
		((MenuTabActivity) (this.getParent())).manageArrayListData(false);
		   Log.d (TAG, "listdata");
		updateUI();
		   Log.d (TAG, "child UI");
	}



	@Override
	public void onBackPressed() {
		((MenuTabActivity) (this.getParent())).onBackPressed();
		super.onBackPressed();
	}
	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		
		try {updateSem.acquire();
		} catch (InterruptedException e) {
			Log.d(TAG, "UpdateSem Error in onListItemClick");
			e.printStackTrace();
		}
		
		Log.d(TAG, "Item click");
		// Get necessary arrays from parent
		Intent intent = new Intent(this, MapsRoadActivity.class);
		
		try
		{
			// Build necessary arrays and get further data from parent
			RoadData tempRoadData = tempRoadDetails.get(position);
			int len1 = roadDetails.size();
			int len2 = roadDataAggregate.size();
			int SequenceID = tempRoadData.getSequenceID();
			double lat [] = new double [1000];
			double lon [] = new double [1000];
			int i=0, j=0;
			for (i=0; i<len1; i++)
			{
				Log.d(TAG, Integer.toString(i) + " " + Integer.toString(roadDetails.get(i).getSequenceID()));
				if (roadDetails.get(i).getSequenceID() == SequenceID  || 
						((roadDetails.get(i).getSequenceID() == (SequenceID+1)) && roadDetails.get(i).getInternalID() == 1))
				{
					Log.d(TAG, "Inside " + roadDetails.get(i).getLatitude() + " " + roadDetails.get(i).getLongitude());
					lat[j] = roadDetails.get(i).getLatitude();
					lon[j++] = roadDetails.get(i).getLongitude();
					Log.d(TAG, Double.toString(lat[j-1]) + " " + Double.toString(lon[j-1]));
				}
			}
			RoadDataAggregate tempRoadDataAggregate = null;
			for (i=0; i<len2; i++)
			{
				if (SequenceID == roadDataAggregate.get(i).getSequenceID())
				{
					tempRoadDataAggregate = roadDataAggregate.get(i);
					break;
				}
			}
			
			// Send data to map activity
			intent.putExtra("Latitudes", lat);
			intent.putExtra("Longitudes", lon);
			intent.putExtra("Size", j);
			intent.putExtra("RoadName", tempRoadDataAggregate.getRoadName());
			String snippet = tempRoadDataAggregate.getCongestion() +
				"\nAvg. Speed: " + tempRoadDataAggregate.getAvgSpeed() + 
				"\nExpected Time: " + tempRoadDataAggregate.getExpectedTime() +
				"\nDistance: " + tempRoadDataAggregate.getDistance() +
				"\nSpeed Limit: " + tempRoadDataAggregate.getSpeedLimit() +
				"\nPrediction: " + tempRoadDataAggregate.getConfidence();
			intent.putExtra("Snippet", snippet);
			updateSem.release();
		}
		catch (Exception e)
		{ Log.d(TAG, "Error in onListItemClick");
			updateSem.release();}
		
		try
		{startActivity(intent);}
		catch (Exception e)
		{ Log.d (TAG, "Could not start map activity");}
	}

	public void updateUI ()
	{
		try {updateSem.acquire();
		} catch (InterruptedException e) {
			Log.d(TAG, "UpdateSem Error in updateUI");
			e.printStackTrace();
		}
		
		try
		{
			// Refresh list adapter view
			dAdapter.notifyDataSetChanged();
	        Log.d(TAG, "UpdateUI");
	        
	        // Get necessary arrays from parent
			tempRoadDetails = ((MenuTabActivity) (this.getParent())).tempRoadDetails;
			roadDetails = ((MenuTabActivity) (this.getParent())).roadDetails;
			roadDataAggregate = ((MenuTabActivity) (this.getParent())).roadDataAggregate;
			
			updateSem.release();
		}
		catch (Exception e)
		{ Log.d(TAG, "Exception in updateUI)");
		updateSem.release();}
	}
	
	public Handler handlerUpdate = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			updateUI();
		}
	};
}