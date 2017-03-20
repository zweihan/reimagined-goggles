package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

import nus.cs4222.activitysim.DataStructure.Fingerprint;

/** Parses the WiFi sensor log file. */
public class WiFiSensorLogParser
    extends SensorLogParser {

    /** Constructor that initialises the parsing. */
    public WiFiSensorLogParser( File logFile ) 
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
        // Note: The fingerprints are in tuples of 3 (MAC, RSSI, Freq)
        if( parts.length < 2 || ( parts.length - 2 ) % 3 != 0 ) {
            throw new IOException( "Invalid line in log file \'" + 
                                   logFile.getName() + "\': " + line );
        }
        long timestamp = Long.parseLong( parts[1] );
        Vector< Fingerprint > fingerprintVector = new Vector<>();
        for( int i = 2 ; i < parts.length ; i += 3 ) {
            String mac = parts[ i ];
            int rssi = Integer.parseInt( parts[ i + 1 ] );
            int freq = Integer.parseInt( parts[ i + 2 ] );
            fingerprintVector.add( new Fingerprint( mac , rssi , freq ) );
        }

        // Create the next sensor event object
        nextEvent = new WiFiEvent( timestamp , fingerprintVector );
    }
}
