package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Timer event.
 */
public class TimerEvent 
    extends SimulatorEvent {

    // Timer
    private SimulatorTimer timer;
    // Timer task
    private Runnable task;

    /** Constructor that initialises the timer event. */
    public TimerEvent( long timestamp , 
                       Runnable task , 
                       SimulatorTimer timer ) {
        // Init the timestamp and sequence number
        super( timestamp );
        // Store the timer and timer task
        this.task = task;
        this.timer = timer;
    }

    /** Handles the event. */
    @Override
    public void handleEvent() {
        timer.invalidateTimerTask();
        task.run();
    }
}
