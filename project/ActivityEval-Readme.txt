ActivityEval README:

You can evaluate your detection algorithm in two ways:
 1. Compare the 'GroundTruth.txt' and 'DetectedActivities.txt' files
    after running the simulation. This will help you understand exactly 
    when your algorithm went wrong.
    Note: Understanding the log file -
     2016-03-21-11-07-11am,1458529631287,IDLE_INDOOR
     2016-03-21-11-17-07am,1458530227436,WALKING
    The above lines mean that, from 11:07 AM to 11:17 AM, the user was
    indoor, while after 11:17 AM, the user was walking.
    In the GroundTruth.txt file, consider the second timestamp in the 
    log line, and ignore the first timestamp.
 2. Use ActivityEval (this program) to get an accuracy and latency
    summary for your algorithm's detection results on different traces
    (AFTER you run ActivitySim).

While executing the program, you must provide a path to the folder
 containing the sensor data, ground truth, and activity detection results. 
 The folder can contain multiple traces (each trace in a different
 sub-folder). 

Compiling:
 $ ant jarify
Executing:
 $ java -jar ActivityEval.jar <path-to-the-folder-containing-trace-subfolders>
  (If there is a complaint about Java class version, then run '$ ant clean'
   before compiling, and delete the jar file)
