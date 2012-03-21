package com.govind.FYP;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class DashboardActivity extends ListActivity {

	public static String TAG = DashboardActivity.class.getSimpleName();
	
	// List adapter used to display road names
	private static DashboardListAdapter dAdapter;
	
	// Variables containing data obtained from the parent activity
	ArrayList <RoadData> tempRoadDetails;
	ArrayList <RoadData> roadDetails;
	ArrayList <RoadDataAggregate> roadDataAggregate;
	String roadOverall [];
	
	// Display components
	TextView estSpd;
	TextView estTim;
	TextView estDst;
	
	// Semaphore to prevent concurrent occurrence of use action and data update
	//Semaphore updateSem; 
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// Initialize views
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dashboard);
		Log.d (TAG, "onCreate");

        // Populate current estimates
		estSpd = (TextView) findViewById(R.id.valueEstimatedAverageSpeed);
		estTim = (TextView) findViewById(R.id.valueEstimatedTime);
		estDst = (TextView) findViewById(R.id.valueEstimatedDistance);
		roadOverall = ((MenuTabActivity) (this.getParent())).roadOverall;
		estSpd.setText(roadOverall[0]);
		estTim.setText(roadOverall[1]);
		estDst.setText(roadOverall[2]);
		
		// Populate list
		dAdapter = new DashboardListAdapter (this, R.layout.dashboardlist, 
				((MenuTabActivity) (this.getParent())).tempRoadDetails,
				((MenuTabActivity) (this.getParent())).roadDataAggregate);
        setListAdapter(dAdapter);
        
        // Setup semaphore
        //updateSem = new Semaphore(1);
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
		Log.d (TAG, "On Back");
		((MenuTabActivity) (this.getParent())).onBackPressed();
		super.onBackPressed();
	}
	

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		/*try {updateSem.acquire();
		} catch (InterruptedException e) {
			Log.d(TAG, "UpdateSem Error in onListItemClick");
			e.printStackTrace();
		}*/
		
		Log.d(TAG, "Item click");
		// Intent to launch associated map
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
				//Log.d(TAG, Integer.toString(i) + " " + Integer.toString(roadDetails.get(i).getSequenceID()));
				if (roadDetails.get(i).getSequenceID() == SequenceID || 
						((roadDetails.get(i).getSequenceID() == (SequenceID+1)) && roadDetails.get(i).getInternalID() == 1))
				{
					Log.d(TAG, "Inside " + roadDetails.get(i).getLatitude() + " " + roadDetails.get(i).getLongitude());
					lat[j] = roadDetails.get(i).getLatitude();
					lon[j++] = roadDetails.get(i).getLongitude();
					//Log.d(TAG, Double.toString(lat[j-1]) + " " + Double.toString(lon[j-1]));
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
			intent.putExtra("Snippet", tempRoadDataAggregate.getCongestion());
			//updateSem.release();
		}
		catch (Exception e)
		{ Log.d(TAG, "Error in onListItemClick");
			//updateSem.release();
		}
		
		try
		{startActivity(intent);}
		catch (Exception e)
		{ Log.d (TAG, "Could not start map activity");}
	}

	public void updateUI ()
	{
		/*try {updateSem.acquire();
		} catch (InterruptedException e) {
			Log.d(TAG, "UpdateSem Error in updateUI");
			e.printStackTrace();
		}*/
		
		try
		{
			// Get necessary arrays from parent
			tempRoadDetails = ((MenuTabActivity) (this.getParent())).tempRoadDetails;
			roadDetails = ((MenuTabActivity) (this.getParent())).roadDetails;
			roadDataAggregate = ((MenuTabActivity) (this.getParent())).roadDataAggregate;
			roadOverall = ((MenuTabActivity) (this.getParent())).roadOverall;
			
			// Populate list
			dAdapter.notifyDataSetChanged();
			Log.d(TAG, "UpdateUI");
	        
	        // Refresh textviews
			estSpd.setText(roadOverall[0]);
			estTim.setText(roadOverall[1]);
			estDst.setText(roadOverall[2]);
			
			//updateSem.release();
		}
		catch (Exception e)
		{ Log.d(TAG, "Exception in updateUI)");
		//updateSem.release();
		}
	}
	
	public Handler handlerUpdate = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			updateUI();
		}
	};
	
}