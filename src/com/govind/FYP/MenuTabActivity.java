package com.govind.FYP;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;

public class MenuTabActivity extends TabActivity implements OnSharedPreferenceChangeListener{

	// Debug Tag
	private static String TAG = MenuTabActivity.class.getSimpleName();
	
	// Stores road data details
	public ArrayList <RoadData> roadDetails = new ArrayList <RoadData> ();
	// Stores road data that forms the origin of each road segment
	public ArrayList <RoadData> tempRoadDetails = new ArrayList <RoadData> ();
	// Stores aggregated data for each road
	public ArrayList <RoadDataAggregate> roadDataAggregate = new ArrayList <RoadDataAggregate> ();
	// Stores list of alerts
	public ArrayList <RoadAlerts> roadAlerts = new ArrayList <RoadAlerts> ();
	// Stores overall route information
	public String roadOverall[] = new String [5]; // 1-> Speed, 2-> Time, 3-> Distance, 4-> Congestion, 5-> Confidence
	
	// String responses for HTTP data requests
	public String httpResponses = "";
	public boolean initialAlertDialog = false;
	public boolean isReloadNecessary = false;
	
	// Display options for initial lack of data
	ProgressDialog dialog = null;
	ProgressDialog refreshDialog = null;
	AlertDialog alert = null;
	
	// Store origin and destination requests made by user
	public double originLat = 0.0;
	public double originLon = 0.0;
	public double destinationLat = 0.0;
	public double destinationLon = 0.0;
	
	// Latest update of data and alerts objects
	public long lastUpdateData = 0;
	public long lastUpdateAlerts = 0;
	
	// Frequency of update (number of milliseconds)
	public long refreshFrequency = 1000;
	
	// Stores user's preference for updating by active origin
	boolean activeOrigin = false;
	
	// ID of road being updated
	public int SequenceID = 1;
	
	// Threads for handling server updates
	private Handler mHandler;
	private Runnable mReceiveDataTask;
	private ObtainDataTask obtainTask;
	
	// URL for data access
	String urlHost = "http://lightning.helloworld.sg/";
    
	// Stores user's current location. 0.0 on both coordinates is considered to imply unavailability of location
	double currentLat = 0.0;
	double currentLon = 0.0;
	
	// Used to access user preferences
	SharedPreferences prefs;
	
