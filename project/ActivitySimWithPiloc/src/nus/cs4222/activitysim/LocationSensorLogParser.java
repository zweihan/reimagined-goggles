package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/** Parses the location sensor log file. */
public class LocationSensorLogParser
    extends SensorLogParser {

    /** Constructor that initialises the parsing. */
    public LocationSensorLogParser( File logFile ) 
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
        if( parts.length != 11 ) {
            throw new IOException( "Invalid line in log file \'" + 
                                   logFile.getName() + "\': " + line );
        }
        long timestamp = Long.parseLong( parts[1] );
        String provider = parts[4];
        double latitude = Double.parseDouble( parts[5] );
        double longitude = Double.parseDouble( parts[6] );
        float accuracy = Float.parseFloat( parts[7] );
        double altitude = Double.parseDouble( parts[8] );
        float bearing = Float.parseFloat( parts[9] );
        float speed = Float.parseFloat( parts[10] );

        // Create the next sensor event object
        nextEvent = new LocationEvent( timestamp , 
                                       provider , 
                                       latitude , 
                                       longitude , 
                                       accuracy , 
                                       altitude , 
                                       bearing , 
                                       speed );
    }
}
