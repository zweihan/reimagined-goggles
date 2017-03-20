package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Location sensor event.
 */
public class LocationEvent 
    extends SimulatorEvent {

    // Sensor data
    private String provider;
    private double latitude , longitude;
    private float accuracy;
    private double altitude;
    private float bearing , speed;

    /** Constructor that initialises the sensor data. */
    public LocationEvent( long timestamp , 
                          String provider , 
                          double latitude , 
                          double longitude , 
                          float accuracy , 
                          double altitude , 
                          float bearing , 
                          float speed ) {
        // Init the timestamp and sequence number
        super( timestamp );
        // Store the sensor data
        this.provider = provider;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.altitude = altitude;
        this.bearing = bearing;
        this.speed = speed;
    }

    /** Handles the event. */
    @Override
    public void handleEvent() {
        ActivityDetection detectionAlgo = ActivitySimulator.getDetectionAlgorithm();
        detectionAlgo.onLocationSensorChanged( timestamp , 
                                               provider , 
                                               latitude , 
                                               longitude , 
                                               accuracy , 
                                               altitude , 
                                               bearing , 
                                               speed );
    }
}
