package nus.cs4222.activitysim;

import java.io.*;
import java.util.*;
import java.text.*;

import android.hardware.*;
import android.util.*;

import nus.cs4222.activitysim.DataStructure.Fingerprint;
import nus.cs4222.activitysim.DataStructure.RadioMap;
import nus.cs4222.activitysim.ActivitySimulator;
import nus.cs4222.activitysim.UserActivities;

/**
   Class containing the activity detection algorithm.

   <p> You can code your activity detection algorithm in this class.
    (You may add more Java class files or add libraries in the 'libs' 
     folder if you need).
    The different callbacks are invoked as per the sensor log files, 
    in the increasing order of timestamps. In the best case, you will
    simply need to copy paste this class file (and any supporting class
    files and libraries) to the Android app without modification
    (in stage 2 of the project).

   <p> Remember that your detection algorithm executes as the sensor data arrives
    one by one. Once you have detected the user's current activity, output
    it using the {@link ActivitySimulator.outputDetectedActivity(UserActivities)}
    method. If the detected activity changes later on, then you need to output the
    newly detected activity using the same method, and so on.
    The detected activities are logged to the file "DetectedActivities.txt",
    in the same folder as your sensor logs.

   <p> To get the current simulator time, use the method
    {@link ActivitySimulator.currentTimeMillis()}. You can set timers using
    the {@link SimulatorTimer} class if you require. You can log to the 
    console/DDMS using either {@code System.out.println()} or using the
    {@link android.util.Log} class. You can use the {@code SensorManager.getRotationMatrix()}
    method (and any other helpful methods) as you would normally do on Android.

   <p> Note: Since this is a simulator, DO NOT create threads, DO NOT sleep(),
    or do anything that can cause the simulator to stall/pause. You 
    can however use timers if you require, see the documentation of the 
    {@link SimulatorTimer} class. 
    In the simulator, the timers are faked. When you copy the code into an
    actual Android app, the timers are real, but the code of this class
    does not need not be modified.
 */
public class ActivityDetection {

    /** Initialises the detection algorithm. */
    public void initDetection()
        throws Exception {

        // Add initialisation code here, if any. If you use static variables in this class (avoid
        //  doing this, unless they are constants), please do remember to initialise them HERE.
        //  Remember that the simulator will be run on multiple traces, and your algorithm's initialisation
        //  should be done here before each trace is simulated.

        // In this "dummy algorithm", we just show a dummy example of a timer that runs every 10 min,
        //  outputting WALKING and INDOOR alternatively.
        // You will most likely not need to use Timers at all, it is just
        //  provided for convenience if you require.
        // REMOVE THIS DUMMY CODE (2 lines below), otherwise it will mess up your algorithm's output
//        SimulatorTimer timer = new SimulatorTimer();
//        timer.schedule( this.task ,        // Task to be executed
//                        10 * 60 * 1000 );  // Delay in millisec (10 min)
//        // Assume the user is IDLE_INDOOR, then change state based on your algorithm
//        ActivitySimulator.outputDetectedActivity( UserActivities.IDLE_INDOOR );

        // If you are using the Piloc API, then you must load a radio map (in this case, Hande
        //  has provided the radio map data for the pathways marked in the map image in IVLE
        //  workbin, which represents IDLE_COM1 state). You can use your own radio map data, or
        //  code your own localization algorithm in PilocApi. Please see the "onWiFiSensorChanged()"
        //  method.
        pilocApi = new PilocApi();
        if( pilocApi.loadRadioMap( new File( "radiomap.rm" ) ) == null ) {
            throw new IOException( "Unable to open radio map file, did you specify the correct path in ActivityDetection.java?" );
        }
        this.windowSize = 300; // ~10 sec
        this.linAccSP = new SignalProcessor[3];
        this.linAccCorr = new Correlation[3];
        for(int i = 0; i < 3; i ++){
            linAccSP[i] = new SignalProcessor(windowSize);
            linAccCorr[i] = new Correlation(windowSize); //longer window size
        }
        this.linAccMeanSP = new SignalProcessor(windowSize);
        this.lightSP = new SignalProcessor(windowSize);
        this.detectedFPs = new Vector<Fingerprint>();
        File fpListFile = new File("Com1Fingerprints.txt");
        Scanner fpIn = new Scanner(fpListFile);
        this.com1FPs = new HashSet();
        while(fpIn.hasNext()){
            String next = fpIn.next();
            this.com1FPs.add(next);
        }


    }

    /** De-initialises the detection algorithm. */
    public void deinitDetection()
        throws Exception {
        // Add de-initialisation code here, if any
    }

