package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;

/**
   Fake timer for simulating delayed events.

   <p> This is a 'fake (simulated)' timer that executes the run()
   method of the timer task, after a simulated delay (specified to the
   {@code schedule()} method).

   <p> Schedule a task to be executed in the future using the
   schedule() method. You can cancel the timer using the cancel()
   method.
 */
public class SimulatorTimer {

    /** 
       Schedules the specified task for execution after the specified delay.

       @task   task     Task to be scheduled (override the {@code run()} method)
       @delay  delay    Delay in milliseconds before task is to be executed.
     */
    public void schedule( Runnable task , 
                          long delay ) {

        // Check the args
        long scheduledTime = delay + ActivitySimulator.currentTimeMillis();
        if( delay < 0L || scheduledTime < 0L ) {
            throw new IllegalArgumentException( "Delay is negative, or Delay + ActivitySimulator.currentTimeMillis() is negative" );
        }
        else if( task == null ) {
            throw new NullPointerException( "Timer task must not be null" );
        }
        else if( timerEvent != null || isUseless ) {
            throw new IllegalStateException( "Timer task has already been scheduled/cancelled/finished." + 
                                             " You need to create another SimulatorTimer object to schedule() again" );
        }

        // Add the timer task to the list
        timerEvent = new TimerEvent( scheduledTime ,
                                     task ,
                                     this );
        isUseless = true;
        ActivitySimulator.addTimerEvent( timerEvent );
    }

    /** Cancels a timer. */
    public void cancel() {

        // Check the state of the timer
        if( timerEvent == null ) {
            throw new IllegalStateException( "Timer task has not been scheduled, or has already been cancelled/finished" );
        }

        // Remove the timer event and task
        ActivitySimulator.removeTimerEvent( timerEvent );
        timerEvent = null;
    }

    /** Invalidates the timer task in this class, but does not cancel the timer. */
    public void invalidateTimerTask() {

        // Check the state of the timer
        if( timerEvent == null ) {
            throw new IllegalStateException( "Timer task has not been scheduled, or has already been cancelled/finished" );
        }

        // Remove the timer event
        timerEvent = null;
    }

    /** Timer event. */
    private TimerEvent timerEvent = null;
    /** Flag to indicate that this timer is now useless. */
    private boolean isUseless = false;
}
