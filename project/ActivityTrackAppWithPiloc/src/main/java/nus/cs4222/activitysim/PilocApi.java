package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

import com.google.android.gms.maps.model.LatLng;

import nus.cs4222.activitysim.DataStructure.Fingerprint;
import nus.cs4222.activitysim.DataStructure.RadioMap;

public class PilocApi {

    private RadioMap mRadioMap = new RadioMap(null);
    private double mLocalizationError =-1;
    private HashMap<LatLng, HashMap<String, Integer>> mRadioGuassianMap = new HashMap<>();

    public LatLng getLocation(Vector<Fingerprint> fp) {
        if (mRadioMap == null || fp == null)
            return null;
        return WeightedLocalization(fp, mRadioGuassianMap.keySet());
    }

    public LatLng WeightedLocalization(Vector<Fingerprint> fp, Set<LatLng> CandidateSet) {

        LatLng Key = null;
        double sum;
        double maxScore = 0;

        for (LatLng k : CandidateSet) {
            int number = 0;
            sum = 0;
            for (Fingerprint f2 : fp) {
                if (mRadioGuassianMap.get(k).containsKey(f2.mMac)) {
                    number++;
                    double diff = 1;
                    double impactFactor = 1.0 / mRadioGuassianMap.get(k).get(f2.mMac);
                    if (mRadioGuassianMap.get(k).get(f2.mMac) != f2.mRSSI) {
                        diff = Math.abs(mRadioGuassianMap.get(k).get(f2.mMac) - f2.mRSSI);
                        sum += impactFactor * (1.0 / diff);
                    } else {
                        sum += impactFactor * (1.0);
                    }
                }
            }

            if (number > fp.size() / 2 && sum > maxScore) {
                maxScore = sum;
                Key = new LatLng(k.latitude, k.longitude);
            }
        }
        return Key;
    }

    public RadioMap loadRadioMap( File radiomapFile ) {
        try {
            try {
                BufferedReader in = new BufferedReader(new FileReader(radiomapFile));

                String readLine;
                Vector<String> result = new Vector<String>();
                while ((readLine = in.readLine()) != null) {
                    result.add(readLine);
                }
                in.close();

                // loadRadioMapFromString(result);
                loadRadioGaussianMapFromString(result);

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return mRadioMap;
    }



    public RadioMap loadRadioGaussianMapFromString(Vector<String> mapStrings) {
        try {
            HashMap<LatLng, Vector<Fingerprint>> locMap = new HashMap<>();
            mRadioGuassianMap = new HashMap<>();

            for (String line : mapStrings) {
                HashMap<String, Integer> gpHash = new HashMap<>();
                String[] tokens = line.split(" ");
                if(tokens.length == 3){
                    mLocalizationError = Double.parseDouble(tokens[2]);
                }else {
                    LatLng loc = new LatLng(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1]));
                    Vector<Fingerprint> fp = new Vector<>();
                    for (int i = 0; i < (tokens.length - 2) / 2; i++) {
                        String mac = tokens[i * 2 + 2];
                        String[] element = tokens[i * 2 + 3].split(",");
                        if(element.length==4){
                            int rssi = Integer.parseInt(element[0]);
                            fp.add(new Fingerprint(mac, rssi, 0));
                            gpHash.put(mac, rssi);
                        }

                    }

                    locMap.put(loc, fp);
                    mRadioGuassianMap.put(loc, gpHash);
                }

            }
            mRadioMap.mLocFingerPrints = locMap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return mRadioMap;
    }
}