    /**
       Called when the accelerometer sensor has changed.

       @param   timestamp    Timestamp of this sensor event
       @param   x            Accl x value (m/sec^2)
       @param   y            Accl y value (m/sec^2)
       @param   z            Accl z value (m/sec^2)
       @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onAcclSensorChanged( long timestamp ,
                                     double x ,
                                     double y ,
                                     double z ,
                                     int accuracy ) {

        // Process the sensor data as they arrive in each callback,
        //  with all the processing in the callback itself (don't create threads).
    }

    /**
       Called when the gravity sensor has changed.

       @param   timestamp    Timestamp of this sensor event
       @param   x            Gravity x value (m/sec^2)
       @param   y            Gravity y value (m/sec^2)
       @param   z            Gravity z value (m/sec^2)
       @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onGravitySensorChanged( long timestamp ,
                                        double x ,
                                        double y ,
                                        double z ,
                                        int accuracy ) {
    }

    /**
       Called when the linear accelerometer sensor has changed.

       @param   timestamp    Timestamp of this sensor event
       @param   x            Linear Accl x value (m/sec^2)
       @param   y            Linear Accl y value (m/sec^2)
       @param   z            Linear Accl z value (m/sec^2)
       @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onLinearAcclSensorChanged( long timestamp ,
                                           double x ,
                                           double y ,
                                           double z ,
                                           int accuracy ) {

        linAccSP[0].update(x);
        linAccSP[1].update(y);
        linAccSP[2].update(z);

        if(linAccSP[0].processedVal >= 150){
            linAccCorr[0].update(linAccSP[0].getAbsMean(), linAccSP[1].getAbsMean());
            linAccCorr[1].update(linAccSP[0].getAbsMean(), linAccSP[2].getAbsMean());
            linAccCorr[2].update(linAccSP[1].getAbsMean(), linAccSP[2].getAbsMean());
        }
        linAccMeanSP.update(linAccSP[0].getMean() + linAccSP[1].getMean() + linAccSP[2].getMean());

        detectActivity();
    }

    /**
       Called when the magnetic sensor has changed.

       @param   timestamp    Timestamp of this sensor event
       @param   x            Magnetic x value (microTesla)
       @param   y            Magnetic y value (microTesla)
       @param   z            Magnetic z value (microTesla)
       @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onMagneticSensorChanged( long timestamp ,
                                         double x ,
                                         double y ,
                                         double z ,
                                         int accuracy ) {
    }

    /**
       Called when the gyroscope sensor has changed.

       @param   timestamp    Timestamp of this sensor event
       @param   x            Gyroscope x value (rad/sec)
       @param   y            Gyroscope y value (rad/sec)
       @param   z            Gyroscope z value (rad/sec)
       @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onGyroscopeSensorChanged( long timestamp ,
                                          double x ,
                                          double y ,
                                          double z ,
                                          int accuracy ) {
    }

    /**
       Called when the rotation vector sensor has changed.

       @param   timestamp    Timestamp of this sensor event
       @param   x            Rotation vector x value (unitless)
       @param   y            Rotation vector y value (unitless)
       @param   z            Rotation vector z value (unitless)
       @param   scalar       Rotation vector scalar value (unitless)
       @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onRotationVectorSensorChanged( long timestamp ,
                                               double x ,
                                               double y ,
                                               double z ,
                                               double scalar ,
                                               int accuracy ) {
    }

    /**
       Called when the barometer sensor has changed.

       @param   timestamp    Timestamp of this sensor event
       @param   pressure     Barometer pressure value (millibar)
       @param   altitude     Barometer altitude value w.r.t. standard sea level reference (meters)
       @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onBarometerSensorChanged( long timestamp ,
                                          double pressure ,
                                          double altitude ,
                                          int accuracy ) {
    }

    /**
       Called when the light sensor has changed.

       @param   timestamp    Timestamp of this sensor event
       @param   light        Light value (lux)
       @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onLightSensorChanged( long timestamp ,
                                      double light ,
                                      int accuracy ) {
        this.lightSP.update(light);
    }

    /**
       Called when the proximity sensor has changed.

       @param   timestamp    Timestamp of this sensor event
       @param   proximity    Proximity value (cm)
       @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onProximitySensorChanged( long timestamp ,
                                          double proximity ,
                                          int accuracy ) {
    }

    /**
       Called when the location sensor has changed.

       @param   timestamp    Timestamp of this location event
       @param   provider     "gps" or "network"
       @param   latitude     Latitude (deg)
       @param   longitude    Longitude (deg)
       @param   accuracy     Accuracy of the location data (you may use this) (meters)
       @param   altitude     Altitude (meters) (may be -1 if unavailable)
       @param   bearing      Bearing (deg) (may be -1 if unavailable)
       @param   speed        Speed (m/sec) (may be -1 if unavailable)
     */
    public void onLocationSensorChanged( long timestamp ,
                                         String provider ,
                                         double latitude ,
                                         double longitude ,
                                         double accuracy ,
                                         double altitude ,
                                         double bearing ,
                                         double speed ) {
    }

