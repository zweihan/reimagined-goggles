package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Abstract class inherited by each sensor log parser.
 */
public abstract class SensorLogParser {

    /** Constructor that initialises the parsing. */
    public SensorLogParser( File logFile ) 
        throws IOException {
        try {

            // Open the log file for reading
            this.logFile = logFile;
            in = new Scanner( logFile );

            // Parse next line
            parseNextLine();
        }
        catch( IOException e ) {
            try {
                in.close();
            }
            catch( Exception e2 ) {
                // Nothing can be done
            }

            // Re-throw the exception
            throw e;
        }
    }

    /** Checks if there is a next sensor event available. */
    public boolean hasNextEvent() 
        throws IOException {
        return ( this.nextEvent != null );
    }

    /** Gets the next sensor event ({@code null} if there is no event). */
    public SimulatorEvent getNextEvent() 
        throws IOException {
        // Store the event to be returned
        SimulatorEvent event = this.nextEvent;
        // Advance to the next event in the log
        parseNextLine();
        // Return the stored event
        return event;
    }

    /** Stops parsing the sensor log file. */
    public void stopParsing() {
        try {
            in.close();
        }
        catch( Exception e ) {
            // Nothing can be done
        }
        finally {
            nextEvent = null;
        }
    }

    /** Parses the next log line (if available, otherwise sets the event to {@code null}). */
    protected abstract void parseNextLine() 
        throws IOException;

    /** Sensor log file. */
    protected File logFile;
    /** Scanner to parse the log file. */
    protected Scanner in;

    /** Next sensor event. */
    protected SimulatorEvent nextEvent;
}
