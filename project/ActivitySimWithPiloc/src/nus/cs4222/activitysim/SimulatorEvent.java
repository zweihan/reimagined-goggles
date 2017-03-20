package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Abstract class representing a simulator event (either a sensor or timer event).

   <p> Each event has a timestamp. Events with the same timestamp are given
   different sequence numbers to differentiate them.
 */
public abstract class SimulatorEvent 
    implements Comparable< SimulatorEvent > {

    /** Timestamp (millisecs since epoch). */
    protected long timestamp;
    /** Sequence number. */
    protected int sequenceNumber;

    /** Next sequence number to use. */
    protected static int nextSequenceNumber = 1;

    /** Constructor that initialises the timestamp and sequence number. */
    public SimulatorEvent( long timestamp ) {
        // Store the timestamp
        this.timestamp = timestamp;
        // Set the next sequence number (ignore wrap around :P)
        this.sequenceNumber = SimulatorEvent.nextSequenceNumber++;
    }

    /** Gets the timestamp of the event. */
    public long getTimestamp() {
        return timestamp;
    }

    /** Gets the sequence number of the event. */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /*
      Note: hashCode() and equals() methods are required for
            Maps and Sets to check whether keys are equal.
            Comparators/Comparables are needed for Sorting.

            For a Sorted Set, Comparator (or Comparable)
            and equals() and hashCode() are ALL required, and must 
            be *consistent*, for it to work properly.
            (see Javadocs for meaning of 'consistent with equals'
             and see stackoverflow for difference between 
             Comparator and Comparable. In simple terms, 
             Comparable is the natural ordering, and Comparator
             allows us to define alternative orderings).

            In this class, only the timestamp and sequence number 
            is used for ordering.
     */

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return( new Long( timestamp ).hashCode() | 
                new Integer( sequenceNumber ).hashCode() );
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals( Object otherObject ) {
        // Cast the object
        SimulatorEvent other = (SimulatorEvent) otherObject;
        // Test if timestamp and sequence number are the same
        return ( timestamp == other.timestamp && 
                 sequenceNumber == other.sequenceNumber );
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo( SimulatorEvent other ) {
        // Check the timestamp and sequence number difference
        long timestampDifference = timestamp - other.timestamp;
        if( timestampDifference > 0 ) {
            return 1;
        }
        else if( timestampDifference < 0 ) {
            return -1;
        }
        return ( sequenceNumber - other.sequenceNumber );
    }

    /** Handles the event. */
    public abstract void handleEvent();
}