    /**
       Called when the WiFi sensor has changed (i.e., a WiFi scan has been performed).

       @param   timestamp           Timestamp of this WiFi scan event
       @param   fingerprintVector   Vector of fingerprints from the WiFi scan
     */
    public void onWiFiSensorChanged( long timestamp ,
                                     Vector< Fingerprint > fingerprintVector ) {

        // You can use Piloc APIs here to figure out the indoor location in COM1, or do
        //  anything that will help you figure out the user activity.
        // You can use the method PilocApi.getLocation(fingerprintVector) to get the location
        //  in COM1 from the WiFi scan. You may use your own radio map, or even write your
        //  own localization algorithm in PilocApi.getLocation().

        // NOTE: Please use the "pilocApi" object defined below to use the Piloc API.
        detectedFPs = fingerprintVector;
    }

    /** Piloc API provided by Hande. */
    private PilocApi pilocApi;

    /** Helper method to convert UNIX millis time into a human-readable string. */
    private static String convertUnixTimeToReadableString( long millisec ) {
        return sdf.format( new Date( millisec ) );
    }

    /** To format the UNIX millis time as a human-readable string. */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd-h-mm-ssa" );

    // Dummy variables used in the dummy timer code example
    private boolean isUserOutside = false;
    private int numberTimers = 0;
    private Runnable task = new Runnable() {
            public void run() {

                // Logging to the DDMS (in the simulator, the DDMS log is to the console)
                System.out.println();
                Log.i( "ActivitySim" , "Timer " + numberTimers + ": Current simulator time: " +
                       convertUnixTimeToReadableString( ActivitySimulator.currentTimeMillis() ) );
                System.out.println( "Timer " + numberTimers + ": Current simulator time: " +
                                    convertUnixTimeToReadableString( ActivitySimulator.currentTimeMillis() ) );

                // Dummy example of outputting a detected activity
                //  to the file "DetectedActivities.txt" in the trace folder.
                //  Here, we just alternate between indoor and walking every 10 min.
                if( ! isUserOutside ) {
                    ActivitySimulator.outputDetectedActivity( UserActivities.IDLE_INDOOR );
                }
                else {
                    ActivitySimulator.outputDetectedActivity( UserActivities.WALKING );
                }
                isUserOutside = !isUserOutside;

                // Set the next timer to execute the same task 10 min later
                ++numberTimers;
                SimulatorTimer timer = new SimulatorTimer();
                timer.schedule( task ,             // Task to be executed
                                10 * 60 * 1000 );  // Delay in millisec (10 min)
            }
    };


