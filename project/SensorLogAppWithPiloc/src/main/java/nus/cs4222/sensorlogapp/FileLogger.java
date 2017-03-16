package nus.cs4222.sensorlogapp;

import java.io.*;
import java.util.*;
import java.text.*;
import android.util.*;
import android.os.*;

/**
   Responsible for logging to the sdcard.
 */
public class FileLogger {

    /** Helper method to open the log file for writing. */
    public void openLogFile( String logSubFolderName , 
                             String logFileName )
        throws Exception {

        // If already opened, then nothing to do
        if ( logFileOut != null )
            return;

        // Open a file for logging

        // First, check if the sdcard is available for writing
        String externalStorageState = Environment.getExternalStorageState();
        if ( ! externalStorageState.equals ( Environment.MEDIA_MOUNTED ) &&
             ! externalStorageState.equals ( Environment.MEDIA_SHARED ) ) {
            throw new IOException ( "External storage is not mounted" );
        }

        // Second, create the log directory
        File logDirectory = 
            new File( Environment.getExternalStorageDirectory() , 
                      logDirectoryPath );
        logDirectory.mkdirs();
        if ( ! logDirectory.isDirectory() )
            throw new IOException( "Unable to create log directory" );
        // Third, create the log subfolder
        File logSubFolder = 
            new File( logDirectory , 
                      logSubFolderName );
        logSubFolder.mkdirs();
        if ( ! logSubFolder.isDirectory() )
            throw new IOException( "Unable to create log subfolder" );

        // Fourth, create an output stream for the log file (not APPEND MODE!!)
        logFile = new File( logSubFolder , logFileName );
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
            Log.e( TAG , "Unable to close log file" , e );
        }
        finally {
            logFile = null;
            logFileOut = null;
        }
    }

    /** Helper method to log an event. */
    public void logEvent( String event ) {
        try {

            // Get the current timestamp
            StringBuilder sb = new StringBuilder();
            long currentTime = System.currentTimeMillis();
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
            Log.e( TAG , "logEvent(): Exception while logging event" , e );
        }
    }

    /** Full Path of log file. */
    public File logFile = null;
    /** Log file's output stream. */
    public PrintWriter logFileOut = null;

    /** Relative Path of logging directory. */
    private static final String logDirectoryPath = "SensorLogApp";

    /** To format the UNIX millis time as a human-readable string. */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd-h-mm-ssa" );

    /** TAG used for ddms logging. */
    private static final String TAG = "SensorLogApp";
}
