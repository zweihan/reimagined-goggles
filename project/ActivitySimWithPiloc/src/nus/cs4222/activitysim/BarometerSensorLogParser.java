package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/** Parses the barometer sensor log file. */
public class BarometerSensorLogParser
    extends SensorLogParser {

    /** Constructor that initialises the parsing. */
    public BarometerSensorLogParser( File logFile ) 
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
        if( parts.length != 5 ) {
            throw new IOException( "Invalid line in log file \'" + 
                                   logFile.getName() + "\': " + line );
        }
        long timestamp = Long.parseLong( parts[1] );
        float pressure = Float.parseFloat( parts[2] );
        float altitude = Float.parseFloat( parts[3] );
        int accuracy = Integer.parseInt( parts[4] );

        // Create the next sensor event object
        nextEvent = new BarometerEvent( timestamp , pressure , altitude , accuracy );
    }
}