    //method to detect activity
    private void detectActivity(){
        if(linAccCorr[0].processedVal >= windowSize){

            double avgAbsMeanLinAcc = (linAccSP[0].getAbsMean() + linAccSP[1].getAbsMean() + linAccSP[2].getAbsMean()) / 3;

            if(avgAbsMeanLinAcc >= 0.9){
                //is walking
                ActivitySimulator.outputDetectedActivity(UserActivities.WALKING);
            }else{
                    double meanSP = linAccMeanSP.getAbsStd();
//                double avgAbsLinAccCorr = (Math.abs(linAccCorr[0].getCorr()) + Math.abs(linAccCorr[1].getCorr()) + Math.abs(linAccCorr[2].getCorr()))/3;
//                System.out.println("x: " + linAccSP[0].getMean() + ", y: " + linAccSP[1].getMean() + ", z: " + linAccSP[2].getMean());
//                System.out.println("x: " + linAccCorr[0].getCorr() + ", y: " + linAccCorr[1].getCorr() + ", z: " + linAccCorr[2].getCorr());
                System.out.println(meanSP);
                if(meanSP >= 0.035){
                    //bus
                    ActivitySimulator.outputDetectedActivity(UserActivities.BUS);
                }else{
                    //idle
                    if(isUserInCom1()){
                        //idle in com1
                        ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_COM1);
                    }else{
                        //idle somewhere
                        if(lightSP.getMean() > 2000.0){
                            ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_OUTDOOR);
                        }else{
                            ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_INDOOR);
                        }
                    }

                }

            }
        }
    }

    private boolean isUserInCom1(){
        for(Fingerprint fp : detectedFPs){
            if(com1FPs.contains(fp.mMac)){
                return true;
            }
        }
        return false;
    }

    //Signal processor
    private SignalProcessor[] linAccSP;
    private Correlation[] linAccCorr;
    private SignalProcessor lightSP;
    private Vector<Fingerprint> detectedFPs;
    private Set<String> com1FPs;
    private SignalProcessor linAccMeanSP;
    private int windowSize;

    //implements online algo to implement sliding window to calculate mean and std dev.
    //This calculates mean and stddev for 1 variable.
    private class SignalProcessor {
        public int windowSize;
        public double abssum;
        public double sum;
        public double sumOfSquares;
        public double[] val;
        public int nextValIndex;
        public int processedVal;


        public SignalProcessor(int windowSize){
            this.windowSize = windowSize;
            this.abssum = 0.0;
            this.sum = 0.0;
            this.sumOfSquares = 0.0;
            val = new double[windowSize];
            nextValIndex = 0;
            processedVal = 0;
        }
        public void printState(){
            System.out.println("sum: "+ sum);
            System.out.println("abssum: " + abssum);
            System.out.println("sumofSquares: " + sumOfSquares);

        }
        public void update(double newVal){
            double oldVal = val[nextValIndex];
            this.abssum = this.abssum + Math.abs(newVal) - Math.abs(oldVal);
            this.sum = this.sum + newVal - oldVal;
            this.sumOfSquares = this.sumOfSquares + (newVal * newVal) - (oldVal * oldVal);
            val[nextValIndex] = newVal;
            nextValIndex = nextValIndex >= windowSize - 1 ? 0 : nextValIndex+1;
            processedVal++;
        }

        public double getMean(){
            return sum / windowSize;
        }

        public double getStd(){
            // VAR(X) = E(X^2) - E(x)^2
            double mean = sum/windowSize;
            return Math.sqrt(sumOfSquares/windowSize - mean * mean);
        }

        public double getAbsMean(){
            return abssum / windowSize;
        }

        public double getAbsStd(){
            double mean = abssum / windowSize;
            //VAR(X) = E(X^2) - E(X)^2
            return Math.sqrt(sumOfSquares / windowSize - mean * mean);
        }
    }

    private class Correlation {
        public int windowSize;
        public double[] xvals;
        public double[] yvals;
        public double sumxx;
        public double sumyy;
        public double sumxy;
        public double sumx;
        public double sumy;
        public int nextValIndex;
        public int processedVal;

        public Correlation(int windowSize){
            this.windowSize = windowSize;
            this.xvals = new double[windowSize];
            this.yvals = new double[windowSize];
            this.sumxx = 0.0;
            this.sumyy = 0.0;
            this.sumxy = 0.0;
            this.sumx = 0.0;
            this.sumy = 0.0;
            this.nextValIndex = 0;
            this.processedVal = 0;
        }
        public void printState(){
            System.out.println("sumxx: " + sumxx + ", sumyy: " +sumyy + ", sumxy: " + sumxy + ", sumx: "+ sumx + ", sumy: "+ sumy);
        }

        public void update(double newx, double newy){
            double oldx = xvals[nextValIndex];
            double oldy = yvals[nextValIndex];

            this.sumxx = this.sumxx + newx*newx - oldx*oldx;
            this.sumyy = this.sumyy + newy*newy - oldy*oldy;
            this.sumxy = this.sumxy + newx*newy - oldx*oldy;
            this.sumx = this.sumx + newx - oldx;
            this.sumy = this.sumy + newy - oldy;

            this.xvals[nextValIndex] = newx;
            this.yvals[nextValIndex] = newy;

            nextValIndex = nextValIndex >= windowSize - 1 ? 0 : nextValIndex+1;
            processedVal++;
        }

        public double getCorr(){
            //corr = (E(XY) - E(X)E(Y)) / (STDX * STDY)
            double stdx = Math.sqrt(this.sumxx / this.windowSize - this.sumx / windowSize * this.sumx / windowSize );
            double stdy = Math.sqrt(this.sumyy / this.windowSize - this.sumy / windowSize * this.sumy / windowSize );
            double topline = (this.sumxy / windowSize) - (this.sumx / this.windowSize * this.sumy / this.windowSize);
//            System.out.println("s: " + varx + ", "+ stdy + ", " + topline);

            return topline / (stdx * stdy);
        }

        public double getAbsCorr(){
            return Math.abs(this.getCorr());
        }
    }

}


