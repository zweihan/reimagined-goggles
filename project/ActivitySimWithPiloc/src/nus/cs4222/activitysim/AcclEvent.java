package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Accelerometer sensor event.
 */
public class AcclEvent 
    extends SimulatorEvent {

    // Sensor data
    private float x , y , z;
    private int accuracy;

    /** Constructor that initialises the sensor data. */
    public AcclEvent( long timestamp , 
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
        detectionAlgo.onAcclSensorChanged( timestamp , 
                                           x , 
                                           y , 
                                           z , 
                                           accuracy );
    }
}
