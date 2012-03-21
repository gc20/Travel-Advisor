package com.govind.FYP;

public class RoadAlerts {

	private String AlertMessage;
	private String AlertTime;
	private double Latitude;
	private double Longitude;
	private String AlertType;
	
	public RoadAlerts (String AlertMessage, String AlertTime, Double Latitude, Double Longitude, String AlertType){
		this.AlertMessage = AlertMessage;
		this.AlertTime = AlertTime;
		this.Latitude = Latitude;
		this.Longitude = Longitude;
		this.AlertType = AlertType;
	}
	
	public String getAlertMessage() {
        return AlertMessage;
    }
	
    public String getAlertTime() {
    	return AlertTime;
	}
	
    public double getLatitude() {
    	return Latitude;
	}
	
    public double getLongitude() {
    	return Longitude;
	}
	
	public String getAlertType() {
        return AlertType;
    }

}
