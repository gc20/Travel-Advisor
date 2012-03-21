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

public class AlertsMapsActivity extends MapActivity {

	private static String TAG = AlertsMapsActivity.class.getSimpleName();
	private MapView mapView;
	
	@Override
	protected void onCreate(Bundle arg0) {

		Log.d(TAG, "On create");
		
		List<Overlay> mapOverlays;
		Drawable drawable;
		MapsOverlayDraw itemizedoverlay;
		GeoPoint point;
		double latitude, longitude;
		OverlayItem overlayitem;

		super.onCreate(arg0);
		Log.d (TAG, "after super create");
		setContentView(R.layout.mapview);

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		// Initialize variables for overlap
		mapOverlays = mapView.getOverlays();
		//drawable = this.getResources().getDrawable(R.drawable.alerticon);
		drawable = this.getResources().getDrawable(R.drawable.ic_bullet_key_permission);
		itemizedoverlay = new MapsOverlayDraw(drawable, this);
		
		// Add elements to map
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			latitude = Double.parseDouble(extras.getString("Latitude"));
			longitude = Double.parseDouble(extras.getString("Longitude"));

			// Plot element on map (by adding to itemizedOverlay)
			point = new GeoPoint((int) (latitude * 1e6), (int) (longitude * 1e6));
			overlayitem = new OverlayItem(point, extras.getString("IncidentTime"), 
					extras.getString("IncidentMessage"));

			itemizedoverlay.addOverlay(overlayitem);
			mapView.getController().animateTo(point);
		}

		// Add itemizedoverlap to map
		mapOverlays.add(itemizedoverlay);
		Log.d(TAG, Integer.toString (itemizedoverlay.getLatSpanE6()));
		Log.d(TAG, Integer.toString (itemizedoverlay.getLonSpanE6()));
		mapView.getController().zoomToSpan(itemizedoverlay.getLatSpanE6() + 10000, itemizedoverlay.getLonSpanE6() + 10000);
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
