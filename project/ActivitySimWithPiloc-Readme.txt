ActivitySim README:

This is a single-threaded discrete event simulator. The purpose 
 of the simulator is two-fold:
 1. To easily port your activity detection algorithm code to an 
    Android app (later on in stage 2).
 2. Take care of parsing the input sensor data files, and output 
    the detection results, in the correct format, for evaluating the 
    accuracy.

Code your activity detection algorithm in the class 
  'src/nus/cs4222/activitysim/ActivityDetection.java'. 
Check the documentation in the ActivityDetection.java class to see
 what APIs you can use for outputting, sensor processing, timers, Piloc
 localization, etc.

Compile and execute the simulator on the sensor data you collected.
While executing the program, you must provide a path to the folder
  containing your collected sensor data (log files from the sdcard). 
  The folder can contain multiple traces (each trace in a different
  sub-folder). In this case, each trace is simulated independently.

Compiling:
 $ ant jarify
Executing:
 $ java -jar ActivitySim.jar <path-to-your-trace-folder>
