package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Proximity sensor event.
 */
public class ProximityEvent 
    extends SimulatorEvent {

    // Sensor data
    private float proximity;
    private int accuracy;

    /** Constructor that initialises the sensor data. */
    public ProximityEvent( long timestamp , 
                           float proximity , 
                           int accuracy ) {
        // Init the timestamp and sequence number
        super( timestamp );
        // Store the sensor data
        this.proximity = proximity;
        this.accuracy = accuracy;
    }

    /** Handles the event. */
    @Override
    public void handleEvent() {
        ActivityDetection detectionAlgo = ActivitySimulator.getDetectionAlgorithm();
        detectionAlgo.onProximitySensorChanged( timestamp , 
                                                proximity , 
                                                accuracy );
    }
}