	// Debug parameters
	boolean debugServer = false; // false -> normal, true -> force data
	String debugResponse = "JSONROUTE[{\"SequenceID\":1,\"InternalID\":1,\"RoadName\":\"Scotts Road\",\"Congestion\":1.6,\"Distance\":798.4402488389533,\"SpeedLimit\":60,\"ExpectedTime\":59.88301866292149,\"Confidence\":0,\"Latitude\":1.312384843826294,\"Longitude\":103.83800506591797},{\"SequenceID\":2,\"InternalID\":1,\"RoadName\":\"Newton Circus\",\"Congestion\":1.5999999999999999,\"Distance\":11.666880950662385,\"SpeedLimit\":50,\"ExpectedTime\":1.0500192855596147,\"Confidence\":0,\"Latitude\":1.312876,\"Longitude\":103.838745},{\"SequenceID\":2,\"InternalID\":2,\"RoadName\":\"Newton Circus\",\"Congestion\":1.6,\"Distance\":18.846974235103094,\"SpeedLimit\":50,\"ExpectedTime\":1.6962276811592782,\"Confidence\":0,\"Latitude\":1.312979,\"Longitude\":103.838765},{\"SequenceID\":2,\"InternalID\":3,\"RoadName\":\"Newton Circus\",\"Congestion\":1.5999999999999999,\"Distance\":17.690191760229474,\"SpeedLimit\":50,\"ExpectedTime\":1.5921172584206529,\"Confidence\":0,\"Latitude\":1.31314,\"Longitude\":103.838818},{\"SequenceID\":2,\"InternalID\":4,\"RoadName\":\"Newton Circus\",\"Congestion\":1.5999999999999999,\"Distance\":28.259155526929128,\"SpeedLimit\":50,\"ExpectedTime\":2.5433239974236215,\"Confidence\":0,\"Latitude\":1.313251,\"Longitude\":103.838932},{\"SequenceID\":2,\"InternalID\":5,\"RoadName\":\"Newton Circus\",\"Congestion\":1.6,\"Distance\":22.166702209078032,\"SpeedLimit\":50,\"ExpectedTime\":1.9950031988170227,\"Confidence\":0,\"Latitude\":1.313385,\"Longitude\":103.839148},{\"SequenceID\":2,\"InternalID\":6,\"RoadName\":\"Newton Circus\",\"Congestion\":1.6,\"Distance\":15.794762341545567,\"SpeedLimit\":50,\"ExpectedTime\":1.421528610739101,\"Confidence\":0,\"Latitude\":1.313438057899475,\"Longitude\":103.83934020996094},{\"SequenceID\":2,\"InternalID\":7,\"RoadName\":\"Newton Circus\",\"Congestion\":1.6,\"Distance\":12.86033566960022,\"SpeedLimit\":50,\"ExpectedTime\":1.1574302102640197,\"Confidence\":0,\"Latitude\":1.313383,\"Longitude\":103.839469},{\"SequenceID\":2,\"InternalID\":8,\"RoadName\":\"Newton Circus\",\"Congestion\":1.5999999999999999,\"Distance\":13.235426693717494,\"SpeedLimit\":50,\"ExpectedTime\":1.1911884024345745,\"Confidence\":0,\"Latitude\":1.313317,\"Longitude\":103.839564},{\"SequenceID\":3,\"InternalID\":1,\"RoadName\":\"Keng Lee Road\",\"Congestion\":1.6,\"Distance\":580.2480236496228,\"SpeedLimit\":60,\"ExpectedTime\":43.518601773721706,\"Confidence\":0,\"Latitude\":1.313216,\"Longitude\":103.839627},{\"SequenceID\":3,\"InternalID\":2,\"RoadName\":\"Keng Lee Road\",\"Congestion\":1.6,\"Distance\":98.29209123290725,\"SpeedLimit\":60,\"ExpectedTime\":7.371906842468043,\"Confidence\":0,\"Latitude\":1.31385,\"Longitude\":103.844808},{\"SequenceID\":4,\"InternalID\":1,\"RoadName\":\"Norfolk Road\",\"Congestion\":1.5999999999999999,\"Distance\":117.38290695081595,\"SpeedLimit\":50,\"ExpectedTime\":10.564461625573436,\"Confidence\":0,\"Latitude\":1.313936,\"Longitude\":103.845688},{\"SequenceID\":4,\"InternalID\":2,\"RoadName\":\"Norfolk Road\",\"Congestion\":1.5999999999999999,\"Distance\":96.28481580002375,\"SpeedLimit\":50,\"ExpectedTime\":8.665633422002138,\"Confidence\":0,\"Latitude\":1.314869,\"Longitude\":103.846182},{\"SequenceID\":4,\"InternalID\":3,\"RoadName\":\"Norfolk Road\",\"Congestion\":1.5999999999999999,\"Distance\":56.503215146734,\"SpeedLimit\":50,\"ExpectedTime\":5.08528936320606,\"Confidence\":0,\"Latitude\":1.315513,\"Longitude\":103.846761},{\"SequenceID\":4,\"InternalID\":4,\"RoadName\":\"Norfolk Road\",\"Congestion\":1.5999999999999999,\"Distance\":98.17557420167766,\"SpeedLimit\":50,\"ExpectedTime\":8.83580167815099,\"Confidence\":0,\"Latitude\":1.315888,\"Longitude\":103.847104},{\"SequenceID\":4,\"InternalID\":5,\"RoadName\":\"Norfolk Road\",\"Congestion\":1.6,\"Distance\":90.23989558825993,\"SpeedLimit\":50,\"ExpectedTime\":8.121590602943392,\"Confidence\":0,\"Latitude\":1.316467046737671,\"Longitude\":103.84777069091797},{\"SequenceID\":4,\"InternalID\":6,\"RoadName\":\"Norfolk Road\",\"Congestion\":1.6,\"Distance\":104.31471307620146,\"SpeedLimit\":50,\"ExpectedTime\":9.38832417685813,\"Confidence\":0,\"Latitude\":1.316864,\"Longitude\":103.848478},{\"SequenceID\":4,\"InternalID\":7,\"RoadName\":\"Norfolk Road\",\"Congestion\":1.6,\"Distance\":29.81746348924918,\"SpeedLimit\":50,\"ExpectedTime\":2.6835717140324262,\"Confidence\":0,\"Latitude\":1.317218,\"Longitude\":103.849347},{\"SequenceID\":5,\"InternalID\":1,\"RoadName\":\"Owen Road\",\"Congestion\":1.6,\"Distance\":89.95754407777355,\"SpeedLimit\":50,\"ExpectedTime\":8.096178966999618,\"Confidence\":0,\"Latitude\":1.317207,\"Longitude\":103.849615},{\"SequenceID\":5,\"InternalID\":2,\"RoadName\":\"Owen Road\",\"Congestion\":1.6,\"Distance\":171.0462881105897,\"SpeedLimit\":50,\"ExpectedTime\":15.394165929953072,\"Confidence\":0,\"Latitude\":1.316703,\"Longitude\":103.850248},{\"SequenceID\":5,\"InternalID\":3,\"RoadName\":\"Owen Road\",\"Congestion\":1.6,\"Distance\":130.1507963732636,\"SpeedLimit\":50,\"ExpectedTime\":11.713571673593723,\"Confidence\":0,\"Latitude\":1.315588,\"Longitude\":103.851308},{\"SequenceID\":5,\"InternalID\":4,\"RoadName\":\"Owen Road\",\"Congestion\":1.6,\"Distance\":168.4591274599654,\"SpeedLimit\":50,\"ExpectedTime\":15.161321471396885,\"Confidence\":0,\"Latitude\":1.31474,\"Longitude\":103.852115},{\"SequenceID\":5,\"InternalID\":5,\"RoadName\":\"Owen Road\",\"Congestion\":1.5999999999999999,\"Distance\":84.5850701024716,\"SpeedLimit\":50,\"ExpectedTime\":7.612656309222444,\"Confidence\":0,\"Latitude\":1.313607,\"Longitude\":103.853121},{\"SequenceID\":6,\"InternalID\":1,\"RoadName\":\"Race Course Road\",\"Congestion\":1.6,\"Distance\":103.24536899483017,\"SpeedLimit\":50,\"ExpectedTime\":9.292083209534715,\"Confidence\":0,\"Latitude\":1.313038,\"Longitude\":103.853626},{\"SequenceID\":7,\"InternalID\":1,\"RoadName\":\"Rangoon Road\",\"Congestion\":1.5999999999999999,\"Distance\":90.36428552019872,\"SpeedLimit\":50,\"ExpectedTime\":8.132785696817885,\"Confidence\":0,\"Latitude\":1.313633,\"Longitude\":103.854339},{\"SequenceID\":7,\"InternalID\":2,\"RoadName\":\"Rangoon Road\",\"Congestion\":1.6,\"Distance\":23.175473854436742,\"SpeedLimit\":50,\"ExpectedTime\":2.0857926468993067,\"Confidence\":0,\"Latitude\":1.3129,\"Longitude\":103.85469},{\"SequenceID\":7,\"InternalID\":3,\"RoadName\":\"Rangoon Road\",\"Congestion\":1.5999999999999999,\"Distance\":4.693562134558059,\"SpeedLimit\":50,\"ExpectedTime\":0.4224205921102253,\"Confidence\":0,\"Latitude\":1.312712,\"Longitude\":103.85478},{\"SequenceID\":7,\"InternalID\":4,\"RoadName\":\"Rangoon Road\",\"Congestion\":1.5999999999999999,\"Distance\":116.74511378563143,\"SpeedLimit\":50,\"ExpectedTime\":10.50706024070683,\"Confidence\":0,\"Latitude\":1.3126779794692993,\"Longitude\":103.85480499267578},{\"SequenceID\":8,\"InternalID\":1,\"RoadName\":\"Serangoon Road\",\"Congestion\":1.5999999999999996,\"Distance\":224.1342170666669,\"SpeedLimit\":60,\"ExpectedTime\":16.81006628000002,\"Confidence\":0,\"Latitude\":1.311835,\"Longitude\":103.855432},{\"SequenceID\":8,\"InternalID\":2,\"RoadName\":\"Serangoon Road\",\"Congestion\":1.5999999999999999,\"Distance\":168.28539657558002,\"SpeedLimit\":60,\"ExpectedTime\":12.621404743168501,\"Confidence\":0,\"Latitude\":1.313411,\"Longitude\":103.856689}] JSONALERTS[{\"Time\":1320044880000,\"Message\":\"Flooding on Woodlands Road 44m from Mandai Road/Woodlands Road Junc towards Woodlands Road Junc.\",\"Latitude\":1.4115970628371208,\"Longitude\":103.75584510061404,\"Type\":\"Obstacle\"},{\"Time\":1320044760000,\"Message\":\"Flooding on Woodlands Road 153m from Woodlands Road Junc towards Mandai Road/Woodlands Road Junc.\",\"Latitude\":1.41171180062602,\"Longitude\":103.75588203766053,\"Type\":\"Obstacle\"},{\"Time\":1320044700000,\"Message\":\"Roadworks on CTE towards SLE at Ang Mo Kio Ave 5 Exit. Avoid lane 2.\",\"Latitude\":1.373131154703404,\"Longitude\":103.85939996883336,\"Type\":\"Road Work\"},{\"Time\":1320042900000,\"Message\":\"Vehicle breakdown on PIE towards Changi Airport after Adam Rd.\",\"Latitude\":1.3304950014262007,\"Longitude\":103.82010747088968,\"Type\":\"Vehicle Breakdown\"},{\"Time\":1320042240000,\"Message\":\"Accident on PIE towards Jurong near Anak Bukit Flyover. Congestion till Eng Neo over.\",\"Latitude\":1.3394056971149255,\"Longitude\":103.77826197213092,\"Type\":\"Accident\"},{\"Time\":1320035640000,\"Message\":\"Roadworks on ECP towards Changi Airport before Fort Rd Exit. Avoid lane 4.\",\"Latitude\":1.2954033042252564,\"Longitude\":103.87348688899482,\"Type\":\"Road Work\"},{\"Time\":1320033660000,\"Message\":\"Roadworks on Dunearn Road 218m from Dunearn Road Junc towards Dunearn Road/Rifle Range Road Junc.\",\"Latitude\":1.337012050388976,\"Longitude\":103.78100406476496,\"Type\":\"Road Work\"},{\"Time\":1320032340000,\"Message\":\"Vehicle breakdown on BKE towards PIE before Dairy Farm Rd Exit.\",\"Latitude\":1.3687228256267283,\"Longitude\":103.77902842831055,\"Type\":\"Vehicle Breakdown\"},{\"Time\":1320029280000,\"Message\":\"Roadworks on ECP towards City near Marina Sth Exit. Avoid lane 5.\",\"Latitude\":1.276927811633357,\"Longitude\":103.8558255810632,\"Type\":\"Road Work\"},{\"Time\":1320029220000,\"Message\":\"Roadworks on Commonwealth Avenue West at Clementi Avenue 6/Commonwealth Avenue West Junc.\",\"Latitude\":1.3186994385290318,\"Longitude\":103.7632558182174,\"Type\":\"Road Work\"},{\"Time\":1320027720000,\"Message\":\"Roadworks on CTE towards SLE at Ang Mo Kio Ave 1 Entrance. Avoid lane 1.\",\"Latitude\":1.3567652651406468,\"Longitude\":103.8566616121326,\"Type\":\"Road Work\"},{\"Time\":1320026340000,\"Message\":\"Roadworks on Bukit Timah Road 270m from Balmoral Road/Bukit Timah Road Junc towards Bukit Timah Road/Keng Chin Road Junc.\",\"Latitude\":1.318059960048124,\"Longitude\":103.83279427435212,\"Type\":\"Road Work\"},{\"Time\":1320025680000,\"Message\":\"Roadworks on TPE towards SLE after Jln Kayu Exit. Avoid lane 1.\",\"Latitude\":1.4010666363720043,\"Longitude\":103.87059832242976,\"Type\":\"Road Work\"},{\"Time\":1320024960000,\"Message\":\"Roadworks on Kampong Bahru Road 230m from Kampong Bahru Road Junc towards Kampong Bahru Road/Telok Blangah Road Junc.\",\"Latitude\":1.2683410345526982,\"Longitude\":103.82552820015798,\"Type\":\"Road Work\"},{\"Time\":1320010740000,\"Message\":\"Accident on AYE towards ECP after Portsdown Rd Exit. Avoid lanes 3 and 4.\",\"Latitude\":1.287292096743374,\"Longitude\":103.79572050079436,\"Type\":\"Accident\"},{\"Time\":1319989500000,\"Message\":\"Roadworks on Upper Bukit Timah Road 374m from Jalan Asas/Upper Bukit Timah Road Junc towards Hillview Road/Upper Bukit Timah Road Junc.\",\"Latitude\":1.361747340496368,\"Longitude\":103.7671724533776,\"Type\":\"Road Work\"},{\"Time\":1319988960000,\"Message\":\"Roadworks on PIE towards Changi Airport near Kallang River. Avoid lane 3.\",\"Latitude\":1.3291945578541728,\"Longitude\":103.86551301042527,\"Type\":\"Road Work\"},{\"Time\":1319987700000,\"Message\":\"Roadworks on Telok Blangah Road 164m from Telok Blangah Road Junc towards Telok Blangah Road/West Coast Highway Junc.\",\"Latitude\":1.2686865284059374,\"Longitude\":103.81479636897704,\"Type\":\"Road Work\"},{\"Time\":1319987580000,\"Message\":\"Roadworks on Dunearn Road 60m from Dunearn Road/Dunkirk Avenue Junc towards Dunearn Road Junc.\",\"Latitude\":1.32198604579507,\"Longitude\":103.82253099391508,\"Type\":\"Road Work\"},{\"Time\":1319987340000,\"Message\":\"Roadworks on ECP towards City at PIE Exit.\",\"Latitude\":1.3395277051656378,\"Longitude\":103.98111413221828,\"Type\":\"Road Work\"},{\"Time\":1319987100000,\"Message\":\"Accident on PIE towards Jurong near Eng Neo Flyover with congestion till Adam Flyover. Avoid lane 4.\",\"Latitude\":1.3384146978831208,\"Longitude\":103.80721625563852,\"Type\":\"Accident\"},{\"Time\":1319986980000,\"Message\":\"Roadworks on KPE towards TPE between ECP Entrance and Jalan Teban Entrance. Avoid lane 3.\",\"Latitude\":1.379928536259014,\"Longitude\":103.91550383042022,\"Type\":\"Road Work\"},{\"Time\":1319986860000,\"Message\":\"Roadworks on KPE towards TPE after ECP Entrance. Avoid lane 3.\",\"Latitude\":1.3004201733231326,\"Longitude\":103.8779493128898,\"Type\":\"Road Work\"},{\"Time\":1319986020000,\"Message\":\"Roadworks on Telok Blangah Road 117m from Kampong Bahru Road/Keppel Road/Telok Blangah Road Junc towards Telok Blangah Road Junc.\",\"Latitude\":1.2662170345708832,\"Longitude\":103.82578437833648,\"Type\":\"Road Work\"},{\"Time\":1319985420000,\"Message\":\"Roadworks on Telok Blangah Road 167m from Keppel Bay View/Telok Blangah Road Junc towards Morse Road/Telok Blangah Road Junc.\",\"Latitude\":1.2691294885115196,\"Longitude\":103.81386271692752,\"Type\":\"Road Work\"},{\"Time\":1319984880000,\"Message\":\"Roadworks on PIE towards Changi Airport before Bukit Batok East Ave 3. Avoid lanes 1 and 2.\",\"Latitude\":1.3383557680128984,\"Longitude\":103.75459362344404,\"Type\":\"Road Work\"},{\"Time\":1319968440000,\"Message\":\"Vehicle breakdown on PIE towards Changi Airport before TPE.\",\"Latitude\":1.3505833758896884,\"Longitude\":103.9600009763183,\"Type\":\"Vehicle Breakdown\"},{\"Time\":1319967720000,\"Message\":\"Vehicle breakdown on BKE towards PIE after KJE Exit.\",\"Latitude\":1.3854566455395874,\"Longitude\":103.77508915163864,\"Type\":\"Vehicle Breakdown\"},{\"Time\":1319967300000,\"Message\":\"Vehicle breakdown on AYE towards ECP after Portsdown Rd Exit.\",\"Latitude\":1.2874974723984467,\"Longitude\":103.79534732496356,\"Type\":\"Vehicle Breakdown\"},{\"Time\":1319961360000,\"Message\":\"Roadworks on Telok Blangah Road 295m from Telok Blangah Road/Telok Blangah Street 31 Junc towards Telok Blangah Road Junc.\",\"Latitude\":1.271286609536942,\"Longitude\":103.80795060167762,\"Type\":\"Road Work\"}]";
	
