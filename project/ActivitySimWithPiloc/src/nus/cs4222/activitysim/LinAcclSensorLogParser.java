package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/** Parses the linear accelerometer sensor log file. */
public class LinAcclSensorLogParser
    extends SensorLogParser {

    /** Constructor that initialises the parsing. */
    public LinAcclSensorLogParser( File logFile ) 
        throws IOException {
        super( logFile );
    }

    /** {@inheritDoc} */
    @Override
    protected void parseNextLine() 
        throws IOException {

        // Check if there is another log line available
        if( ! in.hasNextLine() ) {
            nextEvent = null;
            return;
        }

        // Parses the line
        String line = in.nextLine();
        String[] parts = line.split( "," );
        if( parts.length != 6 ) {
            throw new IOException( "Invalid line in log file \'" + 
                                   logFile.getName() + "\': " + line );
        }
        long timestamp = Long.parseLong( parts[1] );
        float x = Float.parseFloat( parts[2] );
        float y = Float.parseFloat( parts[3] );
        float z = Float.parseFloat( parts[4] );
        int accuracy = Integer.parseInt( parts[5] );

        // Create the next sensor event object
        nextEvent = new LinAcclEvent( timestamp , x , y , z , accuracy );
    }
}
