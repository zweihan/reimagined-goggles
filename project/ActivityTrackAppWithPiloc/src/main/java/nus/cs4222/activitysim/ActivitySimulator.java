package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;
import java.text.*;

import android.os.*;

import nus.cs4222.activitytrackapp.*;

/**
   Simulator to simulate the sensor data collected on the phone.

   This is a single-threaded discrete event simulator. The purpose 
   of the simulator is two-fold:
   1. To easily port the activity detection algorithm code to an 
      Android app (later on).
   2. Take care of parsing the input sensor data files, and output 
      the detection results, in the correct format, for evaluating the 
      accuracy using 'ActivityEval'.

   Code your activity detection algorithm in the class 
    'ActivityDetection.java'. Then, compile and execute the simulator
    on the sensor data you collected.
   While executing the program, you must provide a path to the folder
    containing the sensor data (log files from the sdcard). The 
    folder can contain multiple traces (each trace in a different
    sub-folder). In this case, each trace is simulated independently.

   Compiling:
   $ ant jarify
   Executing:
   $ java -jar ActivitySim.jar <path-to-your-trace-folder>
 */
public class ActivitySimulator {

    /** Gets the file object (path) in the sdcard to Piloc's radio map file. */
    public static File getRadioMapFilePath( String radioMapFilename ) {
        // We assume that the radio map file is in the app's main log directory
        File logDirectory = 
            new File( Environment.getExternalStorageDirectory() , 
                      FileLogger.logDirectoryPath );
        logDirectory.mkdirs();
        return new File( logDirectory , radioMapFilename );
    }

    /** Initialises any variables for the simulation. */
    public static void initSimulation( String logFolder ) 
        throws Exception {

        // Create an instance of the detection algorithm
        detectionAlgo = new ActivityDetection();
        // Clear the activity list
        activityList.clear();
        // Set the previous state to a dummy value
        prevDetectedActivity = UserActivities.Confirm;
        // Open the log file for detected activities
        resultLogger.openLogFile( logFolder , DETECTED_ACTIVITIES_LOG_FILENAME );
        // Initialise the detection algo
        detectionAlgo.initDetection();
    }

    /** De-initialises any variables for the simulation. */
    public static void deinitSimulation() 
        throws Exception {
        try {
            // De-initialise the detection algo
            detectionAlgo.deinitDetection();
            // Clear the activity list
            activityList.clear();
        }
        finally {
            // Close the detected activities log file
            resultLogger.closeLogFile();
            detectionAlgo = null;
        }
    }

    /** 
       Returns the simulator time.

       <p> This method returns the simulator or real time, depending on
       whether this code is running on the simulator or on the Android 
       device. The time is in millisecs since the epoch.

       <p> Note that time does not advance automatically in the 
       simulator, so DO NOT busy wait using this method.

       @return   Real or simulator time in millisec since epoch
     */
    public static long currentTimeMillis() {

        // On a real device, return the real time
        return System.currentTimeMillis();
    }

    /** Logs the specified detected activity with a timestamp. */
    public static void outputDetectedActivity( UserActivities activity ) {

        // Check the args
        if( activity == null ) {
            throw new NullPointerException( "Detected activity object cannot be null" );
        }

        // Log the detected activity (ONLY if it is different from the previous detection)
        if( ! activity.equals( prevDetectedActivity ) ) {
            // Log to a file
            resultLogger.logEvent( activity.toString() );
            // Display last 10 detected activities to the GUI
            activityList.add( sdf.format( new Date( System.currentTimeMillis() ) ) + ": " + 
                              activity.toString() + "\n" );
            // Remove the oldest activity if the list is too large
            if( activityList.size() > MAX_ACTIVITY_LIST_SIZE ) {
                activityList.remove( 0 );
            }
            // Build a concated string of the last 10 activites (recent to oldest)
            StringBuilder sb = new StringBuilder();
            Iterator< String > iterator = activityList.descendingIterator();
            while( iterator.hasNext() ) {
                String detectionResult = iterator.next();
                sb.append( detectionResult );
            }
            // Update the real-time GUI display
            RealTimeDisplay.updateDisplay( ActivitySimulator.DISPLAY_DETECTION_RESULTS , 
                                           sb.toString() );
            // Store the current detected activity
            prevDetectedActivity = activity;
        }
    }

    /** Gets the reference to the activity detection algorithm. */
    public static ActivityDetection getDetectionAlgorithm() {
        return detectionAlgo;
    }

    /** Detection algorithm. */
    private static ActivityDetection detectionAlgo;

    /** Logger to log detected activities by the activity detection algorithm. */
    private static FileLogger resultLogger = new FileLogger();
    /** Previous detected activity logged (to avoid duplicates). */
    private static UserActivities prevDetectedActivity;
    /** Name of the detected activities log file. */
    private static final String DETECTED_ACTIVITIES_LOG_FILENAME = "DetectedActivities.txt";

    /** List of recently detected activities to be displayed on the GUI. */
    private static LinkedList< String > activityList = 
        new LinkedList< String >();
    /** Max size of the detection list. */
    private static final int MAX_ACTIVITY_LIST_SIZE = 10;
    /** To format the UNIX millis time as a human-readable string. */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "h-mm-ssa" );

    /** Name of the real-time GUI display for detection results. */
    public static final String DISPLAY_DETECTION_RESULTS = "ACTIVITY DETECTION";
}
