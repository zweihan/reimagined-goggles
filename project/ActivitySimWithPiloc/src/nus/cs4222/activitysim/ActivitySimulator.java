package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

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

    /** Main starting point of the simulator. */
    public static void main( String[] args ) {
        try {

            // Check the arguments
            if( args.length != 1 ) {
                System.err.println( "Usage: java -jar ActivitySim.jar <path-to-your-trace-folder>" );
                return;
            }
            File folder = new File( args[0] );
            if( ! folder.isDirectory() ) {
                throw new IllegalArgumentException( "Invalid trace folder: \'" + 
                                                    folder.getPath() + "\' is not a valid directory" );
            }

            // Make a list of trace folders
            checkForTraceFolders( folder );
            // Check if no traces were found
            if( traceFolderList.isEmpty() ) {
                throw new Exception( "No traces found! Make sure that you specified the correct path to the folder with traces." );
            }

            // Simulate each trace one by one independently
            for( File traceFolder : traceFolderList ) {

                // Output the trace that is currently being simulated
                System.out.println();
                System.out.println( "Simulating the trace in folder \'" + traceFolder.getPath() + "\'..." );

                try {
                    // Initialise any variables for the simulation
                    initSimulation();
                    // Create all the parsers for the sensor log files
                    initParsers( traceFolder );
                    // Open the log file for detected activities
                    resultLogger.openLogFile( traceFolder , DETECTED_ACTIVITIES_LOG_FILENAME );

                    // Populate the event data structure with the first few sensor events
                    populateFewEvents();
                    // Initialise the simulator time
                    simulatorTime = 0L;
                    if( ! eventSet.isEmpty() ) {
                        simulatorTime = eventSet.first().event.timestamp;
                    }
                    // Initialise the detection algo
                    detectionAlgo.initDetection();
                    // Run the simulator
                    runSimulator();
                    // De-initialise the detection algo
                    detectionAlgo.deinitDetection();
                }
                finally {
                    // Close the detected activities log file
                    resultLogger.closeLogFile();
                }
            }

            // Output the traces that were simulated
            System.out.println();
            System.out.println( "List of traces that were simulated are below." );
            System.out.println( "Check the \'DetectedActivities.txt\' file in each trace folder to check the output of your algorithm." );
            System.out.println( "Run ActivityEval to calculate the aggregated accuracy and latency over all the traces together." );
            for( File traceFolder : traceFolderList ) {
                System.out.println( "\t" + traceFolder.getPath() );
            }
        }
        catch( Exception e ) {
            e.printStackTrace();
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

        // In the simulator, we return a 'fake' time
        return simulatorTime;
    }

    /** Logs the specified detected activity with a timestamp. */
    public static void outputDetectedActivity( UserActivities activity ) {

        // Check the args
        if( activity == null ) {
            throw new NullPointerException( "Detected activity object cannot be null" );
        }

        // Log the detected activity (ONLY if it is different from the previous detection)
        if( ! activity.equals( prevDetectedActivity ) ) {
            resultLogger.logEvent( activity.toString() );
            prevDetectedActivity = activity;
        }
    }

    /** Makes a list of all traces in this folder. */
    private static void checkForTraceFolders( File folder ) {

        // Scan for sub-folders
        File[] files = folder.listFiles();
        for( File file : files ) {
            if( file.isDirectory() ) {
                checkForTraceFolders( file );
            }
        }

        // Check if this folder has a trace
        if( checkIfTraceFolder( folder ) ) {
            traceFolderList.add( folder );
            System.out.println( "Found a data collection trace in folder \'" + folder.getPath() + "\'" );
        }
    }

    /** Checks if this is a folder containing a valid trace. */
    private static boolean checkIfTraceFolder( File folder ) {

        // Check if the folder has all the sensor log files
        String[] logFilenameList = 
            new String[] { LOCATION_LOG_FILENAME , 
                           ACCELEROMETER_LOG_FILENAME , 
                           GRAVITY_LOG_FILENAME , 
                           LINEAR_ACCELEROMETER_LOG_FILENAME , 
                           MAGNETIC_LOG_FILENAME , 
                           GYROSCOPE_LOG_FILENAME , 
                           ROTATION_VECTOR_LOG_FILENAME , 
                           BAROMETER_LOG_FILENAME , 
                           LIGHT_LOG_FILENAME , 
                           PROXIMITY_LOG_FILENAME , 
                           WIFI_LOG_FILENAME };
                           //GROUND_TRUTH_LOG_FILENAME };
        for( String logFilename : logFilenameList ) {
            File logFile = new File( folder , logFilename );
            if( ! logFile.isFile() ) {
                return false;
            }
        }

        return true;
    }

    /** Initialises any variables for the simulation. */
    private static void initSimulation() {
        detectionAlgo = new ActivityDetection();
        prevDetectedActivity = UserActivities.Confirm;
        parserList.clear();
        eventSet.clear();
        numSensorEvents = 0;
    }

    /** Initialises the parsers for all the sensor log files. */
    private static void initParsers( File traceFolder ) 
        throws IOException {

        // Add parsers for each sensor
        parserList.add( new LocationSensorLogParser( new File( traceFolder , LOCATION_LOG_FILENAME ) ) );
        parserList.add( new AcclSensorLogParser( new File( traceFolder , ACCELEROMETER_LOG_FILENAME ) ) );
        parserList.add( new GravitySensorLogParser( new File( traceFolder , GRAVITY_LOG_FILENAME ) ) );
        parserList.add( new LinAcclSensorLogParser( new File( traceFolder , LINEAR_ACCELEROMETER_LOG_FILENAME ) ) );
        parserList.add( new MagneticSensorLogParser( new File( traceFolder , MAGNETIC_LOG_FILENAME ) ) );
        parserList.add( new GyroscopeSensorLogParser( new File( traceFolder , GYROSCOPE_LOG_FILENAME ) ) );
        parserList.add( new RotationVectorSensorLogParser( new File( traceFolder , ROTATION_VECTOR_LOG_FILENAME ) ) );
        parserList.add( new BarometerSensorLogParser( new File( traceFolder , BAROMETER_LOG_FILENAME ) ) );
        parserList.add( new LightSensorLogParser( new File( traceFolder , LIGHT_LOG_FILENAME ) ) );
        parserList.add( new ProximitySensorLogParser( new File( traceFolder , PROXIMITY_LOG_FILENAME ) ) );
        parserList.add( new WiFiSensorLogParser( new File( traceFolder , WIFI_LOG_FILENAME ) ) );
    }

    /** Populates the event data structure with the first sensor log events. */
    private static void populateFewEvents() 
        throws IOException {

        // For each parser, add the first sensor log event to the event data structure
        for( SensorLogParser parser : parserList ) {

            // Get the first sensor log event
            if( parser.hasNextEvent() ) {
                SimulatorEvent event = parser.getNextEvent();
                EventTuple eventTuple = new EventTuple( event , 
                                                        parser );
                // Add it to the event data structure
                eventSet.add( eventTuple );

                // UPDATE: Increase the number of sensor events
                ++numSensorEvents;
            }
        }
    }

    /** Runs the simulator by consuming events from the event data structure. */
    private static void runSimulator() 
        throws IOException {

        // Consume each simulator event one by one until there are no more events.
        // The events are processed in the order of timestamps.
        //while( ! eventSet.isEmpty() ) {
        // UPDATE: Run the simulator until there are no more sensor events.
        //  This can happen if the event set has only timer events, or is empty.
        while( numSensorEvents > 0 ) { 

            // Get the earliest event
            EventTuple eventTuple = eventSet.first();
            eventSet.remove( eventTuple );
            SimulatorEvent event = eventTuple.event;
            SensorLogParser parser = eventTuple.parser;

            // UPDATE: If it is a sensor log event, then
            //  decrease the number of sensor events
            if( parser != null ) {
                --numSensorEvents;

                // For a sensor log event, add the next event to the event data structure
                if( parser.hasNextEvent() ) {
                    SimulatorEvent nextEvent = parser.getNextEvent();
                    EventTuple nextEventTuple = new EventTuple( nextEvent , 
                                                                parser );
                    eventSet.add( nextEventTuple );

                    // UPDATE: Increase the number of sensor events
                    ++numSensorEvents;
                }
            }

            // Update the simulator time
            simulatorTime = event.getTimestamp();
            // Handle the event
            // NOTE: The event data structure may be modified in this call
            event.handleEvent();
        }
    }

    /** Gets the reference to the activity detection algorithm. */
    public static ActivityDetection getDetectionAlgorithm() {
        return detectionAlgo;
    }

    /** Adds a timer event. */
    public static void addTimerEvent( TimerEvent event ) {
        eventSet.add( new EventTuple( event , null ) );
    }

    /** Removes a timer event. */
    public static void removeTimerEvent( TimerEvent event ) {
        eventSet.remove( new EventTuple( event , null ) );
    }

    /** Event data structure (chosen to have log(n) insertion and deletion). */
    private static TreeSet< EventTuple > eventSet = 
        new TreeSet< EventTuple >();
    /** List of sensor log file parsers. */
    private static List< SensorLogParser > parserList = 
        new ArrayList< SensorLogParser >();
    // UPDATE: Keep track of the number of sensor events
    /** Number of sensor events in the event set. */
    private static int numSensorEvents;

    /** Current simulator time (millisecs since epoch). */
    private static long simulatorTime;

    /** Detection algorithm. */
    private static ActivityDetection detectionAlgo;
    /** List of traces. */
    private static List< File > traceFolderList = 
        new ArrayList< File >();

    /** Tuple of (simulator event, sensor log parser). */
    private static class EventTuple 
        implements Comparable< EventTuple > {
        public SimulatorEvent event;
        public SensorLogParser parser;
        public EventTuple( SimulatorEvent event , 
                           SensorLogParser parser ) {
            this.event = event;
            this.parser = parser;
        }
        public int hashCode() {
            return event.hashCode();
        }
        public boolean equals( Object otherObject ) {
            EventTuple other = (EventTuple) otherObject;
            return this.event.equals( other.event );
        }
        public int compareTo( EventTuple other ) {
            return this.event.compareTo( other.event );
        }
    }

    /** Logger to log detected activities by the activity detection algorithm. */
    private static FileLogger resultLogger = new FileLogger();
    /** Previous detected activity logged (to avoid duplicates). */
    private static UserActivities prevDetectedActivity;
    /** Name of the detected activities log file. */
    private static final String DETECTED_ACTIVITIES_LOG_FILENAME = "DetectedActivities.txt";

    // Sensor log filenames
    private static final String LOCATION_LOG_FILENAME = "Loc.txt";
    private static final String ACCELEROMETER_LOG_FILENAME = "Accl.txt";
    private static final String GRAVITY_LOG_FILENAME = "Gravity.txt";
    private static final String LINEAR_ACCELEROMETER_LOG_FILENAME = "LinAccl.txt";
    private static final String MAGNETIC_LOG_FILENAME = "Mag.txt";
    private static final String GYROSCOPE_LOG_FILENAME = "Gyro.txt";
    private static final String ROTATION_VECTOR_LOG_FILENAME = "RotVec.txt";
    private static final String BAROMETER_LOG_FILENAME = "Baro.txt";
    private static final String LIGHT_LOG_FILENAME = "Light.txt";
    private static final String PROXIMITY_LOG_FILENAME = "Proximity.txt";
    private static final String WIFI_LOG_FILENAME = "WiFi.txt";
    //private static final String GROUND_TRUTH_LOG_FILENAME = "GroundTruth.txt";
}