    public void onCreate(Bundle savedInstanceState) {
		
		// General initialization
		Log.d(TAG, "On create");
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.menu);

	    // Create alert message for the situation in which network connectivity is absent
		AlertDialog.Builder builder = new AlertDialog.Builder(MenuTabActivity.this);
		builder.setMessage("Cannot connect to server presently. Please try later")
		.setCancelable(false).setPositiveButton("OK", 
				new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		alert = builder.create();
		
	    
	    // Start thread to get latest data via async task
		obtainTask = new ObtainDataTask();
		obtainTask.execute(new String [] {}); // Empty string array
		
	    // Initialize tab variables
	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Receive request values from welcome activity
	    Bundle extras = getIntent().getExtras();
	    if (extras != null)
	    {
	    	try
	    	{
		    	originLat = extras.getDouble("originLat");
		    	originLon = extras.getDouble("originLon");
		    	destinationLat = extras.getDouble("destinationLat");
		    	destinationLon = extras.getDouble("destinationLon");
		    	currentLat = extras.getDouble("currentLat");
		    	currentLon = extras.getDouble("currentLon");
	     	} 
	     	catch (Exception e) 
	     	{
				e.printStackTrace();
				Log.d(TAG, "Coordinates extraction error.");
				startActivity (new Intent (this, WelcomeActivity.class));
			}
	    }
	    
	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, DashboardActivity.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("Dashboard").setIndicator("Dashboard",
	                      res.getDrawable(R.drawable.ic_tab_dashboard)).setContent(intent);
	    tabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, DetailsActivity.class);
	    spec = tabHost.newTabSpec("Details").setIndicator("Details",
	                      res.getDrawable(R.drawable.ic_tab_details)).setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, AlertsActivity.class);
	    spec = tabHost.newTabSpec("Alerts").setIndicator("Alerts",
	                      res.getDrawable(R.drawable.ic_tab_alerts)).setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, RouteMapActivity.class);
	    spec = tabHost.newTabSpec("Route Map").setIndicator("Route Map",
	                      res.getDrawable(R.drawable.ic_tab_map)).setContent(intent);
	    tabHost.addTab(spec);
	    
	    // Display progress message when no data is available
		dialog = ProgressDialog.show(MenuTabActivity.this, "", 
                    "Loading. Please wait...", false);
		
		// Initialize handler and threads which request and receive data from server
        mReceiveDataTask = new Runnable() {
        	   public void run() {
        		   
        		   // Receive data from server
        		   try
        		   {
        			   // Get latest data and update relevant UI
        			   manageArrayListData(false);
        			   Log.d (TAG, "listdata");
        			   updateChildUI ();
        			   Log.d (TAG, "child UI");
            		   // Set timer to recall thread after specified frequency
            		   mHandler.postDelayed(mReceiveDataTask, refreshFrequency);
        		   }
        		   catch (Exception e)
        		   {Log.d(TAG, "Exception in mReceiveDataTask");}
        		   
        	   }
        };
        mHandler = new Handler();
        mHandler.removeCallbacks(mReceiveDataTask);
        mHandler.postDelayed(mReceiveDataTask, refreshFrequency);
		
		// Register receiver to get current location updates
		registermLocationReceiver();
        
        // Initialize preferences access variables
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		storePreferences();

		// Begin with the first tab
	    tabHost.setCurrentTab(1);
		
	}

	// When "menu" is selected
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.tabmenu, menu);
		return true;
	}
	

	// When menu options are selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	
		switch (item.getItemId()) {
		
		case R.id.refresh:
			refreshData ();
			break;
		
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
					Intent myIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS );
				    startActivity(myIntent);
				    serviceMessage = "Sensor service will have started when you return";
				    registermLocationReceiver();
				}
				startService(new Intent(this, SensorService.class));
				Toast.makeText(this, serviceMessage, Toast.LENGTH_SHORT).show();

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
	protected void onDestroy() {
		try
		{
			if (obtainTask.getStatus() == AsyncTask.Status.PENDING || obtainTask.getStatus() == AsyncTask.Status.RUNNING)
				obtainTask.cancel(true); // Close async task. Not implemented, since this task is very short
			unregistermLocationReceiver();
			super.onDestroy();
		}
		catch (Exception e)
		{ Log.d (TAG, "Exception in on Destroy.");}
	}
	
	

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event) {
		
		Log.d (TAG, "On back pressed");
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			
			// Dismiss loading progress bar if its still present
			closeDialog();
			
			// Dismiss refresh progress bar if its still present
			//if (refreshDialog!=null && refreshDialog.isShowing())
				//refreshDialog.dismiss();
			
			String childActivity = getCurrentActivity().toString();;
			if (childActivity.length() >= 32 && childActivity.substring(15, 32).equals("DashboardActivity"))
				((DashboardActivity) getCurrentActivity()).finish();
			if (childActivity.length() >= 30 && childActivity.substring(15, 30).equals("DetailsActivity"))
				((DetailsActivity) getCurrentActivity()).finish();
			if (childActivity.length() >= 29 && childActivity.substring(15, 29).equals("AlertsActivity"))
				((AlertsActivity) getCurrentActivity()).finish();
			if (childActivity.length() >= 31 && childActivity.substring(15, 31).equals("RouteMapActivity"))
				((RouteMapActivity) getCurrentActivity()).finish();
			
			// Call welcome activity. Send current latitude and longitude.
	 		Intent intent = new Intent (this, WelcomeActivity.class);
	 		intent.putExtra("currentLat", currentLat);
	 		intent.putExtra("currentLon", currentLon);
	 		startActivity(intent);
		}
	 	return super.onKeyDown(keyCode, event);
	}

	public void refreshData() {

		// Display progress message when no data is available
		closeDialog();
		if (refreshDialog == null || !refreshDialog.isShowing())
			refreshDialog = ProgressDialog.show(MenuTabActivity.this, "", 
                    "Refreshing. Please wait...", false);
		
		// Thread to get latest data
		Thread refreshThread = new Thread() {
			public void run() 
			{
					try {
						while (!getLatestArrayListDataFromServer(roadDetails)) {
							//if (refreshDialog == null || refreshDialog.isShowing())
								
						}
  					    manageArrayListData(true);
	        			updateChildUI ();
					} catch (Exception e) {
						e.printStackTrace();
					}
			}
			};
		refreshThread.start();
        
	}

	private Handler refreshHandlerDialog = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			if (msg.arg1 == 1) {
				Log.d (TAG, "In refreshHandlerDialog");
				if (refreshDialog!=null && refreshDialog.isShowing())
					refreshDialog.dismiss();
				updateChildUI();
			}
     	}
	};
	
	// Synchronized method to prevent race conditions
	public synchronized void manageArrayListData (boolean force) {

		Log.d(TAG, "Manage Array List Data: " + force);
		
		// If object has not been populated, or if data is too old. Or if an explicit override command is issued
		long currTime = new Date().getTime();
		
		if ((currTime - lastUpdateAlerts) > refreshFrequency || force)
		{
			lastUpdateAlerts = currTime;
		
			// 0-> Not confident, 1-> Confident, 2-> Very confident
			// 0-> Congested, 1-> Slightly Congested, 2-> Moving Freely
			getLatestArrayListDataFromServer(roadDetails);
	
			// Create temp list of datapoints with internal ID of 1
			int len = roadDetails.size();
			tempRoadDetails.clear();
			for (int i = 0; i < len; i++) {
				if (roadDetails.get(i).getInternalID() == 1)
					tempRoadDetails.add(roadDetails.get(i));
			}
	
			String roadName = "";
			// These variables are used to calculate output for each segment
			double congestion = 0.0;
			double speedLimit = 0.0;
			double totalDistance = 0.0;
			double totalTime = 0.0;
			double confidence = 0.0;
	
			// These variables are used to calculate output for entire route
			double ocongestion = 0.0;
			double ototalDistance = 0.0;
			double ototalTime = 0.0;
			double oconfidence = 0.0;
	
			// Used to format string outputs
			DecimalFormat decFormat = new DecimalFormat("0.00");
			DecimalFormat intFormat = new DecimalFormat("00");
	
			// Temporarily clear aggregate list
			Log.d (TAG, "Clearing roaddataaggregate");
			roadDataAggregate.clear();
			
			for (int i = 0; i < len; i++) {
				roadName = roadDetails.get(i).getRoadName();
				speedLimit = roadDetails.get(i).getSpeedLimit();
				totalDistance += roadDetails.get(i).getDistance();
				totalTime += roadDetails.get(i).getExpectedTime();
				confidence += roadDetails.get(i).getConfidence()
						* roadDetails.get(i).getDistance();
				congestion += roadDetails.get(i).getCongestion()
						* roadDetails.get(i).getDistance();
	
				if ((i == (len - 1)) || roadDetails.get(i + 1).getInternalID() == 1) {
					// Get average speed
					String averageSpeedString = "";
					if (totalTime > 0)
						averageSpeedString = decFormat.format(totalDistance
								/ totalTime * 3.6)
								+ " kmph";
	
					// Get speed limit
					String speedLimitString = "";
					if (speedLimit > 0.0)
						speedLimitString = intFormat.format(speedLimit) + " kmph";
	
					// Get estimated time
					String estimatedTimeString = "";
					totalTime = Math.ceil(totalTime);
					if (totalTime > 0) {
						if (totalTime >= 3600)
							estimatedTimeString += intFormat
									.format(totalTime / 3600) + ":";
						else
							estimatedTimeString += "00:";
						if (totalTime >= 60)
							estimatedTimeString += intFormat
									.format((totalTime % 3600) / 60) + ":";
						else
							estimatedTimeString += "00:";
						estimatedTimeString += intFormat.format(totalTime % 60);
						if (estimatedTimeString.equals("00:00:60"))
							Log.d (TAG, "Time: " + totalTime);
					}
	
					// Get congestion
					String congestionString = "Congestion Unknown";
					if (totalDistance > 0.0) {
						double totalCongestion = congestion / totalDistance;
						if (totalCongestion < 0.67)
							congestionString = "Very Congested";
						if (totalCongestion >= 0.67 && totalCongestion <= 1.33)
							congestionString = "Slightly Congested";
						if (totalCongestion > 1.33)
							congestionString = "Traffic Moving Freely";
					}
	
					// Get confidence metric
					String confidenceString = "Confidence Unknown";
					if (totalDistance > 0.0) {
						double totalConfidence = confidence / totalDistance;
						if (totalConfidence < 0.67)
							confidenceString = "Not Confident";
						if (totalConfidence >= 0.67 && totalConfidence <= 1.33)
							confidenceString = "Confident";
						if (totalConfidence > 1.33)
							confidenceString = "Very Confident";
					}
					
					// Create distance string
					String distanceString;
					if (totalDistance > 1000)
						distanceString = decFormat.format(totalDistance/1000) + " km";
					else {
						if (totalDistance > 0)
							distanceString = decFormat.format(totalDistance) + " m";
						else
							distanceString = "N/A m";
					}
					
					
					// Create object and add it to array
					RoadDataAggregate tempRoadDataAggregate = new RoadDataAggregate(
							roadDetails.get(i).getSequenceID(), roadName,
							congestionString, confidenceString, averageSpeedString,
							speedLimitString, estimatedTimeString, distanceString);
					roadDataAggregate.add(tempRoadDataAggregate);
	
					// Add values to overall route calculations
					ocongestion += congestion;
					ototalDistance += totalDistance;
					ototalTime += totalTime;
					oconfidence += confidence;
	
					// Reset parameters
					roadName = "";
					congestion = 0;
					speedLimit = 0.0;
					totalDistance = 0.0;
					totalTime = 0;
					confidence = 0.0;
				}
			}
	
			Log.d (TAG, "Before overall route calculations");
			
			// Overall route calculations
			Log.d (TAG, "overall distance: " + ototalDistance);
			Log.d (TAG, "overall time: " + ototalTime);
			// Speed
			roadOverall[0] = "";
			if (ototalTime > 0)
				roadOverall[0] = decFormat.format(ototalDistance / ototalTime
						* 3.6);
	
			// Get estimated time
			roadOverall[1] = "";
			if (ototalTime > 0) {
				if (ototalTime >= 3600)
					roadOverall[1] += intFormat.format(ototalTime / 3600) + ":";
				else
					roadOverall[1] += "00:";
				if (ototalTime >= 60)
					roadOverall[1] += intFormat.format((ototalTime % 3600) / 60)
							+ ":";
				else
					roadOverall[1] += "00:";
				roadOverall[1] += intFormat.format(ototalTime % 60);
			}
	
			// Get distance
			roadOverall[2] = decFormat.format(ototalDistance / 1000);
	
			// Get congestion
			roadOverall[3] = "Congestion Unknown";
			if (ototalDistance > 0.0) {
				double ototalCongestion = ocongestion / ototalDistance;
				if (ototalCongestion < 0.67)
					roadOverall[3] = "Very Congested";
				if (ototalCongestion >= 0.67 && ototalCongestion <= 1.33)
					roadOverall[3] = "Slightly Congested";
				if (ototalCongestion > 1.33)
					roadOverall[3] = "Traffic Moving Freely";
			}
	
			// Get confidence metric
			roadOverall[4] = "Confidence Unknown";
			if (ototalDistance > 0.0) {
				double ototalConfidence = oconfidence / ototalDistance;
				if (ototalConfidence < 0.67)
					roadOverall[4] = "Not Confident";
				if (ototalConfidence >= 0.67 && ototalConfidence <= 1.33)
					roadOverall[4] = "Confident";
				if (ototalConfidence > 1.33)
					roadOverall[4] = "Very Confident";
			}
			Log.d (TAG, "After overall route calculations");

			// Clear refresh dialog if its showing
			if (force) {
				Message tempMessage = new Message();
				tempMessage.arg1 = 1;
				refreshHandlerDialog.sendMessage(tempMessage);
			}
			
		}
	}
	

	private synchronized boolean getLatestArrayListDataFromServer(ArrayList<RoadData> roadDetails) {
		
		// Execute thread that searches for response only if all previous threads have completed
		String tempResponses = httpResponses.trim();
		if (!(obtainTask.getStatus() == AsyncTask.Status.RUNNING))
		{
			Log.d(TAG, "Going to start another async task");
			obtainTask = new ObtainDataTask();
			obtainTask.execute(new String [] {}); // Empty string array
		}
		
		// Proceed only if data is available
		if (tempResponses.equals(""))
		{
			//Log.d(TAG, "No (new) data yet");
			return false;
		}
		
		
		// Strings which store JSONs
		String jsonRoute = "";
		String jsonAlerts = "";
		
		// Extract both JSONs from HTTP response
		try
		{
			httpResponses = "";
			if (tempResponses.contains("JSONROUTE["))
			{
				if (tempResponses.contains("JSONALERTS["))
				{
					jsonRoute = tempResponses.substring(tempResponses.indexOf('['), tempResponses.indexOf("JSONALERTS[")).trim();
					jsonAlerts = tempResponses.substring(tempResponses.indexOf("JSONALERTS[") + 10).trim();
				}
				else
				{
					jsonRoute = tempResponses.trim();
				}
			}
			else
			{
				if (tempResponses.contains(jsonAlerts))
					jsonAlerts = tempResponses.substring(tempResponses.indexOf("JSONALERTS[") + 10).trim();
			}
			Log.d (TAG, "Route: " + jsonRoute);
			Log.d (TAG, "Alerts: " + jsonAlerts);
		}
		catch (Exception e)
		{  Log.d (TAG, "Failed to extract JSONs");}
		
		
		// Parse Route XML
		try
		{
	      	JSONArray jArray = new JSONArray(jsonRoute);
	      	JSONObject json_data=null;
	      	RoadData tempRoadData;
	      	if (jArray.length() != 0)
	      		roadDetails.clear();
	      	for(int i=0; i<jArray.length();i++)
	      	{
	      		//Log.d (TAG, "Before json_data");
				json_data = jArray.getJSONObject(i);
				//Log.d (TAG, i + ": " + json_data.toString());
				tempRoadData = new RoadData (
						json_data.getInt("SequenceID"), 
						json_data.getInt("InternalID"),
						json_data.getString("RoadName"),
						json_data.getDouble("Congestion"),
						json_data.getDouble("Distance"),
						json_data.getDouble("SpeedLimit"),
						json_data.getDouble("ExpectedTime"),
						json_data.getDouble("Confidence"),
						json_data.getDouble("Latitude"),
						json_data.getDouble("Longitude"));
				roadDetails.add(tempRoadData);
	      	}
		}
		catch(Exception e1){
			Log.d (TAG, "Exception in parsing route JSON");
		}
		
		// Parse Alerts XML
		try
		{
	      	JSONArray jArray = new JSONArray(jsonAlerts);
	      	JSONObject json_data=null;
	      	RoadAlerts tempRoadAlerts;
	      	if (jArray.length() != 0)
	      		roadAlerts.clear();
	      	for(int i=0; i<jArray.length();i++)
	      	{
	      		//Log.d (TAG, "Before json_data");
				json_data = jArray.getJSONObject(i);
				//Log.d (TAG, i + ": " + json_data.toString());
				tempRoadAlerts = new RoadAlerts (
						json_data.getString("Message"), 
						new Date (json_data.getLong("Time")).toLocaleString(),
						json_data.getDouble("Latitude"),
						json_data.getDouble("Longitude"),
						json_data.getString("Type"));
				roadAlerts.add(tempRoadAlerts);
	      	}
		}
		catch(Exception e1){
			Log.d (TAG, "Exception in parsing alerts JSON");
		}
		return true;
	}
	
	public void updateChildUI()
	{
		try
		{
			// Dismiss progress bar if its still present
			closeDialog();
			
			String childActivity = getCurrentActivity().toString();
			if (childActivity.length() >= 32 && childActivity.substring(15, 32).equals("DashboardActivity")) {
				((DashboardActivity) getCurrentActivity()).handlerUpdate.sendMessage(new Message());
				Log.d (TAG, "yoyooyoy");
			}
			if (childActivity.length() >= 30 && childActivity.substring(15, 30).equals("DetailsActivity"))
				((DetailsActivity) getCurrentActivity()).handlerUpdate.sendMessage(new Message());
			if (childActivity.length() >= 29 && childActivity.substring(15, 29).equals("AlertsActivity"))
				((AlertsActivity) getCurrentActivity()).handlerUpdate.sendMessage(new Message());
			if (childActivity.length() >= 31 && childActivity.substring(15, 31).equals("RouteMapActivity"))
				((RouteMapActivity) getCurrentActivity()).handlerUpdate.sendMessage(new Message());
		
		}
		catch (Exception e)
		{ Log.d (TAG, "Error in updateChildUI");}
	}
	
	
	public void closeDialog () {
		try
		{
			if (dialog!=null && dialog.isShowing())
				dialog.dismiss();
		}
		catch (Exception e) 
		{ Log.d (TAG, "Exception in closeDialog");}
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
			// Get refresh frequency
			String tempFrequency = prefs.getString("refreshFrequency", "1 Minute");
			refreshFrequency = Integer.parseInt(tempFrequency.substring(0, tempFrequency.indexOf(' ')))
				* 1000;
			if (!tempFrequency.substring(tempFrequency.indexOf(' ') + 1).equals("Seconds"))
				refreshFrequency *= 60;
			
			// Get active origin preference
			activeOrigin = prefs.getBoolean("activeOrigin", false);
		}
		catch (Exception e) {
			Log.d (TAG, "Exception in store preferences");
		}
	}

	// Async task which obtains data in the background
	private class ObtainDataTask extends AsyncTask<String, Void, String> {
		
		@Override
		protected String doInBackground(String... urls) {
			
			// Variables used to send data
    		HttpClient hc = new DefaultHttpClient();
    		HttpPost post;
    		HttpResponse resp;
    		String response = "";
    		
    		Log.d (TAG, "ObtainDataTask Initiated: " + httpResponses);
    		
			try
			{
				// Construct request url
				String url = urlHost + "routerequest.php?";
				if (!initialAlertDialog) {
					
					// Use either current location or origin based on user preference
					if (activeOrigin && (currentLat!=0.0 && currentLon!=0.0))
						url += "lat1=" + currentLat + 
							"&lon1=" + currentLon;
					else
						url += "lat1=" + originLat + 
						"&lon1=" + originLon;
					
					// Add destination to url request
					url += "&lat2=" + destinationLat + 
						"&lon2=" + destinationLon;
				}
				else
				{
					// Use either current location or origin based on user preference
					if (activeOrigin && (currentLat!=0.0 && currentLon!=0.0))
						url += "lat1=" + currentLat + 
							"&lon1=" + currentLon;
					else
						url += "lat1=" + originLat + 
						"&lon1=" + originLon;
					
					// Add destination to url request
					url += "&lat2=" + destinationLat + 
						"&lon2=" + destinationLon;
				}
				
				Log.d (TAG, "ObtainDataTaskAsync Obtaining: " + url);
				
				if (debugServer == true) {
					response = debugResponse;
				}
				else {
					post = new HttpPost(url);
					resp = hc.execute(post);
	        		BufferedReader reader =  new BufferedReader(new InputStreamReader(resp.getEntity().getContent(),"iso-8859-1"),8);
	        		if ((response = reader.readLine()) != null) {
		        		Log.d (TAG, "ObtainDataTaskAsync Obtained: " + response);
	    			}
				}
				
	        	//Toast.makeText(MenuTabActivity.this, "Sent: " + url, Toast.LENGTH_SHORT).show();
			}
			catch (Exception e)
			{ Log.d (TAG, "ObtainDataTaskAsync: Exception");}
			
			// Dismiss progress bar if its still present
			closeDialog();

			Log.d (TAG, "Before checkResponse " + response);
			
			// If response is not appropriate, and this is the first 
			if (!checkResponseSyntax(response))
			{
				Log.d (TAG, "In if response");
				isReloadNecessary = true; // Since adequate data is unavailable, retry
				Log.d (TAG, "httpresponses: " + httpResponses);
			}
			else
			{

				Log.d (TAG, "In else response");
				alert = null;
				if (!initialAlertDialog)
				{
					isReloadNecessary = true; // Since data has just presented itself, reload
					initialAlertDialog = true;
				}
				else
					isReloadNecessary = false;
				httpResponses = response; // Assign response if its of proper syntax
			}
			
			// Mandatory return
			Log.d (TAG, "Before response");
			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			Log.d (TAG, "ObtainDataTask result:  " + result.length());
			// Display alert if it has not been shown before
			if (!initialAlertDialog)
			{
				alert.show();
				initialAlertDialog = true;
			}
			if (isReloadNecessary)
			{
				Log.d (TAG, "On post execute");
				manageArrayListData(true);
				updateChildUI();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}
		
		private boolean checkResponseSyntax (String response)
		{
			Log.d (TAG, "Inside checkresponsesyntax");
			if (response.contains("JSONROUTE") || response.contains("JSONALERT"))
				return true;
			return false;
		}
		
	}
	
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
	
	/*private void addSampleData () {
		// Add received data to arraylists
		int InternalID = 1;
		Log.d (TAG, "Adding 1st");
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Orchard Road" , 1, 41.43, 50, 10, 1, 1.306842, 103.828466));
		Log.d (TAG, "Adding 2nd");
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Orchard Road" , 2, 42.43, 50, 10, 1, 1.306687, 103.828792));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Orchard Road" , 2, 43.43, 50, 10, 1, 1.305762, 103.830608));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Orchard Road" , 2, 44.43, 50, 10, 1, 1.305714, 103.830698));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Orchard Road" , 2, 45.43, 50, 10, 1, 1.305117, 103.831778));
		tempCount++;
		
		SequenceID++;
		InternalID = 1;
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road", 1, 46.43, 50, 10, 1, 1.305440, 103.832133));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road", 1, 47.43, 50, 10, 1, 1.306181, 103.832524));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road", 1, 48.43, 50, 10, 1, 1.306529, 103.832683));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road", 1, 49.43, 50, 10, 1, 1.307115, 103.832907));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road", 1, 50.43, 50, 10, 1, 1.308169, 103.833321));
		
		SequenceID++;
		InternalID = 1;
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road II", 0, 51.43, 50, 10, 1, 1.308893,103.833617));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road II", 0, 52.43, 50, 10, 1, 1.309389, 103.833885));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road II", 1, 53.43, 50, 10, 1, 1.309773, 103.834119));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road II", 0, 54.43, 50, 10, 1, 1.310715, 103.835491));
		roadDetails.add(new RoadData(SequenceID, (InternalID++), "Scotts Road II", 1, 55.43, 50, 10, 1, 1.312876, 103.838745));
		Log.d (TAG, "Data obtained from server");
		
		
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on PIE", "6:53 PM", 1.292407,103.779630));
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on Changi Road", "12:19 PM", 1.292787,103.779133));
		roadAlerts.add(new RoadAlerts("Roadworks at Choa Chu Kang", "2:31 PM", 1.293711,103.776086));
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on PIE", "6:53 PM", 1.292407,103.779630));
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on Changi Road", "12:19 PM", 1.292787,103.779133));
		roadAlerts.add(new RoadAlerts("Roadworks at Choa Chu Kang", "2:31 PM", 1.293711,103.776086));
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on PIE", "6:53 PM", 1.292407,103.779630));
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on Changi Road", "12:19 PM", 1.292787,103.779133));
		roadAlerts.add(new RoadAlerts("Roadworks at Choa Chu Kang", "2:31 PM", 1.293711,103.776086));
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on PIE", "6:53 PM", 1.292407,103.779630));
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on Changi Road", "12:19 PM", 1.292787,103.779133));
		roadAlerts.add(new RoadAlerts("Roadworks at Choa Chu Kang", "2:31 PM", 1.293711,103.776086));
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on PIE", "6:53 PM", 1.292407,103.779630));
		roadAlerts.add(new RoadAlerts("Vehicle breakdown on Changi Road", "12:19 PM", 1.292787,103.779133));
		roadAlerts.add(new RoadAlerts("Roadworks at Choa Chu Kang", "2:31 PM", 1.293711,103.776086));
		
	}*/
}