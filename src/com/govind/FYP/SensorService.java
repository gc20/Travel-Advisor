package com.govind.FYP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SensorService extends Service implements SensorEventListener, OnSharedPreferenceChangeListener {

	// Debug tag
	private static String TAG = SensorService.class.getSimpleName();

	// Accelerometer initialization variables
	public boolean isAccelRunning = false;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	
	// GPS initialization variables
	public boolean isGPSRunning = false;
	private static LocationManager locationManager;
	private static LocationListener GPSListener;
	private static LocationListener networkListener;
	
	// Thread which sends data to server
	private Handler mHandler;
	private Runnable mSendDataTask;
	private SendDataTask task;
	
	// Latest data
	private static Vector <Double> latitudes;
	private static Vector <Double> longitudes;
	private static Vector <Double> locationAccuracy;
	private static Vector <Long> locationTimestamps;
	private Vector <Long> accelTimestamps;
	private Vector <Double> acceleration;
	private static int locationFrequency = 5000; // Frequency of location sensor readings, in milliseconds
	private static int accelFrequency = 5000; // Frequency of accelerometer sensor readings, in milliseconds
	int tempCount = 1; // Remove

	// Prevent mutexes
	private static Semaphore accelSem; // Prevent the thread which sends data to server from being disturbed by new accelerometer reads
	private static Semaphore locationSem;// Prevent the thread which sends data to server, GPS accuracy listener and network accuracy listener from disturbing each other
	
	// Network-pass filter variables
	private final static double alpha = 0.9; // To be examined (Kalman filter gain?)
	private static double gravity[] = new double[3];
	private static double linearAcceleration[] = new double[3];

	// Device unique identification
	String deviceId;
	
	// URL for data access
	String urlHost = "http://lightning.helloworld.sg/";
	
	// Used to access user preferences
	SharedPreferences prefs;
	
    // Strings to Broadcast current location to activity
	public static String MOVEMENT_UPDATE = ".action.MOVEMENT_UPDATE";
	public static String CURR_LATITUDE = ".CURR_LATITUDE";
    public static String CURR_LONGITUDE = ".CURR_LONGITUDE";
   
    // Check if user wishes to use the active origin option
    boolean activeOrigin = false;
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// On create
	public void onCreate() {

		// Super
		super.onCreate();
		
		// Add package name to movement strings for broadcasting location
		MOVEMENT_UPDATE = this.getPackageName() + "." + 
			SensorService.class.getSimpleName().toUpperCase() + MOVEMENT_UPDATE;
		CURR_LATITUDE = this.getPackageName() + "." + 
			SensorService.class.getSimpleName().toUpperCase() + CURR_LATITUDE;
		CURR_LONGITUDE = this.getPackageName() + "." + 
			SensorService.class.getSimpleName().toUpperCase() + CURR_LONGITUDE;
		
		// Initialize accelerometer sensor
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Log.d(TAG, "On Create: Requested accelerometer updates");
		
		// Initialize location variables
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Log.d(TAG, "On Create: Requested location updates");
		
		// Initialize vectors
		latitudes = new Vector <Double> ();
		longitudes = new Vector <Double> ();
		locationAccuracy = new Vector <Double> ();
		acceleration = new Vector <Double> ();
		locationTimestamps = new Vector <Long> ();
		accelTimestamps = new Vector <Long> ();
		
		// Initialize semaphores
		accelSem = new Semaphore (1);
		locationSem = new Semaphore (1);
		
        // Initialize preferences access variables
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		storePreferences();
	}

	// Service started
	@Override
	public synchronized void onStart(Intent intent, int startId) {
		
		// Super
		super.onStart(intent, startId);

		// Turn on accelerometer if it is off
		if (!this.isAccelRunning) {
			Log.d(TAG, "On start - In accelerometer register listener area");
			mSensorManager.registerListener((SensorEventListener) this,
					mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
			this.isAccelRunning = true;
		}
		
		// Turn on location collection if its is off
		if (!this.isGPSRunning) {
			Log.d(TAG, "On start - In location register listener area");
			locationUpdates();
			//locationManager.requestLocationUpdates(locationManager.get, 0, 0, this); // Hard-coded source
			this.isGPSRunning = true;
		}
		
		// Get Device ID
		final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
	    final String tmDevice, tmSerial, androidId;
	    tmDevice = "" + tm.getDeviceId();
	    tmSerial = "" + tm.getSimSerialNumber();
	    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
	    deviceId = deviceUuid.toString();
		if (deviceId == null)
			deviceId = "1"; // Default ID for non-telephony devices with no ID
		try {deviceId = URLEncoder.encode(deviceId,"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		Log.d(TAG, deviceId);
	    
	    
		// Initialize handler and threads which send data to server
        mSendDataTask = new Runnable() {
        	   public void run() {
        		   
        		   boolean checkDeadlock1 = false;
        		   boolean checkDeadlock2 = false;
        		   boolean checkDeadlock3 = false;
        		   boolean checkDeadlock4 = false;
        		   // Send sensor data to user
        		   try
        		   {
	        		   locationSem.acquire();
	        		   checkDeadlock1 = true;
	        		   accelSem.acquire();
	        		   checkDeadlock2 = true;
	        		   Log.d (TAG + tempCount++, sendSensorData());
	        		   accelSem.release();
	        		   checkDeadlock3 = true;
	        		   locationSem.release();
        		   }
        		   catch (Exception e)
        		   {
        			   if (checkDeadlock2 && !checkDeadlock3)
        				   accelSem.release();
        			   if (checkDeadlock1 && !checkDeadlock4)
        				   locationSem.release();
        		   }
        		   
        		   // Set timer to recall thread after specified frequency
        		   mHandler.postDelayed(mSendDataTask, locationFrequency);
        	   }
        };
        mHandler = new Handler();
        mHandler.removeCallbacks(mSendDataTask);
        mHandler.postDelayed(mSendDataTask, locationFrequency);
		
		Log.d(TAG, "On start complete");
	}

	// When sensors are turned off
	@Override
	public synchronized void onDestroy() {

		try
		{
			super.onDestroy();
			mSensorManager.unregisterListener((SensorEventListener) this);
			locationManager.removeUpdates(networkListener);
			locationManager.removeUpdates(GPSListener);
			mHandler.removeCallbacks(mSendDataTask);
			task.cancel(true); // For Async task. Not implemented, since this is a short task.
			Log.d(TAG, "On destroy completed");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.d (TAG, "Exception in onDestroy");
		}
	
	}

	// When accelerometer change event occurs
	@Override
	public void onSensorChanged(SensorEvent event) {
		
		boolean checkDeadlock1 = false;
		boolean checkDeadlock2 = false;
		
		try {
			
			// Use only accelerometer values
			if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
				return;

			// If minimum change frequency granularity has not passed, continue
			if (accelTimestamps.size() != 0 && (new Date().getTime() - accelFrequency) < accelTimestamps.lastElement())
				return;

			// Acquire accelerometer semaphore
			accelSem.acquire();
			checkDeadlock1 = true;
			
			// Network-pass filter
			gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
			linearAcceleration[0] = event.values[0] - gravity[0];
			linearAcceleration[1] = event.values[1] - gravity[1];
			linearAcceleration[2] = event.values[2] - gravity[2];

			// Log values
			String eveVals = "Event Values " + event.values[0] + " " + event.values[1]
			                                 			          + " " + event.values[2];
			String linVals = "Linear Acceleration Values " + linearAcceleration[0] + " " + 
						linearAcceleration[1] + " " + linearAcceleration[2];
			Log.d (TAG, eveVals);
			Log.d (TAG, linVals);
			
			// Add timestamp to vector
			accelTimestamps.add(new Date().getTime());
			
			// Add accelerometer values to vector
			acceleration.add (linearAcceleration[0]);
			acceleration.add (linearAcceleration[1]);
			acceleration.add (linearAcceleration[2]);
			
			// Release accelerometer semaphore
			accelSem.release();
			checkDeadlock2 = true;
			

		} catch (Exception e) {
			Log.d(TAG, "Exited onSensorChanged with exception");
			if (checkDeadlock1 && !checkDeadlock2)
				accelSem.release();
		}
	}
	
	// This method creates two listeners, each for a different level of accuracy
	public void locationUpdates (){

		  // Part Credit for folnetworking code: http://developerlife.com/tutorials/?p=1375 (20th August, 2011)
		  
		  // Get network accuracy provider
		  LocationProvider network=
		    locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
		  
		  // Get GPS accuracy provider
		  LocationProvider GPS=
			locationManager.getProvider(LocationManager.GPS_PROVIDER);

		  // Using network accuracy provider to listen for updates
		  networkListener = new LocationListener() {
		        
			  	public void onLocationChanged(Location location) {

					Log.d(TAG, "Network: On Location Changed Start");
					long readTime = new Date().getTime();
					
					boolean checkDeadlock1 = false;
					boolean checkDeadlock2 = false;
					
					// Acquire location semaphore
					try {
						locationSem.acquire();
						checkDeadlock1 = true;
					
					// If minimum change frequency granularity has been crossed, proceed
					if (!(locationTimestamps.size() != 0 && (readTime - locationFrequency) < locationTimestamps.lastElement()))
					{
						// Make sure mutex problems caused by multiple location listeners is not a problem. Discard any late entries.
						if (!(locationTimestamps.size() > 0 && readTime < locationTimestamps.lastElement()))
						{
							// Avoid similar entries with very slight time discrepancy
							if (!(locationTimestamps.size() > 0 && latitudes.lastElement() == location.getLatitude()
									&& longitudes.lastElement() == location.getLongitude()))
							{
								// Store latest data
								latitudes.add(location.getLatitude());
								longitudes.add(location.getLongitude());
								locationAccuracy.add((double) location.getAccuracy());
								locationTimestamps.add(readTime);
							}
						}
					}
					
					// Send location to MenuTabActivity
					try {
				        Intent intent = new Intent(MOVEMENT_UPDATE);
				        intent.putExtra(CURR_LATITUDE, location.getLatitude());
				        intent.putExtra(CURR_LONGITUDE, location.getLongitude());
				        sendBroadcast(intent);
					}
					catch (Exception e) {
						Log.d(TAG, "Could not send location to menutabactivity");
					}
					
					// Release location semaphore
					locationSem.release();
					checkDeadlock2 = true;

					} catch (InterruptedException e) {
						e.printStackTrace();
						if (checkDeadlock1 && !checkDeadlock2)
							locationSem.release();
					}
					
					Log.d(TAG, "Network: On Location Changed Complete");
		        }
		        
		        public void onStatusChanged(String s, int status, Bundle bundle) {
		        	Log.d(TAG, "Network: Status changed");
		        	/*if (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE)
		        		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);*/
		        
		        }
		        
		        public void onProviderEnabled(String s) {
		        	// Try switching to a different provider
		    		Log.d(TAG, "On Provider Enabled");
		        }
		        
		        public void onProviderDisabled(String s) {
		           // Try switching to a different provider
		    		Log.d(TAG, "On Provider Disabled");
		        }
		  };
		  locationManager.requestLocationUpdates(network.getName(), 0, 0f, networkListener);
				  
		  
		  // Using GPS accuracy provider to listen for updates
		  GPSListener = new LocationListener() {
		        
			  	public void onLocationChanged(Location location) {

					Log.d(TAG, "GPS: On Location Changed Start");
					long readTime = new Date().getTime();
					
					boolean checkDeadlock1 = false;
					boolean checkDeadlock2 = false;
					
					// Acquire location semaphore
					try {
						locationSem.acquire();
						checkDeadlock1 = true;
					
					// If minimum change frequency granularity has passed, continue
					if (!(locationTimestamps.size() != 0 && (readTime - locationFrequency) < locationTimestamps.lastElement()))
					{
						// Make sure mutex problems caused by multiple location listeners is not a problem. Discard any late entries.
						if (!(locationTimestamps.size() > 0 && readTime < locationTimestamps.lastElement()))
						{
							// Avoid similar entries with very slight time discrepancy
							if (!(locationTimestamps.size() > 0 && latitudes.lastElement() == location.getLatitude()
									&& longitudes.lastElement() == location.getLongitude()))
							{
								// Store latest data
								latitudes.add(location.getLatitude());
								longitudes.add(location.getLongitude());
								locationAccuracy.add((double) location.getAccuracy());
								locationTimestamps.add(readTime);
							}
						}
					}
					
					// Send location to MenuTabActivity
					try {
				        Intent intent = new Intent(MOVEMENT_UPDATE);
				        intent.putExtra(CURR_LATITUDE, location.getLatitude());
				        intent.putExtra(CURR_LONGITUDE, location.getLongitude());
				        sendBroadcast(intent);
					}
					catch (Exception e) {
						Log.d(TAG, "Could not send location to menutabactivity");
					}
					
					
					// Release location semaphore
					locationSem.release();
					checkDeadlock2 = true;

					} catch (InterruptedException e) {
						e.printStackTrace();
						if (checkDeadlock1 && !checkDeadlock2)
							locationSem.release();
					}
					
					Log.d(TAG, "GPS: On Location Changed Complete");
		        }
		        
			  	public void onStatusChanged(String s, int status, Bundle bundle) {
		        	/*if (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE)
		        		locationManager.requestLocationUpdates(LocationManager, 0, 0, this);*/

		        }
		       
			  	public void onProviderEnabled(String s) {
		        	// Try switching to a different provider
		    		Log.d(TAG, "On Provider Enabled");
		        }
		        
			  	public void onProviderDisabled(String s) {
		        	// Try switching to a different provider
		        	Log.d(TAG, "On Provider Disabled");
		        }
		      };
		   locationManager.requestLocationUpdates(GPS.getName(), 0, 0f, GPSListener);
		    		  
		      
	}
	
	
	// Called when preferences change
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d (TAG, "Preferences changed");
		storePreferences();
	}
	
	// Obtain preferences from preferences menu
	private void storePreferences () {

		try {
			// Get sensor frequencies
			String tempFrequency = prefs.getString("accelFrequency", "5 Seconds");
			accelFrequency = Integer.parseInt(tempFrequency.substring(0, tempFrequency.indexOf(' ')))
					* 1000;
			if (!tempFrequency.substring(tempFrequency.indexOf(' ') + 1).equals("Seconds"))
				accelFrequency *= 60;
			
			tempFrequency = prefs.getString("locationFrequency", "5 Seconds");
			locationFrequency = Integer.parseInt(tempFrequency.substring(0, tempFrequency.indexOf(' ')))
					* 1000;
			if (!tempFrequency.substring(tempFrequency.indexOf(' ') + 1).equals("Seconds"))
				locationFrequency *= 60;
		}
		catch (Exception e) {
			Log.d (TAG, "Exception in store preferences");
		}
	}
	
	/*// This criteria will settle for less accuracy, GPS power, and monetary cost
	public static Criteria createCoarseCriteria() {

	  Criteria c = new Criteria();
	  c.setAccuracy(Criteria.ACCURACY_COARSE);
	  c.setAltitudeRequired(false);
	  c.setBearingRequired(false);
	  c.setSpeedRequired(false);
	  c.setCostAllowed(true);
	  c.setPowerRequirement(Criteria.POWER_LOW);
	  return c;

	}

	// This criteria needs GPS accuracy, GPS power, and monetary cost
	public static Criteria createFineCriteria() {

	  Criteria c = new Criteria();
	  c.setAccuracy(Criteria.ACCURACY_FINE);
	  c.setAltitudeRequired(false);
	  c.setBearingRequired(false);
	  c.setSpeedRequired(false);
	  c.setCostAllowed(true);
	  c.setPowerRequirement(Criteria.POWER_HIGH);
	  return c;

	}*/
	
	// HTTP Request
	private String sendSensorData () {
    	String str = "***";

        // Variables
		int i = 1, len;
		String url, urlExtra;
		
		// Construct URLs for all sensor values
		len = latitudes.size();
		Log.d(TAG, "Send sensor data");
		
		// Array which will store requisite urls
		Log.d (TAG, "Size = " + len);
		String urls [];
		if (len > 1)
			urls = new String [len-1];
		else
			return str;
		
		for (i=1; i<len; i++)
		{
			// Create URL
			url = urlHost + "inputdata.php?" + 
					"lat1=" + latitudes.elementAt(i-1) + 
					"&lon1=" + longitudes.elementAt(i-1) +
					"&lat2=" + latitudes.elementAt(i) + 
					"&lon2=" + longitudes.elementAt(i) + 
					"&acc=" + ((locationAccuracy.get(i-1) + locationAccuracy.get(i))/2) +
					"&stime=" +  locationTimestamps.elementAt(i-1) + 
					"&etime=" +  locationTimestamps.elementAt(i) + 
					"&uid=" + deviceId;
			Log.d(TAG, url);
			urls [i-1] = url;
			
		}
		
		len = acceleration.size();
		while (len%3 != 2)
			len--;
		for (i=0; i<len; i+=3) {
			// Create accelerometer based JSON
			acceleration.elementAt(i);
			acceleration.elementAt(i+1);
			acceleration.elementAt(i+2);
		}

		// Clear vectors since they have been read and turned into URL form
		latitudes.removeAllElements();
		longitudes.removeAllElements();
		locationAccuracy.removeAllElements();
		locationTimestamps.removeAllElements();
		acceleration.removeAllElements();
		accelTimestamps.removeAllElements();
		
		// Send data via async task
		task = new SendDataTask();
		task.execute(urls);  
    	
    	return str;
    }

	
	// Unimplemented methods
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		//Log.d(TAG, "On Accuracy Changed");
	}

	// Async task which sends data in the background
	private class SendDataTask extends AsyncTask<String, Void, String> {
		
		@Override
		protected String doInBackground(String... urls) {
			
			// Variables used to send data
    		HttpClient hc = new DefaultHttpClient();
    		HttpPost post;
    		HttpResponse resp;
    		String response = "";
			
			try
			{
				for (String url : urls) {
	
		    		// Send raw data
	        		Log.d (TAG, "SendDataTaskAsync Sending: " + url);
	        		post = new HttpPost(url);
	        		resp = hc.execute(post);
	        		BufferedReader reader =  new BufferedReader(new InputStreamReader(resp.getEntity().getContent(),"iso-8859-1"),8);
	        		while ((response = reader.readLine()) != null) {
		        		Log.d (TAG, "SendDataTaskAsync Sent: " + response);
	    			}
	        		//Toast.makeText(getApplicationContext(), "Sent: " + url, Toast.LENGTH_SHORT).show();
	    		}
			}
			catch (Exception e)
			{ Log.d (TAG, "SendDataTaskAsync: Exception");}
			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			Log.d (TAG, "Result received " + result);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}
		
		
	}

}
