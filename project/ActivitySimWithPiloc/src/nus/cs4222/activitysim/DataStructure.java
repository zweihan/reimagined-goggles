package nus.cs4222.activitysim;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;

public class DataStructure {
	

	
	public static class Fingerprint implements Serializable
	{
		public String mMac;
		public Integer mRSSI;
		public Integer mFrequency;

		public Fingerprint(String m, int r, int f) {
			mMac = m;
			mRSSI = r;
			mFrequency = f;
		}

	}

	//Hash map of point to vector of fingerprint
	public static class RadioMap
	{
		//public Vector<LocFP> mLocFPList;
		public HashMap<LatLng,Vector<Fingerprint>> mLocFingerPrints;

		RadioMap(HashMap<LatLng,Vector<Fingerprint>> f)
		{
			this.mLocFingerPrints = f;
		}
	}

	
}
