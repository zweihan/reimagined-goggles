package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Gyroscope sensor event.
 */
public class GyroscopeEvent 
    extends SimulatorEvent {

    // Sensor data
    private float x , y , z;
    private int accuracy;

    /** Constructor that initialises the sensor data. */
    public GyroscopeEvent( long timestamp , 
                           float x , 
                           float y , 
                           float z , 
                           int accuracy ) {
        // Init the timestamp and sequence number
        super( timestamp );
        // Store the sensor data
        this.x = x;
        this.y = y;
        this.z = z;
        this.accuracy = accuracy;
    }

    /** Handles the event. */
    @Override
    public void handleEvent() {
        ActivityDetection detectionAlgo = ActivitySimulator.getDetectionAlgorithm();
        detectionAlgo.onGyroscopeSensorChanged( timestamp , 
                                                x , 
                                                y , 
                                                z , 
                                                accuracy );
    }
}
