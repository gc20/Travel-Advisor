package com.govind.FYP;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class WelcomeActivity extends Activity implements OnClickListener, Runnable {

	// Debug tag and view variables
	private static String TAG = WelcomeActivity.class.getSimpleName();
	Button buttonProceed;
	EditText edittextOrigin;
	EditText edittextDestination;
	
	// Loading dialog
	ProgressDialog dialog;
	
	// User's entered values
	String textOrigin = "";
	String textDestination = "";
	
	// Message that updates user on processing status
	String message = "";
	
	// Thread to carry out geocoding
	Thread thread;
	
	// Variable that store geocoding results
	double originLat = 0.0;
	double originLon = 0.0;
	double destinationLat = 0.0;
	double destinationLon = 0.0;
    
	// Stores user's current location. 0.0 on both coordinates is considered to imply unavailability of location
	double currentLat = 0.0;
	double currentLon = 0.0;
	
	// Singapore's borders
	double downLat = 1.2372390405371851;
	double upLat = 1.4720060101903352;
	double leftLon = 103.61000061035156;
	double rightLon = 104.04190063476562;
	
	// Debug parameters
	boolean debugGeocoding = false; // false -> normal, true -> force data
	double debugOriginLat = 1.3124816;
	double debugOriginLon = 103.8379255;
	double debugDestinationLat = 1.314769;
	double debugDestinationLon = 103.857449;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	Log.d(TAG, "On create");
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
		
        buttonProceed = (Button) findViewById(R.id.buttonProceed);
        buttonProceed.setOnClickListener(this);

        edittextOrigin = (EditText) findViewById(R.id.originEditText);
        edittextDestination = (EditText) findViewById(R.id.destinationEditText);
		
		// If activity was initiated by MenuTab, obtain currentlat and currentlon
        try {
	    	Bundle extras = getIntent().getExtras();
	    	if (extras != null) {
		   	    currentLat = extras.getDouble("currentLat");
		    	currentLon = extras.getDouble("currentLon");
	    	}
	    } 
	    catch (Exception e) {
	    	e.printStackTrace();
			Log.d(TAG, "Error in extracting current coordinates from intent bundle");
	    }
	    
	    // Register location listener
        registermLocationReceiver();
    }
        
	@Override
	protected void onDestroy() {
		stopService(new Intent (WelcomeActivity.this, SensorService.class));
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
		unregistermLocationReceiver();
		super.onDestroy();
	}
    

    @Override
	protected void onPause() {
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
		super.onPause();
	}

	public void onClick(View v) {
    	
		Log.d(TAG, "On click proceed");
		Log.d(TAG, this.getPackageName());
		boolean SensorStatus = isMyServiceRunning(this.getPackageName() + ".SensorService");
		Log.d(TAG, SensorStatus + "");
		
		// Clear message
		message = "";
		
		// Check sensor statuses
		if (SensorStatus == false)
			message = "Turn on Sensors from Menu (button) to Proceed";
		
		// Geocode and proceed if sensors are turned on appropriately
		if (message.equals("")) // If GPS and accelerometer conditions pass
		{
			try 
	     	{
	     		// Show dialog message
	     		dialog = ProgressDialog.show(WelcomeActivity.this, "", "Geocoding. Please wait...", false);

	     		// Store user entries
	     		textOrigin = edittextOrigin.getText().toString();
	     		textDestination = edittextDestination.getText().toString();
	     		
	     		// Start dialog thread
	     		if (thread != null && thread.isAlive())
	     			thread.destroy();
	     		thread = new Thread (this);
	     		if (thread != null && thread.isAlive())
	     			return;
	     		thread.start();
	     	}
			catch (Exception e) {
				Log.d (TAG, "Exception in onClick");
				if (dialog != null && dialog.isShowing())
					dialog.dismiss();
			}
		}
		else {
 			Log.d(TAG, message);
 			AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
 			builder.setMessage(message).setCancelable(false)
 			       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
 			           public void onClick(DialogInterface dialog, int id) {
 			        	   dialog.cancel();
 			           }
 				       });
 			AlertDialog alert = builder.create();
 			alert.show();
		}
	}
	
    @Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	private boolean isMyServiceRunning(String serviceName) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
        	Log.d(TAG, "Service: " + service.service.getClassName());
            if (serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



	// When "menu" is selected
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.welcomemenu, menu);
		return true;
	}
	

	// When menu options are selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		
		case R.id.setting:
			startActivity(new Intent(this, PrefsActivity.class));
			break;
			
		case R.id.sensorServiceStart:// Open GPS menu if disabled
			try
			{
				String serviceMessage = "Sensor service started";
				LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
				if(!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER ) || !locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER))
				{
					Toast.makeText(this, "Turn on 'Use Wireless Networks' and 'Use GPS Satellites' for better quality", Toast.LENGTH_LONG).show();
					Intent myIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
				    startActivity(myIntent);
				    serviceMessage = "Sensor service will have started when you return";
				}
				startService(new Intent(this, SensorService.class));
				Toast.makeText(this, serviceMessage, Toast.LENGTH_SHORT).show();
				registermLocationReceiver();
				break;
			}
			catch (Exception e)
			{e.printStackTrace();
			Log.d (TAG, "Exception in sensorServiceStart");}

		case R.id.sensorServiceStop:
			unregistermLocationReceiver();
			stopService(new Intent(this, SensorService.class));
			Toast.makeText(this, "Sensor service stopped. Please turn off GPS to save power.", Toast.LENGTH_SHORT).show();
			break;
	
		default:
			break;
		}

		return true;
	}
	

	@Override
	public void run() {
		
		// Variable to ensure that valid addresses were entered
		boolean successCheck = true;
		
		// Variables that store geocoded values, if required
		Geocoder geocoder = new Geocoder(this);
		List<Address> address;
     	
		try
		{
			// Geocode the origin
			// If user has entered current location
			if (textOrigin.toLowerCase().equals("current location") ||
					textOrigin.toLowerCase().equals("here")) 
			{
				// Action taken depends on whether current location is available
				if (currentLat == 0.0 && currentLon == 0.0) {
					successCheck = false;
					message = "Current location is currently unavailable";
				}
				else {
					originLat = currentLat;
					originLon = currentLon;
				}
			}
			else {
				if (debugGeocoding == true) {
					originLat = debugOriginLat;
					originLon = debugOriginLon;
				}
				else {
			 		Log.d(TAG, "1 " + textOrigin + " Singapore");
			 		address = geocoder.getFromLocationName(textOrigin + " Singapore", 1, 
			 				downLat, leftLon, upLat, rightLon);
			 		Log.d(TAG, "2");
			 		if (address.size() > 0) {
			 			originLat = address.get(0).getLatitude();
			 			originLon = address.get(0).getLongitude();
			 		}
			 		else {
			     		successCheck = false;
			 			message = "Check your network connectivity or enter another origin location";
			 		}
				}
			}
	 		
	 		// Geocode the destination
	 		if (successCheck)
	 		{
	 			// If user has entered current location
				if (textDestination.toLowerCase().equals("current location") ||
						textDestination.toLowerCase().equals("here")) 
				{
					// Action taken depends on whether current location is available
					if (currentLat == 0.0 && currentLon == 0.0) {
						successCheck = false;
						message = "Current location is currently unavailable";
					}
					else {
						destinationLat = currentLat;
						destinationLon = currentLon;
					}
				}
				else {
					if (debugGeocoding == true) {
						destinationLat = debugDestinationLat;
			     		destinationLon = debugDestinationLon;
					}
					else {
						Log.d(TAG, "3");
			     		address = geocoder.getFromLocationName(textDestination + " Singapore", 1, 
				 			downLat, leftLon, upLat, rightLon);
			     		Log.d(TAG, "4");
			     		if (address.size() > 0) {
			     			destinationLat = address.get(0).getLatitude();
			     			destinationLon = address.get(0).getLongitude();
			     		}
			     		else {
				     		successCheck = false;
			     			message = "Check your network connectivity or enter another destination location";
			     		}
					}
				}
	 		}
	 		
	 		// Log test geocoding
	 		Log.d(TAG, Double.toString(originLat));
	 		Log.d(TAG, Double.toString(originLon));
	 		Log.d(TAG, Double.toString(destinationLat));
	 		Log.d(TAG, Double.toString(destinationLon));
	 	}
	 	catch (Exception e)
	 	{
	 		Log.d (TAG, "Thread exception");
	 		e.printStackTrace();
	 		successCheck = false;
	 		message = "Error ocurred while interpreting origin and destination entered";
	 	}


 		Log.d (TAG, "Success Check: " + successCheck);
		Message tempMessage = new Message();
		tempMessage.arg1 = 0;
	 	if (successCheck)
	 		tempMessage.arg1 = 1;
 		handlerDialog.sendMessage(tempMessage);
 	}
	
	private Handler handlerDialog = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			Log.d (TAG, "Message: " + msg.arg1 + "x" + message);
			
     		if (dialog != null && dialog.isShowing())
     			dialog.dismiss();
     		
     		if (msg.arg1 == 0) {
     			Log.d(TAG, message);
     			AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
     			builder.setMessage(message).setCancelable(false)
     			       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
     			           public void onClick(DialogInterface dialog, int id) {
     			        	   dialog.cancel();
     			           }
     				       });
     			AlertDialog alert = builder.create();
     			alert.show();
     		}
     		
     		if (msg.arg1 == 1) {
     			Intent intent = new Intent (WelcomeActivity.this, MenuTabActivity.class);
	     		intent.putExtra("originLat", originLat);
	     		intent.putExtra("originLon", originLon);
	     		intent.putExtra("destinationLat", destinationLat);
	     		intent.putExtra("destinationLon", destinationLon);
	     		intent.putExtra("currentLat", currentLat);
	     		intent.putExtra("currentLon", currentLon);
	     		startActivity(intent);
     		}
		}
	};
	
	BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	
	    	try {
	    		
	    		Log.d (TAG, "Entering mLocationReceiver");
	    		
	    		// Receive current location
	    		double tempCoordinate = intent.getDoubleExtra(SensorService.CURR_LATITUDE, 0);
	            if (tempCoordinate!=0.0)
	            	currentLat = tempCoordinate;
	        	tempCoordinate = intent.getDoubleExtra(SensorService.CURR_LONGITUDE, 0);
	            if (tempCoordinate!=0.0)
	            	currentLon = tempCoordinate;
	    		
	            Log.d (TAG, "Exiting mLocationReceiver: " + currentLat + "xx" + currentLon);
	    	}
	    	catch (Exception e) {
	    		Log.d (TAG, "Exception in mLocationReceiver");
	    	}
	    }
	};


	// Register receiver to get current location updates
	public void registermLocationReceiver () {
		
		// Initialize handler and threads which perform registration after 10 milliseconds
        Runnable mRegisterLocationReceiver = new Runnable() {
        	   public void run() {
        		   try
        		   {
        				Log.d (TAG, "Registering mLocationReceiver");
        				IntentFilter movementFilter;
        		        movementFilter = new IntentFilter(SensorService.MOVEMENT_UPDATE);
        		        registerReceiver(mLocationReceiver, movementFilter);
        		   }
        		   catch (Exception e)
        		   {Log.d (TAG, "Exception in registering broadcast receiver");}
        		   
        	   }
        };
        Handler registerHandler = new Handler();
        registerHandler.removeCallbacks(mRegisterLocationReceiver);
        registerHandler.postDelayed(mRegisterLocationReceiver, 10); // 10ms to register

	}

	// Unregister current location updates receiver
	public void unregistermLocationReceiver () {
		try {
			Log.d (TAG, "Unregistering mLocationReceiver");
			unregisterReceiver(mLocationReceiver);
		}
		catch (Exception e) {
			Log.d (TAG, "Could not unregister location receiver");
		}
	}
	
}