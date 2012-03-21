package com.govind.FYP;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class RoadDataAggregate implements Parcelable{

	private static String TAG = RoadDataAggregate.class.getSimpleName();
	private int SequenceID;
	private String RoadName;
	private String Congestion;
	private String Confidence;
	private String AvgSpeed;
	private String SpeedLimit; 
	private String ExpectedTime;
	private String Distance;
	
	
	public RoadDataAggregate (int SequenceID, String RoadName, String Congestion, String Confidence, String AvgSpeed, String SpeedLimit, String ExpectedTime, String Distance){
		this.SequenceID = SequenceID;
		this.RoadName = RoadName;
		this.Congestion = Congestion;
		this.Confidence = Confidence;
		this.AvgSpeed = AvgSpeed;
		this.SpeedLimit = SpeedLimit;
		this.ExpectedTime = ExpectedTime;
		this.Distance = Distance;
	}
	
    public int getSequenceID() {
        return SequenceID;
    }
	
    public String getRoadName() {
        return RoadName;
    }
	
    public String getCongestion() {
    	return Congestion;
	}

	public String getConfidence() {
    	return Confidence;
	}
	
    public String getAvgSpeed() {
    	return AvgSpeed;
	}
	
    public String getSpeedLimit() {
    	return SpeedLimit;
	}
	
    public String getExpectedTime() {
    	return ExpectedTime;
	}
	
    public String getDistance() {
    	return Distance;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Log.v(TAG, "writeToParcel..."+ flags);
		dest.writeInt(SequenceID);
		dest.writeString(RoadName);
		dest.writeString(Congestion);
		dest.writeString(Confidence);
		dest.writeString(AvgSpeed);
		dest.writeString(SpeedLimit);
		dest.writeString(ExpectedTime);
		dest.writeString(Distance);
	}
	
	public class MyCreator implements Parcelable.Creator<RoadDataAggregate> {
	      public RoadDataAggregate createFromParcel(Parcel source) {
	            return new RoadDataAggregate(source);
	      }
	      public RoadDataAggregate[] newArray(int size) {
	            return new RoadDataAggregate[size];
	      }
	}
    
	// Reconstruct from the parcel
	public RoadDataAggregate(Parcel source){
        Log.v(TAG, "RoadData(Parcel source): time to put back parcel data");
        SequenceID = source.readInt();
        RoadName = source.readString();
        Congestion = source.readString();
        Confidence = source.readString();
        AvgSpeed = source.readString();
        SpeedLimit = source.readString();
        ExpectedTime = source.readString();
        Distance = source.readString();
  }
}
