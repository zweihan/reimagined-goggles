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

    /** Initialises the timer. */
    public SimulatorTimer() {
        timer = new Timer();
    }

    /** 
       Schedules the specified task for execution after the specified delay.

       @task   task     Task to be scheduled (override the {@code run()} method)
       @delay  delay    Delay in milliseconds before task is to be executed.
     */
    public void schedule( final Runnable task , 
                          long delay ) {

        // On a real device, schedule a real timer
        TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    task.run();
                }
            };
        timer.schedule( timerTask , delay );
    }

    /** Cancels a timer. */
    public void cancel() {
        // Cancel the timer
        timer.cancel();
    }

    /** Invalidates the timer task in this class, but does not cancel the timer. */
    public void invalidateTimerTask() {
        // On a real device, nothing to do
    }

    /** Real Timer used for real device. */
    private Timer timer;
}
