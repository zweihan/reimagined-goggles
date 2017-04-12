Activity Tracking Android app (ActivityTrackApp) readme:

Porting your code to this Android app is simple. You basically just need 
to copy paste your ActivityDetection.java file to the app (with a 1 line change), 
and recompile the app, and try running on a phone. If you face any problems, 
please email me.

Remember to take a **BACKUP** of your code BEFORE OVERWRITING FILES.
This app is a slightly modified version of SensorLogApp, which additionally 
displays the most recent 10 detected activities (with timestamp) from 
your algorithm. You can see your algorithm output just below the buttons.
By default, the algorithm in the app's code does nothing except
run a dummy timer every 10 min, so you need to overwrite it with your
algorithm.

Steps to port your code: 
 1. Try compiling the app (just to check if your build system is OK with the specified android target version, etc).
 2. Clean the project (IMPORTANT!!). 
 3. Take a *BACKUP* of your simulator code. DO NOT OVERWRITE your code by mistake while copy pasting :P
 4. Copy paste *your* ActivityDetection.java file, PilocApi.java, and any other source files and libraries you may have added, from your simulator code (ActivitySim) to the Android app's code (ActivityTrackApp), overwriting the dummy algorithm.
 5. Make a small code change in the *copied* ActivityDetection.java in initDetection():
        //if( pilocApi.loadRadioMap( new File( "radiomap.rm" ) ) == null ) {
        if( pilocApi.loadRadioMap( ActivitySimulator.getRadioMapFilePath( "radiomap.rm" ) ) == null ) {
 6. Recompile the android app (clean the project first, in case you did not do so in step 2).
 7. Place Hande's radiomap.rm file (or your own radio map, if you are using a different one) in the phone's sdcard under the folder "ActivityTrackApp" (running the app and pressing the "Start logging" button will create the "ActivityTrackApp" directory in the sdcard in case it is not present). In case you are using Windows Explorer to copy the radio map file from the laptop to the phone, you may have to restart the phone for the filesystem changes to be flushed to the sdcard and become visible on your laptop.
 8. Try running ActivityTrackApp on your phone, and see if your algorithm seems to detect your activities in real-time :-)
Note: If your code is too slow, or you have done too many calculations, you may notice that the GUI is laggy. 
