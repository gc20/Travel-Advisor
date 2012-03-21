package com.govind.FYP;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class RouteMapActivity extends MapActivity {

	private static String TAG = RouteMapActivity.class.getSimpleName();
	private MapView mapView;
	ArrayList <RoadData> roadDetails;
	ArrayList <RoadDataAggregate> roadDataAggregate;
	ArrayList <RoadAlerts> roadAlerts;
	
	// Key geopoints and associated variables for appropriate zoom 
	GeoPoint origin;
	GeoPoint destination;
	GeoPoint lastPoint;
	GeoPoint currentPoint = null;
	GeoPoint temppoint;
	
	// Store specific overlays for zoom
	MapsOverlayDraw routeOverview;
	MapsOverlayDraw currentLocationOverlay;
	
	// Other locations and related managers
	LocationManager locationManager;
	
	// Stores all overlays
	List<Overlay> mapOverlays;
	OverlayItem overlayitem;
	
	// Stores position of current location overlay
	int currentPositionLocation = -1;
	
	
	@Override
	protected void onCreate(Bundle arg0) {

		// Initialize variables/views
		Log.d(TAG, "On create");
		super.onCreate(arg0);
		setContentView(R.layout.mapview);
		
		// Get necessary parent data
		roadDetails = ((MenuTabActivity) (this.getParent())).roadDetails;
		roadDataAggregate = ((MenuTabActivity) (this.getParent())).roadDataAggregate;
		roadAlerts = ((MenuTabActivity) (this.getParent())).roadAlerts;
		
		// Get map
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

        // Let tab activity handle updating of array list and associated UI
		((MenuTabActivity) (this.getParent())).manageArrayListData(false);
		
		// Add overlays
		updateUI();
		
		// Prepare current location overlay
		currentLocationOverlay = new MapsOverlayDraw(this.getResources().getDrawable(R.drawable.ic_maps_indicator_current_position), this);

		// Center map on origin
		mapView.getController().animateTo(origin);
		mapView.getController().setZoom(19);
	}

	@Override
	protected void onStart() {
		
		super.onStart();
        Log.d (TAG, "onStart");
		   Log.d (TAG, "mapdata");
		updateUI();
		   Log.d (TAG, "child UI");
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
        Log.d (TAG, "onResume");
        
        // Let tab activity handle updating of array list and associated UI
		((MenuTabActivity) (this.getParent())).manageArrayListData(false);
		   Log.d (TAG, "mapdata");
		updateUI();
		   Log.d (TAG, "child UI");
	}


	@Override
	public void onBackPressed() {
		((MenuTabActivity) (this.getParent())).onBackPressed();
		super.onBackPressed();
	}
	
	
	public String getSnippet (int SequenceID)
	{
		int len = roadDataAggregate.size();
		for (int i=0; i<len; i++)
		{
			if (roadDataAggregate.get(i).getSequenceID() == SequenceID)
			{
				RoadDataAggregate tempRoadDataAggregate = roadDataAggregate.get(i);
				return (tempRoadDataAggregate.getCongestion() +
					"\nAvg. Speed: " + tempRoadDataAggregate.getAvgSpeed() + 
					"\nExpected Time: " + tempRoadDataAggregate.getExpectedTime() +
					"\nDistance: " + tempRoadDataAggregate.getDistance() +
					"\nSpeed Limit: " + tempRoadDataAggregate.getSpeedLimit() +
					"\nPrediction: " + tempRoadDataAggregate.getConfidence());
			}
		}
		return "";
	}

	public void centerOverlays(MapsOverlayDraw itemizedoverlay) {
	    
		// Broad boundaries of Singapore
		int len = itemizedoverlay.size();
	    if (len == 0)
	    	return;
	    GeoPoint tempPoint = itemizedoverlay.getItem(0).getPoint();
	    int minLat = tempPoint.getLatitudeE6();
	    int maxLat = tempPoint.getLatitudeE6();
	    int minLon = tempPoint.getLongitudeE6();
	    int maxLon = tempPoint.getLongitudeE6();
	    
	    for (int i = 0; i < itemizedoverlay.size(); i++) 
	    {
	    	
	    	tempPoint = itemizedoverlay.getItem(i).getPoint();
	    	
	    	// Get minimum and maximum latitudes
	    	if (tempPoint.getLatitudeE6() > maxLat)
	    		maxLat = tempPoint.getLatitudeE6();
	    	else
	    	{
	    		if (tempPoint.getLatitudeE6() < minLat)
	    			minLat = tempPoint.getLatitudeE6();
	    	}
	    		
	    	// Get minimum and maximum longitudes
	    	if (tempPoint.getLongitudeE6() > maxLon)
	    		maxLon = tempPoint.getLongitudeE6();
	    	else
	    	{
	    		if (tempPoint.getLongitudeE6() < minLon)
	    			minLon = tempPoint.getLongitudeE6();
	    	}
	    }
	    
	    Log.d(TAG, maxLat + " " + maxLon + " " + minLat + " " + minLon);
	    mapView.getController().zoomToSpan((int)((maxLat - minLat)*1.1), (int)((maxLon - minLon)*1.1));
	    mapView.getController().animateTo(new GeoPoint((maxLat + minLat) / 2, (maxLon + minLon) / 2));
	    //mapView.getController().zoomOut();
	}
	


	// When "menu" is selected
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.mapmenu, menu);
		return true;
	}
	
	// When menu options are selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.routeOrigin:
			mapView.getController().animateTo(origin);
			mapView.getController().setZoom(19);
			break;
			
		case R.id.routeDestination:
			mapView.getController().animateTo(destination);
			mapView.getController().setZoom(19);
			break;
	
		case R.id.routeOverview:
			centerOverlays(routeOverview);
			break;
			
		case R.id.currentLocation:
			
			// Update overlay for current location
			updateCurrentLocation();
			
			// Zoom to current location, if it is available
			if (currentPoint== null || (currentPoint.getLatitudeE6() == 0.0 && currentPoint.getLongitudeE6() == 0.0))
			{
				Toast.makeText(this, "Current location is unavailable. Please retry shortly.", Toast.LENGTH_SHORT).show();
			}
			else
			{
				mapView.getController().animateTo(currentPoint);
				mapView.getController().setZoom(19);
			}
			break;
		
		case R.id.refresh:
			((MenuTabActivity)this.getParent()).refreshData();
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
				}
				startService(new Intent(this, SensorService.class));
				Toast.makeText(this, serviceMessage, Toast.LENGTH_SHORT).show();
				((MenuTabActivity)this.getParent()).registermLocationReceiver();
				break;
			}
			catch (Exception e)
			{e.printStackTrace();
			Log.d (TAG, "Exception in sensorServiceStart");}

		case R.id.sensorServiceStop:
			((MenuTabActivity)this.getParent()).unregistermLocationReceiver();
			stopService(new Intent(this, SensorService.class));
			Toast.makeText(this, "Sensor service stopped", Toast.LENGTH_SHORT).show();
			break;
	
		default:
			break;
		}

		return true;
	}
	
	
	@Override
	protected void onDestroy() {
		//db.close(); // Close database
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	public void updateUI ()
	{
		Log.d (TAG, "UpdateUI");
		// Initialize itemized overlay
		MapsOverlayDraw itemizedoverlay = new MapsOverlayDraw(this.getResources().
				getDrawable(R.drawable.locationmarker_black), this);
		
		// Obtain map overlays
	    mapOverlays = mapView.getOverlays();
		
		// Remove existing overlays
		mapOverlays.clear();

		// Add current location markers
		currentPositionLocation = -1;
		updateCurrentLocation ();
		
		// Plot starting and ending points
	    int len = roadDetails.size();
		if (len > 0)
		{
			// Starting point
			temppoint = new GeoPoint ((int) (roadDetails.get(0).getLatitude() * 1e6),
					(int) (roadDetails.get(0).getLongitude() * 1e6));
			overlayitem = new OverlayItem(temppoint, roadDetails.get(0).getRoadName(), 
					getSnippet (roadDetails.get(0).getSequenceID()));
			itemizedoverlay.addOverlay(overlayitem);
			origin = temppoint;
			
			// Ending point
			temppoint = new GeoPoint ((int) (roadDetails.get(len-1).getLatitude() * 1e6),
					(int) (roadDetails.get(len-1).getLongitude() * 1e6));
			overlayitem = new OverlayItem(temppoint, roadDetails.get(len-1).getRoadName() + " (Road Ending)",
					getSnippet (roadDetails.get(len-1).getSequenceID()));
			itemizedoverlay.addOverlay(overlayitem);
			destination = temppoint;
		}
		mapOverlays.add(itemizedoverlay);
		Log.d (TAG, "Plotted starting and ending points");
		
		// Draw route lines and plot intermediate routes 
		for (int i=1; i<len; i++)
		{
			Log.d(TAG, "Overlay creation");
			MapsRouteDraw myLocationOverlay = new MapsRouteDraw(
					new GeoPoint((int) (roadDetails.get(i-1).getLatitude() * 1e6),
							(int) (roadDetails.get(i-1).getLongitude() * 1e6)), new GeoPoint(
							(int) (roadDetails.get(i).getLatitude() * 1e6),
							(int) (roadDetails.get(i).getLongitude() * 1e6)));
			mapOverlays.add(myLocationOverlay);
			
			if (roadDetails.get(i).getInternalID()==1)
			{
				temppoint = new GeoPoint ((int) (roadDetails.get(i).getLatitude() * 1e6),
						(int) (roadDetails.get(i).getLongitude() * 1e6));
				overlayitem = new OverlayItem(temppoint, roadDetails.get(i).getRoadName(),
						getSnippet (roadDetails.get(i).getSequenceID()));
				itemizedoverlay.addOverlay(overlayitem);
				mapOverlays.add(itemizedoverlay);
			}
		}
		
		// Store route overview overlay
		routeOverview = itemizedoverlay;
		
		// Add alert signs
		Log.d(TAG, "Alerts " + 1);
		len = roadAlerts.size();
		itemizedoverlay = new MapsOverlayDraw(this.getResources().getDrawable(R.drawable.ic_bullet_key_permission), this);
		RoadAlerts tempRoadAlerts;
		Log.d(TAG, "Alerts " + 2 + " " + len);
		for (int i=0; i<len; i++)
		{
			Log.d(TAG, "Adding Alert " + i);
			tempRoadAlerts = roadAlerts.get(i);
			temppoint = new GeoPoint((int) (tempRoadAlerts.getLatitude() * 1e6), 
					(int) (tempRoadAlerts.getLongitude() * 1e6));
			overlayitem = new OverlayItem(temppoint, tempRoadAlerts.getAlertTime(), 
					tempRoadAlerts.getAlertMessage());
			itemizedoverlay.addOverlay(overlayitem);
		}
		mapOverlays.add(itemizedoverlay);
	}
	
	
	// Add current location marker
	private void updateCurrentLocation() {
		

		// Remove last current location overlay
		try {
			if (currentPositionLocation != -1)
				mapOverlays.remove(currentPositionLocation);
		}
		catch (Exception e) {
			Log.d (TAG, "Could not remove last current location overlay");
		}
		
		try {
			// Proceed only if location is available
			if (((MenuTabActivity)this.getParent()).currentLat!=0.0 && 
					((MenuTabActivity)this.getParent()).currentLon!=0.0)
			{
				// Calculate current point
				currentPoint = new GeoPoint(
						(int)(((MenuTabActivity)this.getParent()).currentLat * 1e6), 
						(int)(((MenuTabActivity)this.getParent()).currentLon * 1e6)); 
				
				// Add current location overlay
				currentLocationOverlay.addOverlay(new OverlayItem(currentPoint, null, "Current Location"));
				mapOverlays.add (currentLocationOverlay);
				
				// Store index of current position location
				currentPositionLocation = mapOverlays.size()-1;
			}
		}
		catch (Exception e) {
			Log.d (TAG, "Error adding current location overlay");
		}
	}



	public Handler handlerUpdate = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			updateUI();
		}
	};

}
