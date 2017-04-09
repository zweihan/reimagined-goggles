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
        this.windowSize = 350; // ~12 sec
        this.linAccSP = new SignalProcessor[3];
        this.linAccEMA = new ExponentialMovingAverage[3];
        for(int i = 0; i < 3; i ++){
            linAccSP[i] = new SignalProcessor(windowSize);
            linAccEMA[i] = new ExponentialMovingAverage(0.05);
        }
        this.linAccMeanSP = new SignalProcessor(windowSize);
        this.linAccMeanStdEMA = new ExponentialMovingAverage(0.1);
        this.avgAbsMeanLinAccEMA = new ExponentialMovingAverage(0.1);
        this.longPeriodSP = new SignalProcessor(900);
        this.longPeriodSPStdSP = new SignalProcessor(1800);
        this.lightSP = new SignalProcessor(windowSize);
        this.detectedFPs = new Vector<Fingerprint>();
        File fpListFile = new File("Com1Fingerprints.txt");
        Scanner fpIn = new Scanner(fpListFile);
        this.com1FPs = new HashSet();
        while(fpIn.hasNext()){
            String next = fpIn.next();
            this.com1FPs.add(next);
        }
        this.classifier = new ActivityClassifier();
        this.baroSP = new SignalProcessor(200);
        this.baroEMA = new ExponentialMovingAverage(0.5);
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
        // Process the sensor data as they arrive in each callback,
        //  with all the processing in the callback itself (don't create threads).
        linAccSP[0].update(x);
        linAccSP[1].update(y);
        linAccSP[2].update(z);
        longPeriodSP.update(Math.abs(x) + Math.abs(y) + Math.abs(z));
        linAccMeanSP.update(linAccSP[0].getMean() + linAccSP[1].getMean() + linAccSP[2].getMean());
        longPeriodSPStdSP.update(longPeriodSP.getStd());
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

        baroSP.update(baroEMA.average(altitude));
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

