package nus.cs4222.activityeval;

import java.io.*;
import java.util.*;

/**
   Evaluates the accuracy (and latency) of the activity detection results.

   This uses the ground truth log and activity detection log files to 
    check the accuracy and latency of the activity detection algorithm.
   While executing the program, you must provide a path to the folder
    containing the sensor data and activity detection log files. The 
    folder can contain multiple traces (each trace in a different
    sub-folder). In this case, the accuracy results are evaluated 
    individually, and also for all the traces together.
   You can specify an optional flag '--novehiclestate' to indicate that
    the three vehicle states {bus, car, train} should NOT be mapped
    to the general vehicle state. In this case, the accuracy is 
    reported for all 3 vehicle states separately.

   Compiling:
   $ ant jarify
   Executing:
   $ java -jar ActivityEval.jar [--novehiclestate] <path-to-the-folder-containing-trace-subfolders>
 */
public class ActivityEvaluator {

    /** Main starting point of the evaluator. */
    public static void main( String[] args ) {
        try {

            // Check the arguments
            String traceArg = null;
            if( args.length == 1 ) {
                traceArg = args[0];
            }
            else if( args.length == 2 ) {
                if( ! args[0].equals( "--novehiclestate" ) ) {
                    System.err.println( "Usage: java -jar ActivityEval.jar [--novehiclestate] <path-to-the-folder-containing-trace-subfolders>" );
                    return;
                }
                needToMapToVehicle = false;
                traceArg = args[1];
            }
            else {
                System.err.println( "Usage: java -jar ActivityEval.jar [--novehiclestate] <path-to-the-folder-containing-trace-subfolders>" );
                return;
            }
            File traceFolder = new File( traceArg );
            if( ! traceFolder.isDirectory() ) {
                System.err.println( "\'" + traceFolder.getPath() + "\' is not a valid directory" );
                return;
            }

            // Make a list of trace folders
            checkForTraceFolders( traceFolder );
            // Parse all traces first
            parseAllTraces();

            // Evaluate all traces (including individual accuracy/latency output)
            evaluateAllTraces();
            // Output the accuracy and latency for all traces together
            if( traceFolderList.size() > 1 ) {
                outputAggregateEvaluation();
            }
        }
        catch( Exception e ) {
            e.printStackTrace();
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

        // Check if the folder has the required log files
        File baroFile = new File( folder , BAROMETER_LOG_FILENAME );
        File groundTruthFile = new File( folder , GROUND_TRUTH_LOG_FILENAME );
        File detectedActivitiesFile = new File( folder , DETECTED_ACTIVITIES_LOG_FILENAME );

        if( ! baroFile.isFile() ) {
            return false;
        }
        else if( ! groundTruthFile.isFile() ) {
            return false;
        }
        else if( ! detectedActivitiesFile.isFile() ) {
            System.out.println( "Warning: Ignoring folder \'" + folder + 
                                "\' which has the ground truth and sensor log files, but no \'" + 
                                DETECTED_ACTIVITIES_LOG_FILENAME + "\' file. Run the simulator first." );
            return false;
        }

        return true;
    }

    /** Parses all traces first. */
    private static void parseAllTraces() 
        throws Exception {

        // Check if no traces were found
        if( traceFolderList.isEmpty() ) {
            throw new Exception( "No traces found! Make sure that you specified the correct path to the folder with traces." );
        }

        // Parse each trace one by one
        for( File traceFolder : traceFolderList ) {
            parseTrace( traceFolder );
        }

        // Create IDs for each state (this includes bonus states added by students
        createStateIDs();
    }

    /** Parse a trace and store its details. */
    private static void parseTrace( File traceFolder ) 
        throws Exception {

        // Create an empty trace details object for this trace
        TraceDetails trace = new TraceDetails();
        traceToDetailsMap.put( traceFolder.getPath() , trace );

        // Parse the log files
        parseGroundTruthFile( traceFolder , trace );
        parseBarometerFile( traceFolder , trace );
        parseDetectedActivityFile( traceFolder , trace );
    }

    /** Parses the ground truth file. */
    private static void parseGroundTruthFile( File traceFolder , 
                                              TraceDetails trace ) 
        throws Exception {

        // Initialise the start timestamp to a dummy value
        trace.startTimestamp = -1L;
        // Previous state (to remove duplicate lines in the log)
        String prevState = "DummyPrevState";
        trace.groundTruthList = new ArrayList< TimestampedState >();

        // Parse the ground truth
        File groundTruthFile = new File( traceFolder , GROUND_TRUTH_LOG_FILENAME );
        Scanner in = null;
        try {
            in = new Scanner( groundTruthFile );
            while( in.hasNextLine() ) {
                String line = in.nextLine();
                if( line.trim().isEmpty() ) {
                    continue;
                }
                String[] parts = line.split( "," );
                if( parts.length != 5 ) {
                    throw new Exception( "Invalid line \'" + line + "\' in file \'" + 
                                           groundTruthFile.getPath() + "\'" );
                }
                long timestamp = Long.parseLong( parts[3] );
                String state = mapToVehicle( parts[4] );
                if( ! state.equals( prevState ) ) {
                    trace.groundTruthList.add( new TimestampedState( timestamp , state ) );
                    stateSet.add( state );
                    prevState = state;
                }
                // Check if the ground truth has an "INCORRECT" state
                if( state.equals( "INCORRECT" ) ) {
                    throw new Exception( "Error: Ground truth file \'" + groundTruthFile.getPath() + 
                                         "\' has an INCORRECT state logged. Please manually correct the ground truth." );
                }
                if( state.equals( "OTHER" ) ) {
                    throw new Exception( "Error: Ground truth file \'" + groundTruthFile.getPath() + 
                                         "\' has an \'OTHER\' (unknown) state logged. Please manually correct the ground truth" + 
                                         " and give the state a proper name." );
                }
                // Set the start timestamp
                if( trace.startTimestamp < 0 ) {
                    trace.startTimestamp = timestamp;
                }
            }

            // Check if the log file is empty
            if( trace.groundTruthList.isEmpty() )  {
                throw new Exception( "Error: Ground truth file \'" + 
                                     groundTruthFile.getPath() + "\' is empty" );
            }
            // Sanity check: start timestamp must be positive
            else if( trace.startTimestamp < 0 ) {
                throw new Exception( "Error: Negative start timestamp found in ground truth file \'" + 
                                     groundTruthFile.getPath() + "\'" );
            }
        }
        finally {
            try {
                in.close();
            }
            catch( Exception e ) {
                // Nothing can be done
            }
        }
    }

    /** Parses the detected activities file. */
    private static void parseDetectedActivityFile( File traceFolder , 
                                                   TraceDetails trace ) 
        throws Exception {

        // Previous state (to remove duplicate lines in the log)
        String prevState = "DummyPrevState";
        trace.detectedActivitiesList = new ArrayList< TimestampedState >();

        // Parse the detected activities
        File detectedActivitiesFile = new File( traceFolder , DETECTED_ACTIVITIES_LOG_FILENAME );
        Scanner in = null;
        try {
            in = new Scanner( detectedActivitiesFile );
            while( in.hasNextLine() ) {
                String line = in.nextLine();
                if( line.trim().isEmpty() ) {
                    continue;
                }
                String[] parts = line.split( "," );
                if( parts.length != 3 ) {
                    throw new Exception( "Invalid line \'" + line + "\' in file \'" + 
                                           detectedActivitiesFile.getPath() + "\'" );
                }
                long timestamp = Long.parseLong( parts[1] );
                String state = mapToVehicle( parts[2] );
                if( ! state.equals( prevState ) ) {
                    trace.detectedActivitiesList.add( new TimestampedState( timestamp , state ) );
                    stateSet.add( state );
                    prevState = state;
                }
                // Check if there is an "INCORRECT" state
                if( state.equals( "INCORRECT" ) ) {
                    throw new Exception( "Error: Detected activities file \'" + detectedActivitiesFile.getPath() + 
                                         "\' has an INCORRECT state logged. Modify your algorithm to not output \'INCORRECT\'." );
                }
            }

            // Check if the log file is empty
            if( trace.detectedActivitiesList.isEmpty() )  {
                System.out.println( "Warning: Detected activities file \'" + 
                                    detectedActivitiesFile.getPath() + "\' is empty" );
            }
        }
        finally {
            try {
                in.close();
            }
            catch( Exception e ) {
                // Nothing can be done
            }
        }
    }

    /** Parses the barometer sensor file. */
    private static void parseBarometerFile( File traceFolder , 
                                            TraceDetails trace ) 
        throws Exception {

        // Initialise the end timestamp to a dummy value
        trace.endTimestamp = -1L;

        // Parse the barometer data
        File baroFile = new File( traceFolder , BAROMETER_LOG_FILENAME );
        Scanner in = null;
        try {
            in = new Scanner( baroFile );
            String line = null;
            while( in.hasNextLine() ) {
                String readLine = in.nextLine();
                if( readLine.trim().isEmpty() ) {
                    continue;
                }
                line = readLine;
            }

            // Sanity check: The barometer file cannot be empty
            if( line == null )  {
                throw new Exception( "Error: Barometer log file \'" + 
                                     baroFile.getPath() + "\' is empty" );
            }

            // Parse the last line in the baro file to get the end timestamp
            String[] parts = line.split( "," );
            if( parts.length != 5 ) {
                throw new Exception( "Invalid line \'" + line + "\' in file \'" + 
                                       baroFile.getPath() + "\'" );
            }
            trace.endTimestamp = Long.parseLong( parts[1] );

            // Sanity check: The end timestamp must be positive
            if( trace.endTimestamp < 0 )  {
                throw new Exception( "Error: Negative end timestamp found in barometer log file \'" + 
                                     baroFile.getPath() + "\'" );
            }
            // Sanity check: The start timestamp must be earlier than the end timestamp
            else if( trace.startTimestamp > trace.endTimestamp ) {
                throw new Exception( "Error: First timestamp in ground truth file is later than last timestamp in baro log file" );
            }
        }
        finally {
            try {
                in.close();
            }
            catch( Exception e ) {
                // Nothing can be done
            }
        }
    }

    /** Creates state IDs for statenames found in the traces. */
    private static void createStateIDs() {

        // Give state IDs first to IDLE_INDOOR, IDLE_OUTDOOR, WALKING, VEHICLE (or BUS/TRAIN/CAR), OTHER
        int id = -1;
        stateToIDMap.put( "IDLE_INDOOR" , ++id );
        stateToIDMap.put( "IDLE_OUTDOOR" , ++id );
        stateToIDMap.put( "IDLE_COM1" , ++id );
        stateToIDMap.put( "WALKING" , ++id );
        if( needToMapToVehicle ) {
            stateToIDMap.put( "VEHICLE" , ++id );
        }
        else {
            stateToIDMap.put( "BUS" , ++id );
            stateToIDMap.put( "TRAIN" , ++id );
            stateToIDMap.put( "CAR" , ++id );
        }

        // Add any extra bonus states
        for( String state : stateSet ) {

            // If it is not already given an ID, then add it
            if( ! stateToIDMap.containsKey( state ) && 
                ! state.equals( "OTHER" ) ) {
                stateToIDMap.put( state , ++id );
            }
        }

        // Last is the OTHER/UNKNOWN state
        stateToIDMap.put( "OTHER" , ++id );

        // Store the number of states
        numStates = id + 1;
    }

    /** Evaluates all traces one by one. */
    private static void evaluateAllTraces() {

        // Evaluate each trace one by one
        for( File traceFolder : traceFolderList ) {
            TraceDetails trace = traceToDetailsMap.get( traceFolder.getPath() );
            evaluateTrace( traceFolder , trace );
            outputTraceEvaluation( trace );
        }
    }

    /** Evaluates the trace. */
    private static void evaluateTrace( File traceFolder , 
                                       TraceDetails trace ) {

        // Output details of the trace
        System.out.println();
        System.out.println( "** Evaluating trace in folder \'" + traceFolder.getPath() + "\' :" );
        double duration = ( trace.endTimestamp - trace.startTimestamp ) / 1000.0 / 3600.0;
        System.out.printf( "* DURATION of the trace: %.2f hrs (%.2f min)" , duration , duration * 60.0 );
        System.out.println();

        // Evaluate the accuracy
        evaluateAccuracy( trace );
        // Evaluate the Latency
        evaluateLatency( trace );
    }

    /** Evaluate accuracy of a trace. */
    private static void evaluateAccuracy( TraceDetails trace ) {

        // Create a confusion matrix
        trace.confusionMatrix = new int[ numStates ][ numStates ];

        // Loop from the start of the trace to the end, check the accuracy every second
        String correctState = trace.groundTruthList.get( 0 ).state;
        String detectedState = "OTHER";
        int correctStateId = stateToIDMap.get( correctState );
        int detectedStateId = stateToIDMap.get( detectedState );
        int groundIndex = -1;
        int detectedIndex = -1;
        for( long time = trace.startTimestamp ; time <= trace.endTimestamp ; time += ACCURACY_TIME ) {

            // Get the ground truth state
            while( groundIndex + 1 < trace.groundTruthList.size() && 
                   time >= trace.groundTruthList.get( groundIndex + 1 ).timestamp ) {
                ++groundIndex;
                correctState = trace.groundTruthList.get( groundIndex ).state;
                correctStateId = stateToIDMap.get( correctState );
            }
            // Get the detected state
            // Note: The detected list could be empty
            while( detectedIndex + 1 < trace.detectedActivitiesList.size() && 
                   time >= trace.detectedActivitiesList.get( detectedIndex + 1 ).timestamp ) {
                ++detectedIndex;
                detectedState = trace.detectedActivitiesList.get( detectedIndex ).state;
                detectedStateId = stateToIDMap.get( detectedState );
            }

            // Update the confusion matrix
            trace.confusionMatrix[ correctStateId ][ detectedStateId ] += 1;
        }
    }

    /** Evaluate latency of a trace. */
    private static void evaluateLatency( TraceDetails trace ) {

        /*
           To calculate latency, we do the following:
            1. For each ground truth transition, we check if the detection
               was missed or detected. The transition is missed if it is 
               not detected BEFORE THE NEXT ground truth transition.
            2. If not missed, the earliest detection is considered for
               the latency calculation.
         */

        // Create a latency array (millis)
        double[] latencies = new double[ numStates ];
        // Sum of latencies for each state (millis)
        long[] sum = new long[ numStates ];
        // Total ground truth transitions for each state
        int[] total = new int[ numStates ];
        // Number of missed detections of transitions
        int[] missed = new int[ numStates ];

        // Go over the ground truth states one by one
        int j = 0;
        for( int i = 0 ; i < trace.groundTruthList.size() ; ++i ) {

            // Get the state name and timestamp
            String state = trace.groundTruthList.get( i ).state;
            int stateId = stateToIDMap.get( state );
            long begin = trace.groundTruthList.get( i ).timestamp;
            long end = Long.MAX_VALUE;
            if( i + 1 < trace.groundTruthList.size() ) {
                end = trace.groundTruthList.get( i + 1 ).timestamp;
            }

            // Get the earliest detection of this state
            boolean isDetected = false;
            long latency = 0L;
            for( ; j < trace.detectedActivitiesList.size() ; ++j ) {
                long begin2 = trace.detectedActivitiesList.get( j ).timestamp;
                String state2 = trace.detectedActivitiesList.get( j ).state;
                int stateId2 = stateToIDMap.get( state2 );
                long end2 = Long.MAX_VALUE;
                if( j + 1 > trace.detectedActivitiesList.size() ) {
                    end2 = trace.detectedActivitiesList.get( j + 1 ).timestamp;
                }

                // Come out of the loop if there is no hope
                if( begin2 >= end ) {
                    // Go back one detection, since the for the next ground truth,
                    //  the detection may be early
                    j -= 2;  // One to go back, one to negate ++j
                    if( j < 0 ) {  // Boundary case: j is at the beginning
                        j = 0;
                    }
                    break;
                }
                // If the state detected is not the state we want, then continue
                if( stateId != stateId2 ) {
                    continue;
                }

                // Case 1: The detection is early
                if( begin >= begin2 && begin < end2 ) {
                    isDetected = true;
                    latency = 0L;
                    break;
                }
                // Case 2: The detection is late
                if( begin2 >= begin && begin2 < end ) {
                    isDetected = true;
                    latency = ( begin2 - begin );
                    break;
                }
            }

            // Store the latency for this state
            total[ stateId ] += 1;
            if( ! isDetected ) {
                missed[ stateId ] += 1;
            }
            else {
                sum[ stateId ] += latency;
            }
        }

        // Calculate the latencies
        for( int i = 0 ; i < numStates ; ++i ) {
            // NOTE: The average latency is over the ones NOT missed
            if( total[i] - missed[i] == 0 ) {
                latencies[i] = 0.0;
            }
            else {
                latencies[i] = (double) sum[i] / ( total[i] - missed[i] );
            }
        }
        trace.latencies = latencies;
        trace.total = total;
        trace.missed = missed;
    }

    /** Output the accuracy (confusion matrix) and latency evaluation for this trace. */
    private static void outputTraceEvaluation( TraceDetails trace ) {

        // Output the confusion matrix
        outputConfusionMatrix( trace.confusionMatrix );
        // Output the latencies
        outputLatencies( trace.latencies , 
                         trace.total , 
                         trace.missed );
    }

    /** Outputs a confusion matrix. */
    private static void outputConfusionMatrix( int[][] confusionMatrix ) {

        // Output the column names
        String formatString = "%12s" , formatDouble = "%11.2f%%";
        System.out.println( "* CONFUSION MATRIX (Rows are ground truth, columns are detected states):" );
        System.out.printf( formatString , "Truth/Detect" );
        Set< String > keySet = stateToIDMap.keySet();
        for( String state : keySet ) {
            System.out.printf( " " + formatString , state );
        }
        System.out.println();

        // Output the rows
        int[] total = new int[ numStates ];
        for( int i = 0 ; i < numStates ; ++i ) {
            for( int j = 0 ; j < numStates ; ++j ) {
                total[i] += confusionMatrix[i][j];
            }
        }
        for( String state : keySet ) {
            // Ignore the OTHER (unknown) state
            if( state.equals( "OTHER" ) ) {
                continue;
            }
            int i = stateToIDMap.get( state );
            System.out.printf( formatString , state );
            for( int j = 0 ; j < numStates ; ++j ) {
                double percent = 0.0;
                if( total[i] != 0 ) {
                    percent = (double) confusionMatrix[i][j] * 100.0 / total[i];
                }
                System.out.printf( " " + formatDouble , percent );
            }
            System.out.println();
        }

        // Output accuracies
        System.out.println( "* ACCURACY:" );
        int overallTotal = 0 , overallCorrect = 0;
        for( String state : keySet ) {
            // Ignore the OTHER (unknown) state
            if( state.equals( "OTHER" ) ) {
                continue;
            }
            int i = stateToIDMap.get( state );
            overallTotal += total[i];
            overallCorrect += confusionMatrix[i][i];
            double percent = 0.0;
            if( total[i] != 0 ) {
                percent = (double) confusionMatrix[i][i] * 100.0 / total[i];
            }
            double duration = (double) total[i] * ACCURACY_TIME / 1000.0 / 3600.0;
            System.out.printf( "\t%s (%.2f min [%.2f hrs]): %.2f%%" , state , duration * 60.0 , duration , percent );
            System.out.println();
        }
        // Overall accuracy too
        double percent = 0.0;
        if( overallTotal != 0 ) {
            percent = (double) overallCorrect * 100.0 / overallTotal;
        }
        System.out.printf( "\tOVERALL Accuracy: %.2f%%" , percent );
        System.out.println();
    }

    /** Outputs latencies. */
    private static void outputLatencies( double[] latencies , 
                                         int[] total , 
                                         int[] missed ) {

        // Output the latencies for each state
        System.out.println( "* LATENCY:" );
        Set< String > keySet = stateToIDMap.keySet();
        for( String state : keySet ) {
            // Ignore the OTHER (unknown) state
            if( state.equals( "OTHER" ) ) {
                continue;
            }
            int i = stateToIDMap.get( state );
            double latency = latencies[i] / 1000.0 / 60.0;
            System.out.printf( "\t%s: %.2f min (%.2f sec) [missed %d out of %d]" , 
                               state , latency , latency * 60.0 , missed[i] , total[i] );
            System.out.println();
        }
    }

    /** Outputs the accuracy (confusion matrix) and latency evaluation for all traces together. */
    private static void outputAggregateEvaluation() {

        // Need to calculate the aggregated confusion matrix and latencies
        int[][] confusionMatrix = new int[ numStates ][ numStates ];
        double[] latencies = new double[ numStates ];
        double[] sum = new double[ numStates ];
        int[] total = new int[ numStates ];
        int[] missed = new int[ numStates ];
        double traceDuration = 0.0;
        System.out.println();
        System.out.println( "** AGGREGATE EVALUATION for all traces together: " );

        // Go over each trace one by one
        for( File traceFolder : traceFolderList ) {
            TraceDetails trace = traceToDetailsMap.get( traceFolder.getPath() );
            // Total the duration
            double duration = ( trace.endTimestamp - trace.startTimestamp ) / 1000.0 / 3600.0;
            traceDuration += duration;
            // Total the confusion matrix
            for( int i = 0 ; i < numStates ; ++i ) {
                for( int j = 0 ; j < numStates ; ++j ) {
                    confusionMatrix[i][j] += trace.confusionMatrix[i][j];
                }
            }
            // Total the latencies
            for( int i = 0 ; i < numStates ; ++i ) {
                total[i] += trace.total[i];
                missed[i] += trace.missed[i];
                sum[i] += ( trace.latencies[i] * ( trace.total[i] - trace.missed[i] ) );
            }
        }
        // Calculate the aggregated latencies
        for( int i = 0 ; i < numStates ; ++i ) {
            // NOTE: The average latency is over the ones NOT missed
            if( total[i] - missed[i] == 0 ) {
                latencies[i] = 0.0;
            }
            else {
                latencies[i] = (double) sum[i] / ( total[i] - missed[i] );
            }
        }

        // Output the duration
        System.out.printf( "* TOTAL DURATION of all traces: %.2f hrs (%.2f min)" , 
                           traceDuration , traceDuration * 60.0 );
        System.out.println();
        // Output the confusion matrix
        outputConfusionMatrix( confusionMatrix );
        // Output the latencies
        outputLatencies( latencies , 
                         total , 
                         missed );
    }

    /** Maps the {bus, car, train} state to the vehicle state. */
    private static String mapToVehicle( String state ) {
        if( needToMapToVehicle ) {
            if( state.equals( "BUS" ) || 
                state.equals( "TRAIN" ) || 
                state.equals( "CAR" ) ) {
                return "VEHICLE";
            }
        }
        return state;
    }

    /** List of traces. */
    private static List< File > traceFolderList = 
        new ArrayList< File >();
    /** Map of a trace {key} ==> trace details {value}. */
    private static Map< String , TraceDetails > traceToDetailsMap = 
        new LinkedHashMap< String , TraceDetails >();

    /** Set of statenames (this can include bonus states by students). */
    private static Set< String > stateSet = 
        new HashSet< String >();
    /** Map of statename {key} ==> state ID (index into the confusion matrix and latency array). */
    private static Map< String , Integer > stateToIDMap = 
        new LinkedHashMap< String , Integer >();
    /** Number of states (including OTHER/UNKNOWN). */
    private static int numStates;
    /** Flag to indicate whether the {car, bus, train} should be mapped to a 'vehicle' state. */
    private static boolean needToMapToVehicle = true;

    /** Tuple of (timestamp, state) for ground truth and detected activity. */
    private static class TimestampedState {
        public long timestamp;
        public String state;
        public TimestampedState( long timestamp , 
                                 String state ) {
            this.timestamp = timestamp;
            this.state = state;
        }
    }

    /** Stores details about a trace. */
    private static class TraceDetails {

        /** Starting time of the trace (first timestamp in ground truth). */
        public long startTimestamp;
        /** Ending time of the trace (last timestamp in the barometer file). */
        public long endTimestamp;

        /** List of ground truth states. */
        public ArrayList< TimestampedState > groundTruthList;
        /** List of detected activity states. */
        public ArrayList< TimestampedState > detectedActivitiesList;

        /** Confusion matrix (for accuracy). */
        public int[][] confusionMatrix;
        /** Latencies. */
        public double[] latencies;
        /** Total ground truth transitions for each state. */
        public int[] total;
        /** Number of missed detections of transitions. */
        public int[] missed;
    }

    /** Time advance for calculating accuracy (millis). */
    private static final long ACCURACY_TIME = 1000L;

    // Log filenames
    private static final String BAROMETER_LOG_FILENAME = "Baro.txt";
    private static final String DETECTED_ACTIVITIES_LOG_FILENAME = "DetectedActivities.txt";
    private static final String GROUND_TRUTH_LOG_FILENAME = "GroundTruth.txt";
}
