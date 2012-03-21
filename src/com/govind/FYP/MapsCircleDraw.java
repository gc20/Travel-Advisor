package com.govind.FYP;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;


public class MapsCircleDraw extends Overlay {

	private static String TAG = MapsRouteDraw.class.getSimpleName();
	public ArrayList <RoadData> roadDetails = new ArrayList <RoadData> ();
	GeoPoint gp;
	
	public MapsCircleDraw (GeoPoint gp)
	{
		//this.roadDetails = roadDetails;
		this.gp = gp;
		Log.d(TAG, "Constructor");
	}
	
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {  
		
		//Log.d(TAG, "In draw method");
	    Projection projection = mapView.getProjection();  
	    Paint paint = new Paint(); 
        paint.setTextSize(14); 
        //paint.setColor(Color.rgb(102, 0, 255));  
	    paint.setColor(Color.GRAY);
        paint.setStrokeWidth(15);  
	    paint.setAlpha(120);  
	    paint.setAntiAlias(true);
	    
	    Point point = new Point();  
	    Point curScreenCoords = projection.toPixels(gp, point);
	    canvas.drawCircle((float)curScreenCoords.x, curScreenCoords.y, 10, paint);
	    super.draw(canvas, mapView, shadow);  
	}  
	
}