//    /** Helper method to convert UNIX millis time into a human-readable string. */
//    private static String convertUnixTimeToReadableString( long millisec ) {
//        return sdf.format( new Date( millisec ) );
//    }



    //method to detect activity
    private void detectActivity(){
        if(linAccSP[0].processedVal >= windowSize && linAccSP[0].processedVal % 60 == 0){
//            System.out.println(longPeriodSP.findMax());
//              double avgAbsMeanLinAcc = Math.sqrt((linAccSP[0].getSS() + linAccSP[1].getSS() + linAccSP[2].getSS())/3);
            double avgAbsMeanLinAcc = avgAbsMeanLinAccEMA.average((linAccSP[0].getAbsMean() + linAccSP[1].getAbsMean() + linAccSP[2].getAbsMean()) / 3);
//            double avgAbsMeanLinAcc = (linAccSP[0].getAbsMean() + linAccSP[1].getAbsMean() + linAccSP[2].getAbsMean()) / 3;
//              System.out.println(avgAbsMeanLinAcc);
            if(avgAbsMeanLinAcc >= 0.85){
                //is walking
//                System.out.println("WALKING");
//                ActivitySimulator.outputDetectedActivity(UserActivities.WALKING);
                classifier.updateActivity(UserActivities.WALKING);

            }else{
                    double linAccMeanStd = linAccMeanStdEMA.average(linAccMeanSP.getAbsStd());
//                double linAccMeanStd = linAccMeanSP.getAbsStd();
//                System.out.println(linAccMeanStd);
                if(baroSP.getStd() >= 1){
                    //bus
//                    System.out.println("BUS");
                    classifier.updateActivity(UserActivities.BUS);
                }else{
                    //idle
                    if(lightSP.getMean() >= 1000.0){
                        //idle outdoor somewhere
//                        System.out.println("COM1");
                        classifier.updateActivity(UserActivities.IDLE_OUTDOOR);
                    }else{
                        //idle somewhere indoors
                        if(isUserInCom1()){
//                            System.out.println("OUTDOOR");
                            classifier.updateActivity(UserActivities.IDLE_COM1);
                        }else{
//                            System.out.println("INDOOR");
                            classifier.updateActivity(UserActivities.IDLE_INDOOR);
                        }
                    }

                }

            }
        }
    }


    private void detectActivity2(){
        if(linAccSP[0].processedVal >= windowSize && linAccSP[0].processedVal % 300 == 0) {
//            System.out.println(longPeriodSP.findMax());
//              double avgAbsMeanLinAcc = Math.sqrt((linAccSP[0].getSS() + linAccSP[1].getSS() + linAccSP[2].getSS())/3);
            double mean = avgAbsMeanLinAccEMA.average((linAccSP[0].getAbsMean() + linAccSP[1].getAbsMean() + linAccSP[2].getAbsMean()) / 3);
            double abstd = linAccMeanStdEMA.average(linAccMeanSP.getAbsStd());
//            double mean = (linAccSP[0].getAbsMean() + linAccSP[1].getAbsMean() + linAccSP[2].getAbsMean()) / 3;
//            double abstd = linAccMeanSP.getAbsStd();
//            double avgAbsMeanLinAcc = (linAccSP[0].getAbsMean() + linAccSP[1].getAbsMean() + linAccSP[2].getAbsMean()) / 3;
            if(mean <= 0.8353){
                if(abstd <= 0.0387){
                    if(isUserInCom1()){
                        classifier.updateActivity(UserActivities.IDLE_COM1);
                    }else if(lightSP.getMean() >= 2000.0){
                        classifier.updateActivity(UserActivities.IDLE_OUTDOOR);
                    }else{
                        classifier.updateActivity(UserActivities.IDLE_INDOOR);
                    }
                }else{
                    classifier.updateActivity(UserActivities.BUS);
                }
            }else if(mean <= 0.9916 && abstd > 0.2135){
                classifier.updateActivity(UserActivities.BUS);
            }else{
                classifier.updateActivity(UserActivities.WALKING);
            }
        }
    }


    private boolean isUserInCom1(){
        int fpDetectedCount = 0;
        for(Fingerprint fp : detectedFPs){
            if(com1FPs.contains(fp.mMac)
                    && fp.mRSSI <= 75
                    ){
                fpDetectedCount++;
            }
        }
        if (fpDetectedCount <= 3) {
            return false;
        }else{
            return true;
        }
    }

    //Signal processor
    private SignalProcessor[] linAccSP;
    private ExponentialMovingAverage[] linAccEMA;
    private SignalProcessor lightSP;
    private Vector<Fingerprint> detectedFPs;
    private Set<String> com1FPs;
    private SignalProcessor linAccMeanSP;
    private int windowSize;
    private ExponentialMovingAverage avgAbsMeanLinAccEMA;
    private ExponentialMovingAverage linAccMeanStdEMA;
    private ActivityClassifier classifier;
    private SignalProcessor longPeriodSP;
    private SignalProcessor longPeriodSPStdSP;
    private SignalProcessor baroSP;
    private ExponentialMovingAverage baroEMA;

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

        public double findMax(){
            double max = 0;
            for(double val : this.val){
                if(Math.abs(val) > max) {
                    max = Math.abs(val);
                }
            }
            return max;
        }

        public double getSS(){
           return this.sumOfSquares/this.windowSize;
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


    private class ExponentialMovingAverage {
        private double alpha;
        private Double oldValue;
        public ExponentialMovingAverage(double alpha) {
            this.alpha = alpha;
        }

        public double average(double value) {
            if (oldValue == null) {
                oldValue = value;
                return value;
            }
            double newValue = oldValue + alpha * (value - oldValue);
            oldValue = newValue;
            return newValue;
        }
    }



    private class ActivityClassifier {
        public UserActivities currentState;
        public int currentStateCount;
        public UserActivities suggestedNewState; // Suggested because we don't know if we 
                                                 // should switch for sure
        public int suggestedNewStateCount;
        public int tries;

        public ActivityClassifier() {
            this.currentState = UserActivities.INCORRECT;
            currentStateCount = 0;
            suggestedNewStateCount = 0;
            suggestedNewState = UserActivities.INCORRECT;
            tries = 0;
        }

        public void updateActivity(UserActivities newState){

//            tries++;
//            if(currentState == newState){
//                currentStateCount++;
//                return;
//            }
//
//            // Change the suggestion if the old one wasn't correct
//            if (newState != suggestedNewState &&
//                    ((float)suggestedNewStateCount/(float)tries) < 0.35) {
//                suggestedNewState = newState;
//                suggestedNewStateCount = 1;
//                tries = 0;
//                return;
//            } else if (newState != suggestedNewState) {
//                return;
//            }
//
//            // We have to be sure this is the new state
//            if (suggestedNewStateCount < 20 ||
//                    (float)suggestedNewStateCount / (float)tries < 0.7) {
//                suggestedNewStateCount++;
//                return;
//            }
//
            switch(newState){
                case BUS: ActivitySimulator.outputDetectedActivity(UserActivities.BUS); break;
                case WALKING: ActivitySimulator.outputDetectedActivity(UserActivities.WALKING); break;
                case IDLE_INDOOR: ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_INDOOR); break;
                case IDLE_OUTDOOR: ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_OUTDOOR); break;
                case IDLE_COM1: ActivitySimulator.outputDetectedActivity(UserActivities.IDLE_COM1); break;
                default: ActivitySimulator.outputDetectedActivity(UserActivities.INCORRECT); break;
            }
//
//            suggestedNewState = UserActivities.INCORRECT;
//            suggestedNewStateCount = 0;
//            tries = 0;
        }

        public void resetStates(){
            this.currentState = UserActivities.INCORRECT;
            this.currentStateCount = 0;
        }
    }

}


