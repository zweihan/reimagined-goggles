package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Barometer sensor event.
 */
public class BarometerEvent 
    extends SimulatorEvent {

    // Sensor data
    private float pressure , altitude;
    private int accuracy;

    /** Constructor that initialises the sensor data. */
    public BarometerEvent( long timestamp , 
                           float pressure , 
                           float altitude , 
                           int accuracy ) {
        // Init the timestamp and sequence number
        super( timestamp );
        // Store the sensor data
        this.pressure = pressure;
        this.altitude = altitude;
        this.accuracy = accuracy;
    }

    /** Handles the event. */
    @Override
    public void handleEvent() {
        ActivityDetection detectionAlgo = ActivitySimulator.getDetectionAlgorithm();
        detectionAlgo.onBarometerSensorChanged( timestamp , 
                                                pressure , 
                                                altitude , 
                                                accuracy );
    }
}
