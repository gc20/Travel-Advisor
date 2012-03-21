package com.govind.FYP;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MapsRoadActivity extends MapActivity {

	private static String TAG = MapsRoadActivity.class.getSimpleName();
	private MapView mapView;
	
	@Override
	protected void onCreate(Bundle arg0) {

		Log.d(TAG, "On create");
		
		// Initialize variables 
		GeoPoint point;
		super.onCreate(arg0);
		Log.d (TAG, "after super create");
		setContentView(R.layout.mapview);

		// Setup map
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		List<Overlay> mapOverlays = mapView.getOverlays();

		// Draw route
		Bundle extras = getIntent().getExtras();
		if (extras != null) 
		{
			// Get data
			double lat[] = extras.getDoubleArray("Latitudes");
			double lon[] = extras.getDoubleArray("Longitudes");
			int size = extras.getInt("Size");
			
			// Mark route
			for (int i=1; i<size; i++)
			{
				Log.d(TAG, "Overlay creation " + Double.toString(lat[i-1]) + " " + Double.toString(lon[i-1]));
				MapsRouteDraw myLocationOverlay = new MapsRouteDraw(
						new GeoPoint((int)(lat[i-1] * 1e6), (int)(lon[i-1] * 1e6)),
						new GeoPoint((int)(lat[i] * 1e6), (int)(lon[i] * 1e6)));
				mapOverlays.add(myLocationOverlay);
			}
			
			if (size > 0)
			{
				Log.d(TAG, "Drawing process begun");
				// Draw circle at origin and end
				MapsCircleDraw myCircleOverlay = new MapsCircleDraw(new GeoPoint((int)(lat[0] * 1e6), (int)(lon[0] * 1e6)));
				mapOverlays.add(myCircleOverlay);
				myCircleOverlay = new MapsCircleDraw(new GeoPoint((int)(lat[size-1] * 1e6), (int)(lon[size-1] * 1e6)));
				mapOverlays.add(myCircleOverlay);
			
				// Initialize variables for overlay
				Drawable drawable = this.getResources().getDrawable(R.drawable.locationmarker_black);
				MapsOverlayDraw itemizedoverlay = new MapsOverlayDraw(drawable, this);
				point = new GeoPoint ((int)(lat[0] * 1e6), (int)(lon[0] * 1e6));
				OverlayItem overlayitem = new OverlayItem(point, extras.getString("RoadName"), extras.getString("Snippet"));
				itemizedoverlay.addOverlay(overlayitem);
				mapOverlays.add(itemizedoverlay);
				point = new GeoPoint ((int)(lat[size-1] * 1e6), (int)(lon[size-1] * 1e6));
				overlayitem = new OverlayItem(point, extras.getString("RoadName"), extras.getString("Snippet"));
				itemizedoverlay.addOverlay(overlayitem);
				mapOverlays.add(itemizedoverlay);
				
				// Zoom to relevant part of map
				centerOverlays (itemizedoverlay);
			}
		}
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

	    mapView.getController().zoomToSpan((maxLat - minLat), (maxLon - minLon));
	    mapView.getController().animateTo(new GeoPoint((maxLat + minLat) / 2, (maxLon + minLon) / 2));
	    //mapView.getController().zoomOut();
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

	
	
}
