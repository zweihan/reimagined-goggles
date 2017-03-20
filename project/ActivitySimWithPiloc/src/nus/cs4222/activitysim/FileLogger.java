package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;
import java.text.*;

/**
   Responsible for logging the ground truth.
 */
public class FileLogger {

    /** Helper method to open the log file for writing. */
    public void openLogFile( File directory , 
                             String logFileName )
        throws Exception {

        // If already opened, then nothing to do
        if ( logFileOut != null )
            return;

        // Open a file for logging

        // Create an output stream for the log file (not APPEND MODE!!)
        logFile = new File( directory , logFileName );
        FileOutputStream fout = new FileOutputStream( logFile ); // , true );
        logFileOut = new PrintWriter( fout );
    }

    /** Helper method that closes the log file. */
    public void closeLogFile() {

        // Close the normal log file
        try {
            if( logFileOut == null )
                return;
            logFileOut.close();
        }
        catch ( Exception e ) {
            // Nothing can be done
        }
        finally {
            logFile = null;
            logFileOut = null;
        }
    }

    /** Helper method to log an event. */
    public void logEvent( String event ) {
        try {

            // Get the current (simulator) timestamp
            StringBuilder sb = new StringBuilder();
            long currentTime = ActivitySimulator.currentTimeMillis();
            sb.append( sdf.format( new Date( currentTime ) ) );
            sb.append( "," );
            sb.append( currentTime );

            // Log to a file (don't forget to flush it!)
            sb.append( "," );
            sb.append( event );
            logFileOut.println( sb.toString() );
            logFileOut.flush();
        }
        catch ( Exception e ) {
            // Log the exception
            e.printStackTrace();
        }
    }

    /** Full Path of log file. */
    public File logFile = null;
    /** Log file's output stream. */
    public PrintWriter logFileOut = null;

    /** To format the UNIX millis time as a human-readable string. */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd-h-mm-ssa" );
}
