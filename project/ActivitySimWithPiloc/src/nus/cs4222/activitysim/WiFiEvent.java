package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

import nus.cs4222.activitysim.DataStructure.Fingerprint;

/**
   WiFi sensor event.
 */
public class WiFiEvent 
    extends SimulatorEvent {

    // Sensor data
    private Vector< Fingerprint > fingerprintVector;

    /** Constructor that initialises the sensor data. */
    public WiFiEvent( long timestamp , 
                      Vector< Fingerprint > fingerprintVector ) {
        // Init the timestamp and sequence number
        super( timestamp );
        // Store the sensor data
        this.fingerprintVector = fingerprintVector;
    }

    /** Handles the event. */
    @Override
    public void handleEvent() {
        ActivityDetection detectionAlgo = ActivitySimulator.getDetectionAlgorithm();
        detectionAlgo.onWiFiSensorChanged( timestamp , 
                                           fingerprintVector );
    }
}
