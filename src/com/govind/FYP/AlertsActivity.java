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

public class AlertsActivity extends ListActivity{

	private static String TAG = AlertsActivity.class.getSimpleName();
	
	// List adapter used to display alerts
	AlertsListAdapter dAdapter;
	
	// Stores list of alerts
	public ArrayList <RoadAlerts> roadAlerts = new ArrayList <RoadAlerts> ();
	
	// Semaphore to prevent concurrent occurrence of use action and data update
	//Semaphore updateSem; 
	
    @Override
	public void onCreate(Bundle savedInstanceState) {

    	// Initialize views
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alerts);
		Log.d(TAG, "On create");
		
		// Populate list
        dAdapter= new AlertsListAdapter (this, R.layout.alertslist, 
				((MenuTabActivity) (this.getParent())).roadAlerts);
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
		
		//super.onListItemClick(l, v, position, id);
		//String selection = l.getItemAtPosition(position).toString();
		//Toast.makeText(this, selection + " " + roadAlerts.get(position).getAlertMessage().toString(), Toast.LENGTH_LONG).show();
		Intent intent = new Intent(AlertsActivity.this, AlertsMapsActivity.class);
		try
		{
			RoadAlerts tempRoadAlerts = ((MenuTabActivity) (AlertsActivity.this.getParent())).roadAlerts.get(position);
			intent.putExtra("IncidentMessage", tempRoadAlerts.getAlertMessage().toString());
			intent.putExtra("IncidentTime", tempRoadAlerts.getAlertTime().toString());
			intent.putExtra("Latitude", Double.toString(tempRoadAlerts.getLatitude()));
			intent.putExtra("Longitude", Double.toString(tempRoadAlerts.getLongitude()));
			//updateSem.release();
		}
		catch (Exception e)
		{ Log.d(TAG, "Error in onListItemClick");
		}//updateSem.release();}
		
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
			// Get road alerts from parent
			roadAlerts = ((MenuTabActivity)this.getParent()).roadAlerts;
			
			// Update adapter
			dAdapter.notifyDataSetChanged();
			Log.d(TAG, "UpdateUI");
	        
	        // Release update semaphore
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