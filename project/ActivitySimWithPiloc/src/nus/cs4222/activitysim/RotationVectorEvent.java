package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Rotation vector sensor event.
 */
public class RotationVectorEvent 
    extends SimulatorEvent {

    // Sensor data
    private float x , y , z , scalar;
    private int accuracy;

    /** Constructor that initialises the sensor data. */
    public RotationVectorEvent( long timestamp , 
                                float x , 
                                float y , 
                                float z , 
                                float scalar , 
                                int accuracy ) {
        // Init the timestamp and sequence number
        super( timestamp );
        // Store the sensor data
        this.x = x;
        this.y = y;
        this.z = z;
        this.scalar = scalar;
        this.accuracy = accuracy;
    }

    /** Handles the event. */
    @Override
    public void handleEvent() {
        ActivityDetection detectionAlgo = ActivitySimulator.getDetectionAlgorithm();
        detectionAlgo.onRotationVectorSensorChanged( timestamp , 
                                                     x , 
                                                     y , 
                                                     z , 
                                                     scalar ,
                                                     accuracy );
    }
}
