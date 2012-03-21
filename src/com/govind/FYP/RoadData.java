package com.govind.FYP;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class RoadData implements Parcelable{

	private static String TAG = RoadData.class.getSimpleName();
	private int SequenceID;
	private int InternalID;
	private String RoadName;
	private double Congestion;
	private double Distance;
	private double SpeedLimit; 
	private double ExpectedTime;    
	private double Confidence;
	private double Latitude;
	private double Longitude;
	
	public RoadData (int SequenceID, int InternalID, String RoadName, double Congestion, double Distance, double SpeedLimit, double ExpectedTime, double Confidence, double Latitude, double Longitude){
		this.SequenceID = SequenceID;
		this.InternalID = InternalID;
		this.RoadName = RoadName;
		this.Congestion = Congestion;
		this.Distance = Distance;
		this.SpeedLimit = SpeedLimit;
		this.ExpectedTime = ExpectedTime;
		this.Confidence = Confidence;
		this.Latitude = Latitude;
		this.Longitude = Longitude;
	}
	
    public int getSequenceID() {
        return SequenceID;
    }
	
    public int getInternalID() {
        return InternalID;
    }
	
    public String getRoadName() {
        return RoadName;
    }
	
    public double getCongestion() {
    	return Congestion;
	}
	
    public Double getDistance(){
    	return Distance;
    }
	
    public Double getSpeedLimit(){
    	return SpeedLimit;
    }
	
    public double getExpectedTime(){
    	return ExpectedTime;
    }
    
    public double getConfidence() {
    	return Confidence;
	}
    
    public double getLatitude() {
    	return Latitude;
	}
    
    public double getLongitude() {
    	return Longitude;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Log.v(TAG, "writeToParcel..."+ flags);
		dest.writeInt(SequenceID);
		dest.writeInt(InternalID);
		dest.writeString(RoadName);
		dest.writeDouble(Congestion);
		dest.writeDouble(Distance);
		dest.writeDouble(SpeedLimit);
		dest.writeDouble(ExpectedTime);
		dest.writeDouble(Confidence);
		dest.writeDouble(Latitude);
		dest.writeDouble(Longitude);
	}
	
	public class MyCreator implements Parcelable.Creator<RoadData> {
	      public RoadData createFromParcel(Parcel source) {
	            return new RoadData(source);
	      }
	      public RoadData[] newArray(int size) {
	            return new RoadData[size];
	      }
	}
    
	// Reconstruct from the parcel
	public RoadData(Parcel source){
        Log.v(TAG, "RoadData(Parcel source): time to put back parcel data");
        SequenceID = source.readInt();
        InternalID = source.readInt();
        RoadName = source.readString();
        Congestion = source.readInt();
        Distance = source.readDouble();
        SpeedLimit = source.readDouble();
        ExpectedTime = source.readDouble();
        Confidence = source.readInt();
        Latitude = source.readDouble();
        Longitude = source.readDouble();
  }
}
