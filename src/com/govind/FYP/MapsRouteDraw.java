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


public class MapsRouteDraw extends Overlay {

	private static String TAG = MapsRouteDraw.class.getSimpleName();
	public ArrayList <RoadData> roadDetails = new ArrayList <RoadData> ();
	GeoPoint gp1;
	GeoPoint gp2;
	
	public MapsRouteDraw (GeoPoint gp1, GeoPoint gp2)
	{
		//this.roadDetails = roadDetails;
		this.gp1 = gp1;
		this.gp2 = gp2;
		Log.d(TAG, "Constructor");
	}
	
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {  
		
		//Log.d(TAG, "In draw method");
	    Projection projection = mapView.getProjection();  
	    Paint paint = new Paint();  
	    Point point = new Point();  
	    projection.toPixels(gp1, point);  
	    //paint.setColor(Color.rgb(153, 51, 155));  
	    paint.setColor(Color.GRAY);
	    Point point2 = new Point();  
	    projection.toPixels(gp2, point2);  
	    paint.setStrokeWidth(6);  
	    paint.setAlpha(120);  
	    canvas.drawLine(point.x, point.y, point2.x, point2.y, paint);  
	    super.draw(canvas, mapView, shadow);  
	}  
	
}
