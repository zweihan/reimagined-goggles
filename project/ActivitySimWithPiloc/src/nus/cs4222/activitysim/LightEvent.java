package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Light sensor event.
 */
public class LightEvent 
    extends SimulatorEvent {

    // Sensor data
    private float light;
    private int accuracy;

    /** Constructor that initialises the sensor data. */
    public LightEvent( long timestamp , 
                       float light , 
                       int accuracy ) {
        // Init the timestamp and sequence number
        super( timestamp );
        // Store the sensor data
        this.light = light;
        this.accuracy = accuracy;
    }

    /** Handles the event. */
    @Override
    public void handleEvent() {
        ActivityDetection detectionAlgo = ActivitySimulator.getDetectionAlgorithm();
        detectionAlgo.onLightSensorChanged( timestamp , 
                                            light , 
                                            accuracy );
    }
}
