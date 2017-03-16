package nus.cs4222.sensorlogapp;

import java.util.*;
import java.text.*;

import android.os.*;
import android.app.*;
import android.content.*;
import android.widget.*;
import android.hardware.*;
import android.location.*;
import android.net.wifi.*;
import android.util.*;
import android.support.v4.app.NotificationCompat;

import nus.cs4222.sensorlogapp.DataStructure.Fingerprint;
import nus.cs4222.sensorlogapp.DataStructure.RadioMap;

/** Service that samples and logs sensor data. */
public class SensorLogAppService
    extends Service 
    implements SensorEventListener , 
               LocationListener {

    /** Called when the Service is created. */
    @Override
    public void onCreate() {

        try {
            super.onCreate();

            // Get the handler to the main UI thread
            mainHandler = new Handler();

            // Put the service in the foreground
            putServiceInForeground();

            // Register real-time displays for sensor data
            registerSensorDisplays();

            // Acquire wake lock
            acquireLocks();
        }
        catch ( Exception e ) {
            // Log the exception
            Log.e ( TAG , "Unable to start service" , e );
            // Display a toast to the user
            Toast.makeText( getApplicationContext() , 
                            "Unable to start service" , 
                            Toast.LENGTH_SHORT ).show();
        }
    }

    /** Called when the service is destroyed. */
    @Override
    public void onDestroy() {

        try {
            super.onDestroy();

            // Put the service in the background
            putServiceInBackground();

            // Clear the real-time sensor displays
            RealTimeDisplay.clearDisplays();
        }
        catch ( Exception e ) {
            // Log the exception
            Log.e ( TAG , "Unable to stop service" , e );
            // Display a toast to the user
            Toast.makeText( getApplicationContext() , 
                            "Unable to stop service, check log" , 
                            Toast.LENGTH_SHORT ).show();
        }
        finally {

            // Release wake lock
            releaseLocks();
        }
    }

    /** Called when the service is destroyed. */
    @Override
    public IBinder onBind( Intent intent ) {

        /*
          NOTE:
           There is a bug in Android, where bindService() returns true (success) and 
           onServiceConnected() is NOT called when the onBind() returns null binder.
           So, there is no way for the service to tell the App that it failed to start :P
           Hence, the service displays a toast that it failed to start.
           http://code.google.com/p/android/issues/detail?id=4368
         */
        return binder;
    }

    /** Helper method to acquire wake/wifi/multicast locks. */
    private void acquireLocks() 
        throws Exception {

        try {

            // Step 1: Acquire Partial wake lock
            PowerManager pm = (PowerManager) getSystemService( Context.POWER_SERVICE );
            partialWakeLock = pm.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK , lockTag );
            partialWakeLock.acquire();

            // Step 2: Acquire Wifi lock
            WifiManager wm = (WifiManager) getSystemService( Context.WIFI_SERVICE );
            wifiLock = wm.createWifiLock( WifiManager.WIFI_MODE_FULL , lockTag );
            wifiLock.acquire();

            // Step 3: Acquire Multicast lock
            //multicastLock = wm.createMulticastLock( lockTag );
            //multicastLock.acquire();
        }
        catch ( Exception e ) {

            // Release the locks and re-throw the exception
            releaseLocks();
            throw e;
        }
    }

    /** Helper method to release wake/wifi/multicast locks. */
    private void releaseLocks() {

        // Step 1: Release the partial wake lock
        try {
            if ( partialWakeLock != null )
                partialWakeLock.release();
        }
        catch ( Exception e ) {
            // Ignore, we can't do anything about this
        }

        // Step 2: Release the wifi lock
        try {
            if ( wifiLock != null )
                wifiLock.release();
        }
        catch ( Exception e ) {
            // Ignore, we can't do anything about this
        }

        /*
        // Step 3: Release the multicast lock
        try {
            if ( multicastLock != null )
                multicastLock.release();
        }
        catch ( Exception e ) {
            // Ignore, we can't do anything about this
        }
        */
    }

    /** Helper method to put service into foreground. */
    private void putServiceInForeground() {

        // Check if the service is already in foreground
        if( isForeground )
            return;

        // Step 1: Create a foreground notification
        // Step 1a: Create an intent specifying what activity to launch
        Intent launchIntent = new Intent( this , SensorLogAppActivity.class );
        // These two flags prevent duplicate activity instances from being created
        launchIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                               Intent.FLAG_ACTIVITY_SINGLE_TOP );
        PendingIntent pendingIntent = 
            PendingIntent.getActivity( this ,          // Context
                                       0 ,             // Reserved
                                       launchIntent ,  // Intent to fire
                                       0 );            // Not sure what this flag is!

        // UPDATE: This method of putting a notification is deprecated.
        //         Use the new method as follows.
        /*
        // Step 1b: Create a notification tied to this Intent
        String notificationTitle = "Shuttle Bus App Service" , 
            notificationText = "Bus-stop detection";
        Notification notification = 
            new Notification( R.drawable.shuttlebusiconcircle ,    // Icon
                              notificationText ,                   // Text
                              System.currentTimeMillis() );        // Timestamp
        notification.setLatestEventInfo( this ,                    // Context
                                         notificationTitle ,       // Title
                                         notificationText ,        // Text
                                         pendingIntent );          // Intent to fire

        // Step 2: Put the service in the foreground
        int dummyID = 1234;    // Dummy notification ID
        startForeground( dummyID , notification );
        */

        // UPDATE: New method for creating notifications.

        // Step 1b: Create a notification tied to this Intent
        String notificationTitle = "SensorLogApp Service" , 
            notificationText = "Tap to launch SensorLogApp's GUI";
        NotificationCompat.Builder builder = 
            new NotificationCompat.Builder( this );
        // NOTE: Now lollipop insists that the icons are white :P
        Notification notification = 
            builder.setContentIntent( pendingIntent )
                   .setSmallIcon( R.drawable.ic_launcher )
                   .setTicker( notificationText )
                   .setWhen( System.currentTimeMillis() )
                   .setAutoCancel( false )
                   .setContentTitle( notificationTitle )
                   .setContentText( notificationText )
                   .build();
        // Step 2: Put the service in the foreground
        int dummyID = 1234;    // Dummy notification ID
        startForeground( dummyID , notification );

        // Set the flag
        isForeground = true;
    }

    /** Helper method to put service into foreground. */
    /*
    private void putServiceInForeground() {

        // Check if the service is already in foreground
        if( isForeground )
            return;

        // Step 1: Create a foreground notification
        // Step 1a: Create an intent specifying what activity to launch
        Intent launchIntent = new Intent( this , SensorLogAppActivity.class );
        // These two flags prevent duplicate activity instances from being created
        launchIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                               Intent.FLAG_ACTIVITY_SINGLE_TOP );
        PendingIntent pendingIntent = 
            PendingIntent.getActivity( this ,          // Context
                                       0 ,             // Reserved
                                       launchIntent ,  // Intent to fire
                                       0 );            // Not sure what this flag is!
        // Step 1b: Create a notification tied to this Intent
        String notificationTitle = "SensorLogApp Service" , 
            notificationText = "Service to log sensor data";
        Notification notification = 
            new Notification( R.drawable.ic_launcher ,       // Icon
                              notificationText ,             // Text
                              System.currentTimeMillis() );  // Timestamp
        notification.setLatestEventInfo( this ,              // Context
                                         notificationTitle , // Title
                                         notificationText ,  // Text
                                         pendingIntent );    // Intent to fire

        // Step 2: Put the service in the foreground
        int dummyID = 1234;    // Dummy notification ID
        startForeground( dummyID , notification );

        // Set the flag
        isForeground = true;
    }
    */

    /** Helper method to put service into background. */
    private void putServiceInBackground() {

        // Check if the service is already in background
        if( ! isForeground )
            return;

        // Set the flag
        isForeground = false;

        // Put the service into background
        // This also cancels the foreground notification
        stopForeground( true );
    }

    /** API for the bound service. */
    public class SensorLogAppServiceApi 
        extends Binder {

        /** Starts the data collection. */
        public void startDataCollection( String logName ) 
            throws Exception {
            // Open the log files
            openLogFiles( logName );
            // Start sampling sensors
            startSensorSampling();
        }

        /** Stops the data collection. */
        public void stopDataCollection() {
            // Stop sampling sensors
            stopSensorSampling();
            // Close the log files
            closeLogFiles();
        }

        /** Logs the ground truth marked manually by the user. */
        public void logGroundTruth( long timestamp , 
                                    UserActivities activity ) {
            String logLine = sdf.format( new Date( timestamp ) ) + "," + 
                timestamp + "," + activity.toString();
            loggerGroundTruth.logEvent( logLine );
        }
    }

    /** Opens the log files for sensor data logging. */
    private void openLogFiles( String logName ) 
        throws Exception {
        loggerLocation.openLogFile( logName , "Loc.txt" );
        loggerAccelerometer.openLogFile( logName , "Accl.txt" );
        loggerGravity.openLogFile( logName , "Gravity.txt" );
        loggerLinearAccl.openLogFile( logName , "LinAccl.txt" );
        loggerMagnetic.openLogFile( logName , "Mag.txt" );
        loggerGyroscope.openLogFile( logName , "Gyro.txt" );
        loggerRotationVector.openLogFile( logName , "RotVec.txt" );
        loggerBarometer.openLogFile( logName , "Baro.txt" );
        loggerLight.openLogFile( logName , "Light.txt" );
        loggerProximity.openLogFile( logName , "Proximity.txt" );
        loggerGroundTruth.openLogFile( logName , "GroundTruth.txt" );
        loggerWiFi.openLogFile( logName , "WiFi.txt" );
    }

    /** Closes the log files for sensor data logging. */
    private void closeLogFiles() {
        loggerLocation.closeLogFile();
        loggerAccelerometer.closeLogFile();
        loggerGravity.closeLogFile();
        loggerLinearAccl.closeLogFile();
        loggerMagnetic.closeLogFile();
        loggerGyroscope.closeLogFile();
        loggerRotationVector.closeLogFile();
        loggerBarometer.closeLogFile();
        loggerLight.closeLogFile();
        loggerProximity.closeLogFile();
        loggerGroundTruth.closeLogFile();
        loggerWiFi.closeLogFile();
    }

    // Sensor data loggers
    private FileLogger loggerLocation = new FileLogger();
    private FileLogger loggerAccelerometer = new FileLogger();
    private FileLogger loggerGravity = new FileLogger();
    private FileLogger loggerLinearAccl = new FileLogger();
    private FileLogger loggerMagnetic = new FileLogger();
    private FileLogger loggerGyroscope = new FileLogger();
    private FileLogger loggerRotationVector = new FileLogger();
    private FileLogger loggerBarometer = new FileLogger();
    private FileLogger loggerLight = new FileLogger();
    private FileLogger loggerProximity = new FileLogger();
    // Logger for ground truth
    private FileLogger loggerGroundTruth = new FileLogger();
    // Logger for WiFi scans
    private FileLogger loggerWiFi = new FileLogger();

    /** Starts sensor sampling at the specified sampling rates. */
    private void startSensorSampling() {

        // Get the sensor manager
        sensorManager = (SensorManager) getSystemService( Context.SENSOR_SERVICE );
        // Get the location manager
        locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        // Get the WiFi manager
        wifiManager = (WifiManager) getSystemService( Context.WIFI_SERVICE );

        // Initialise the sensor displays
        RealTimeDisplay.updateDisplay( DISPLAY_LOCATION , "provider: \nlatitude: \nlongitude: \naltitude: \nspeed: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_ACCELEROMETER , "x: \ny: \nz: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_GRAVITY , "x: \ny: \nz: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_LINEAR_ACCL , "x: \ny: \nz: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_MAGNETIC , "x: \ny: \nz: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_GYROSCOPE , "x: \ny: \nz: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_ROTATION_VECTOR , "x: \ny: \nz: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_BAROMETER , "pressure: \naltitude: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_LIGHT , "light: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_PROXIMITY , "proximity: \n" );
        RealTimeDisplay.updateDisplay( DISPLAY_WIFI , "fingerprint: \n" );

        // Get the various sensors (and check if they are available or not)
        Sensor acclSensor = (Sensor) sensorManager.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
        if( acclSensor == null ) {
            RealTimeDisplay.updateDisplay( DISPLAY_ACCELEROMETER , "Accl sensor not available" );
        }
        Sensor gravitySensor = (Sensor) sensorManager.getDefaultSensor( Sensor.TYPE_GRAVITY );
        if( gravitySensor == null ) {
            RealTimeDisplay.updateDisplay( DISPLAY_GRAVITY , "Gravity sensor not available" );
        }
        Sensor linAcclSensor = (Sensor) sensorManager.getDefaultSensor( Sensor.TYPE_LINEAR_ACCELERATION );
        if( linAcclSensor == null ) {
            RealTimeDisplay.updateDisplay( DISPLAY_LINEAR_ACCL , "Linear Accl sensor not available" );
        }
        Sensor magSensor = (Sensor) sensorManager.getDefaultSensor( Sensor.TYPE_MAGNETIC_FIELD );
        if( magSensor == null ) {
            RealTimeDisplay.updateDisplay( DISPLAY_MAGNETIC , "Magnetic sensor not available" );
        }
        Sensor gyroSensor = (Sensor) sensorManager.getDefaultSensor( Sensor.TYPE_GYROSCOPE );
        if( gyroSensor == null ) {
            RealTimeDisplay.updateDisplay( DISPLAY_GYROSCOPE , "Gyro sensor not available" );
        }
        Sensor rotVecSensor = (Sensor) sensorManager.getDefaultSensor( Sensor.TYPE_ROTATION_VECTOR );
        if( rotVecSensor == null ) {
            RealTimeDisplay.updateDisplay( DISPLAY_ROTATION_VECTOR , "Rot Vector sensor not available" );
        }
        Sensor baroSensor = (Sensor) sensorManager.getDefaultSensor( Sensor.TYPE_PRESSURE );
        if( baroSensor == null ) {
            RealTimeDisplay.updateDisplay( DISPLAY_BAROMETER , "Baro sensor not available" );
        }
        Sensor lightSensor = (Sensor) sensorManager.getDefaultSensor( Sensor.TYPE_LIGHT );
        if( lightSensor == null ) {
            RealTimeDisplay.updateDisplay( DISPLAY_LIGHT , "Light sensor not available" );
        }
        Sensor proximitySensor = (Sensor) sensorManager.getDefaultSensor( Sensor.TYPE_PROXIMITY );
        if( proximitySensor == null ) {
            RealTimeDisplay.updateDisplay( DISPLAY_PROXIMITY , "Proximity sensor not available" );
        }
        // Get the location sensor (and check if the location is enabled by the user or not)
        List< String > enabledProviders = locationManager.getProviders( true );
        enabledProviders.remove( LocationManager.PASSIVE_PROVIDER );
        if( ! enabledProviders.contains( LocationManager.GPS_PROVIDER ) || 
            ! enabledProviders.contains( LocationManager.NETWORK_PROVIDER ) ) {
            RealTimeDisplay.updateDisplay( DISPLAY_LOCATION , 
                                           "Please enable both GPS and network location providers" );
        }
        // Get the WiFi interface (and check if the interface is enabled by the user or not)
        if( ! wifiManager.isWifiEnabled() ) {
            RealTimeDisplay.updateDisplay( DISPLAY_WIFI , 
                                           "Please turn on the WiFi interface" );
        }

        // Initialise log and display timestamps
        prevLogTimeLocation = 
            prevLogTimeAccelerometer = 
            prevLogTimeGravity = 
            prevLogTimeLinearAccelerometer = 
            prevLogTimeMagnetic = 
            prevLogTimeGyroscope = 
            prevLogTimeRotationVector = 
            prevLogTimeBarometer = 
            prevLogTimeLight = 
            prevLogTimeProximity = 0L;
        prevDisplayTimeLocation = 
            prevDisplayTimeAccelerometer = 
            prevDisplayTimeGravity = 
            prevDisplayTimeLinearAccelerometer = 
            prevDisplayTimeMagnetic = 
            prevDisplayTimeGyroscope = 
            prevDisplayTimeRotationVector = 
            prevDisplayTimeBarometer = 
            prevDisplayTimeLight = 
            prevDisplayTimeProximity = 0L;

        // Start sampling the various sensors
        if( acclSensor != null ) {
            sensorManager.registerListener( this , acclSensor , SensorManager.SENSOR_DELAY_FASTEST );
        }
        if( gravitySensor != null ) {
            sensorManager.registerListener( this , gravitySensor , SensorManager.SENSOR_DELAY_FASTEST );
        }
        if( linAcclSensor != null ) {
            sensorManager.registerListener( this , linAcclSensor , SensorManager.SENSOR_DELAY_FASTEST );
        }
        if( magSensor != null ) {
            sensorManager.registerListener( this , magSensor , SensorManager.SENSOR_DELAY_FASTEST );
        }
        if( gyroSensor != null ) {
            sensorManager.registerListener( this , gyroSensor , SensorManager.SENSOR_DELAY_FASTEST );
        }
        if( rotVecSensor != null ) {
            sensorManager.registerListener( this , rotVecSensor , SensorManager.SENSOR_DELAY_FASTEST );
        }
        if( baroSensor != null ) {
            sensorManager.registerListener( this , baroSensor , SensorManager.SENSOR_DELAY_FASTEST );
        }
        if( lightSensor != null ) {
            sensorManager.registerListener( this , lightSensor , SensorManager.SENSOR_DELAY_FASTEST );
        }
        if( proximitySensor != null ) {
            sensorManager.registerListener( this , proximitySensor , SensorManager.SENSOR_DELAY_FASTEST );
        }
        // Start sampling the location sensors (but at a 10 sec interval, otherwise the battery will drain too fast)
        if( enabledProviders.contains( LocationManager.GPS_PROVIDER ) && 
            enabledProviders.contains( LocationManager.NETWORK_PROVIDER ) ) {
            for( String provider : enabledProviders ) {
                locationManager.requestLocationUpdates( provider ,                  // GPS or Network provider
                                                        SAMPLING_RATE_LOCATION ,    // Sampling rate (millis): Every 10 sec
                                                        0.0F ,                      // Min distance change: 0 meters
                                                        this );                     // Location Listener to be called
            }
        }
        // Start WiFi scans (but at a 10 sec interval, otherwise the battery will drain too fast)
        if( wifiManager.isWifiEnabled() ) {
            // Create a thread just to do WiFi scans
            wifiScanThread = new WiFiScanThread();
            wifiScanThread.start();
        }
    }

    /** Thread to periodically perform WiFi scans. */
    public class WiFiScanThread 
        extends Thread {

        /** Handler to this thread. */
        private Handler wifiScanThreadHandler;
        /** Broadcast receiver to receive WiFi scan events. */
        private BroadcastReceiver wifiScanReceiver;
        /** Fingerprint scan count. */
        private int scanCount;

        /** Start point of the thread. */
        @Override
        public void run() {
            try {

                // Make this a looper thread, so that we can run the broadcast receiver's
                //  onReceive() method in this thread, instead of in the main thread.
                Looper.prepare();
                // Get a handler to this thread
                // Note: We can start posting runnables to this thread, according to the link:
                //  http://stackoverflow.com/questions/28908729/what-happens-if-a-handler-posts-a-message-to-a-thread-after-looper-prepare-but
                wifiScanThreadHandler = new Handler();

                // Register a broadcast receiver for when the WiFi scan is finished
                IntentFilter wifiScanIntent = new IntentFilter();
                wifiScanIntent.addAction( WifiManager.SCAN_RESULTS_AVAILABLE_ACTION );
                wifiScanReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive( Context context , Intent intent ) {
                            try {

                                // Note: We are executing in the wifi scan thread's context, not the
                                //  main thread. So, we can safely sleep here.

                                // Get the scan results
                                ArrayList< String > operatorNameList = 
                                    new ArrayList< String >();
                                Vector< Fingerprint > fingerprintVector = 
                                    getFingerprint( operatorNameList );
                                ++scanCount;

                                // Log the fingerprint to a file
                                StringBuilder sb = new StringBuilder();
                                boolean firstAccessPoint = true;
                                for( Fingerprint fingerprint : fingerprintVector ) { 
                                    if( ! firstAccessPoint ) {
                                        sb.append( "," );
                                    }
                                    else {
                                        firstAccessPoint = false;
                                    }
                                    sb.append( fingerprint.mMac );
                                    sb.append( "," );
                                    sb.append( fingerprint.mRSSI );
                                    sb.append( "," );
                                    sb.append( fingerprint.mFrequency );
                                }
                                String logLine = sb.toString();
                                loggerWiFi.logEvent( logLine );

                                // Post to the GUI's handler to update the GUI
                                sb = new StringBuilder();
                                sb.append( "fingerprint: " + scanCount + "\n" );
                                int i = 0;
                                for( Fingerprint fingerprint : fingerprintVector ) { 
                                    // Add an operator friendly name only for the GUI
                                    if( android.os.Build.VERSION.SDK_INT >= 23 ) {
                                        sb.append( operatorNameList.get( i ) );
                                        sb.append( " " );
                                    }
                                    sb.append( fingerprint.mMac );
                                    sb.append( " " );
                                    sb.append( fingerprint.mRSSI );
                                    sb.append( "dBm " );
                                    sb.append( fingerprint.mFrequency );
                                    sb.append( "MHz \n" );
                                    ++i;
                                }
                                final String displayLine = sb.toString();
                                mainHandler.post( new Runnable() {
                                        @Override
                                        public void run() {
                                            RealTimeDisplay.updateDisplay( DISPLAY_WIFI , displayLine );
                                        }
                                    } );

                                // Sleep for about 10 sec (staggered sampling)
                                sleep( SAMPLING_RATE_WIFI );

                                // Start the next scan
                                wifiManager.startScan();
                            }
                            catch( Exception e ) {
                                // Log the exception
                                Log.e( TAG , "WiFiScanThread interrupted in onReceive()" , e );
                            }
                        }
                    };
                SensorLogAppService.this.registerReceiver( wifiScanReceiver ,           // Broadcast receiver
                                                           wifiScanIntent ,             // Receive only WiFi scan finished events
                                                           null ,                       // No permissions required to receive this broadcast
                                                           wifiScanThreadHandler );     // onReceive() should be in this thread's context

                // Start a scan, which should later trigger a scan finish event in onReceive()
                scanCount = 0;
                wifiManager.startScan();

                // Start looping, the onReceive() method will execute in this thread's context
                Looper.loop();
            }
            catch( Exception e ) {
                // Log the exception
                Log.e( TAG , "WiFiScanThread looper stopped" , e );
            }
        }

        /** Stop the looper thread and WiFi scanning. */
        public void stopWiFiScans() {

            // De-register the broadcast receiver and make it null
            if( wifiScanReceiver != null ) {
                SensorLogAppService.this.unregisterReceiver( wifiScanReceiver );
                wifiScanReceiver = null;
            }

            // Stop the looper thread in the WiFi scan thread's context
            if( wifiScanThreadHandler != null ) {
                wifiScanThreadHandler.post( new Runnable() {
                        @Override
                        public void run() {
                            Looper myLooper = Looper.myLooper();
                            myLooper.quit();
                        }
                    } );
            }
        }

        /** Hande's helper method to get the scan results (fingerprint). */
	private Vector<Fingerprint> getFingerprint( List< String > operatorNameList ) {
            Vector<Fingerprint> currentFP = new Vector<>();
            List<ScanResult> result = wifiManager.getScanResults();
            for( ScanResult r : result ) {
                if( r.frequency > 0 && r.level != 0 ) {
                    currentFP.add( new Fingerprint( r.BSSID , 
                                                    Math.abs( r.level ) , 
                                                    r.frequency ) );
                    // Add an operator friendly name for displaying on the GUI
                    if( android.os.Build.VERSION.SDK_INT >= 23 ) {
                        if( r.operatorFriendlyName != null ) {
                            operatorNameList.add( r.operatorFriendlyName.toString() );
                        }
                        else {
                            operatorNameList.add( "<no-name>" );
                        }
                    }
                }
            }
            return currentFP;
	}
    }

    /** Stops all sensor sampling. */
    private void stopSensorSampling() {

        // Stop sensor sampling
        if( sensorManager != null ) {
            sensorManager.unregisterListener( this );
            sensorManager = null;
        }
        // Stop location sampling
        if( locationManager != null ) {
            locationManager.removeUpdates( this );
            locationManager = null;
        }
        // Stop WiFi Scans
        if( wifiScanThread != null ) {
            // Interrupt any sleep() method
            wifiScanThread.interrupt();
            // Stop the looper thread itself
            wifiScanThread.stopWiFiScans();
            wifiScanThread = null;
            wifiManager = null;
        }
    }

    /** Sensor manager service. */
    private SensorManager sensorManager;
    /** Location manager service. */
    private LocationManager locationManager;
    /** WiFi manager service. */
    private WifiManager wifiManager;

    /** Thread that performs the WiFi scans. */
    private WiFiScanThread wifiScanThread;

    // Location sensor sampling rate (millis) (other sensors are sampled at the fastest rate, but logged at slower rate)
    private static final long SAMPLING_RATE_LOCATION = 10000L;     // 10 sec 
    // WiFi scan rate (millis) 
    private static final long SAMPLING_RATE_WIFI = 10000L;         // 10 sec 

    /** Called when the sensor value has changed. */
    @Override
    public void onSensorChanged( SensorEvent event ) {

        // Get the current timestamp
        long currentTime = System.currentTimeMillis();
        // Get the sensor event accuracy
        int accuracy = event.accuracy;

        // Check the sensor type
        if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
            // Get the sensor data
            float x = event.values[0] , 
                y = event.values[1] , 
                z = event.values[2];
            // Log the sensor data to the log file
            if( currentTime - prevLogTimeAccelerometer >= LOGGING_RATE_ACCELEROMETER ) {
                prevLogTimeAccelerometer = currentTime;
                String logLine = 
                    x + "," + y + "," + z + "," + accuracy;
                loggerAccelerometer.logEvent( logLine );
            }
            // Display the sensor data on the GUI
            if( currentTime - prevDisplayTimeAccelerometer >= DISPLAY_RATE_ACCELEROMETER ) {
                prevDisplayTimeAccelerometer = currentTime;
                String displayLine = 
                    "x: " + x + "\ny: " + y + "\nz: " + z + "\n";
                RealTimeDisplay.updateDisplay( DISPLAY_ACCELEROMETER , displayLine );
            }
        }
        else if( event.sensor.getType() == Sensor.TYPE_GRAVITY ) {
            // Get the sensor data
            float x = event.values[0] , 
                y = event.values[1] , 
                z = event.values[2];
            // Log the sensor data to the log file
            if( currentTime - prevLogTimeGravity >= LOGGING_RATE_GRAVITY ) {
                prevLogTimeGravity = currentTime;
                String logLine = 
                    x + "," + y + "," + z + "," + accuracy;
                loggerGravity.logEvent( logLine );
            }
            // Display the sensor data on the GUI
            if( currentTime - prevDisplayTimeGravity >= DISPLAY_RATE_GRAVITY ) {
                prevDisplayTimeGravity = currentTime;
                String displayLine = 
                    "x: " + x + "\ny: " + y + "\nz: " + z + "\n";
                RealTimeDisplay.updateDisplay( DISPLAY_GRAVITY , displayLine );
            }
        }
        else if( event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION ) {
            // Get the sensor data
            float x = event.values[0] , 
                y = event.values[1] , 
                z = event.values[2];
            // Log the sensor data to the log file
            if( currentTime - prevLogTimeLinearAccelerometer >= LOGGING_RATE_LINEAR_ACCL ) {
                prevLogTimeLinearAccelerometer = currentTime;
                String logLine = 
                    x + "," + y + "," + z + "," + accuracy;
                loggerLinearAccl.logEvent( logLine );
            }
            // Display the sensor data on the GUI
            if( currentTime - prevDisplayTimeLinearAccelerometer >= DISPLAY_RATE_LINEAR_ACCL ) {
                prevDisplayTimeLinearAccelerometer = currentTime;
                String displayLine = 
                    "x: " + x + "\ny: " + y + "\nz: " + z + "\n";
                RealTimeDisplay.updateDisplay( DISPLAY_LINEAR_ACCL , displayLine );
            }
        }
        else if( event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD ) {
            // Get the sensor data
            float x = event.values[0] , 
                y = event.values[1] , 
                z = event.values[2];
            // Log the sensor data to the log file
            if( currentTime - prevLogTimeMagnetic >= LOGGING_RATE_MAGNETIC ) {
                prevLogTimeMagnetic = currentTime;
                String logLine = 
                    x + "," + y + "," + z + "," + accuracy;
                loggerMagnetic.logEvent( logLine );
            }
            // Display the sensor data on the GUI
            if( currentTime - prevDisplayTimeMagnetic >= DISPLAY_RATE_MAGNETIC ) {
                prevDisplayTimeMagnetic = currentTime;
                String displayLine = 
                    "x: " + x + "\ny: " + y + "\nz: " + z + "\n";
                RealTimeDisplay.updateDisplay( DISPLAY_MAGNETIC , displayLine );
            }
        }
        else if( event.sensor.getType() == Sensor.TYPE_GYROSCOPE ) {
            // Get the sensor data
            float x = event.values[0] , 
                y = event.values[1] , 
                z = event.values[2];
            // Log the sensor data to the log file
            if( currentTime - prevLogTimeGyroscope >= LOGGING_RATE_GYROSCOPE ) {
                prevLogTimeGyroscope = currentTime;
                String logLine = 
                    x + "," + y + "," + z + "," + accuracy;
                loggerGyroscope.logEvent( logLine );
            }
            // Display the sensor data on the GUI
            if( currentTime - prevDisplayTimeGyroscope >= DISPLAY_RATE_GYROSCOPE ) {
                prevDisplayTimeGyroscope = currentTime;
                String displayLine = 
                    "x: " + x + "\ny: " + y + "\nz: " + z + "\n";
                RealTimeDisplay.updateDisplay( DISPLAY_GYROSCOPE , displayLine );
            }
        }
        else if( event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR ) {
            // Get the sensor data
            float x = event.values[0] , 
                y = event.values[1] , 
                z = event.values[2] , 
                scalar = ( event.values.length > 3 ? event.values[3] : 0.0F );
            // Log the sensor data to the log file
            if( currentTime - prevLogTimeRotationVector >= LOGGING_RATE_ROTATION_VECTOR ) {
                prevLogTimeRotationVector = currentTime;
                String logLine = 
                    x + "," + y + "," + z + "," + scalar + "," + accuracy;
                loggerRotationVector.logEvent( logLine );
            }
            // Display the sensor data on the GUI
            if( currentTime - prevDisplayTimeRotationVector >= DISPLAY_RATE_ROTATION_VECTOR ) {
                prevDisplayTimeRotationVector = currentTime;
                String displayLine = 
                    "x: " + x + "\ny: " + y + "\nz: " + z + "\nscalar: " + scalar + "\n";
                RealTimeDisplay.updateDisplay( DISPLAY_ROTATION_VECTOR , displayLine );
            }
        }
        else if( event.sensor.getType() == Sensor.TYPE_PRESSURE ) {
            // Get the sensor data
            float pressure = event.values[0];
            float altitude = 
                SensorManager.getAltitude( SensorManager.PRESSURE_STANDARD_ATMOSPHERE , 
                                           pressure );
            // Log the sensor data to the log file
            if( currentTime - prevLogTimeBarometer >= LOGGING_RATE_BAROMETER ) {
                prevLogTimeBarometer = currentTime;
                String logLine = 
                    pressure + "," + altitude + "," + accuracy;
                loggerBarometer.logEvent( logLine );
            }
            // Display the sensor data on the GUI
            if( currentTime - prevDisplayTimeBarometer >= DISPLAY_RATE_BAROMETER ) {
                prevDisplayTimeBarometer = currentTime;
                String displayLine = 
                    "pressure: " + pressure + "\naltitude: " + altitude + "\n";
                RealTimeDisplay.updateDisplay( DISPLAY_BAROMETER , displayLine );
            }
        }
        else if( event.sensor.getType() == Sensor.TYPE_LIGHT ) {
            // Get the sensor data
            float light = event.values[0];
            // Log the sensor data to the log file
            if( currentTime - prevLogTimeLight >= LOGGING_RATE_LIGHT ) {
                prevLogTimeLight = currentTime;
                String logLine = 
                    light + "," + accuracy;
                loggerLight.logEvent( logLine );
            }
            // Display the sensor data on the GUI
            if( currentTime - prevDisplayTimeLight >= DISPLAY_RATE_LIGHT ) {
                prevDisplayTimeLight = currentTime;
                String displayLine = 
                    "light: " + light + "\n";
                RealTimeDisplay.updateDisplay( DISPLAY_LIGHT , displayLine );
            }
        }
        else if( event.sensor.getType() == Sensor.TYPE_PROXIMITY ) {
            // Get the sensor data
            float proximity = event.values[0];
            // Log the sensor data to the log file
            if( currentTime - prevLogTimeProximity >= LOGGING_RATE_PROXIMITY ) {
                prevLogTimeProximity = currentTime;
                String logLine = 
                    proximity + "," + accuracy;
                loggerProximity.logEvent( logLine );
            }
            // Display the sensor data on the GUI
            if( currentTime - prevDisplayTimeProximity >= DISPLAY_RATE_PROXIMITY ) {
                prevDisplayTimeProximity = currentTime;
                String displayLine = 
                    "proximity: " + proximity + "\n";
                RealTimeDisplay.updateDisplay( DISPLAY_PROXIMITY , displayLine );
            }
        }
        else {
            // Ignore, unknown sensor type
            return;
        }
    }

    /** Called when the sensor's accuracy changes. */
    @Override
    public void onAccuracyChanged( Sensor sensor , 
                                   int accuracy ) {
        // Ignore
    }

    /** Called when the location has changed. */
    @Override
    public void onLocationChanged( Location location ) {

        // Get the current timestamp
        long currentTime = System.currentTimeMillis();
        // Get the location timestamp (may be much older than the current timestamp)
        long locationTime = location.getTime();

        // Get the location details
        String provider = location.getProvider();
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float accuracy = location.getAccuracy();
        double altitude = ( location.hasAltitude() ? 
                            location.getAltitude() : -1.0 );
        float bearing = ( location.hasBearing() ? 
                          location.getBearing() : -1.0F );
        float speed = ( location.hasSpeed() ? 
                        location.getSpeed() : -1.0F );

        // Log the location data to the log file
        if( currentTime - prevLogTimeLocation >= LOGGING_RATE_LOCATION ) {
            prevLogTimeLocation = currentTime;
            String logLine = 
                locationTime + "," + 
                sdf.format( new Date( locationTime ) ) + "," + 
                provider + "," + 
                latitude + "," + 
                longitude + "," + 
                accuracy + "," + 
                altitude + "," + 
                bearing + "," + 
                speed;
            loggerLocation.logEvent( logLine );
        }

        // Display the location data on the GUI
        if( currentTime - prevDisplayTimeLocation >= DISPLAY_RATE_LOCATION ) {
            prevDisplayTimeLocation = currentTime;
            String displayLine = 
                "provider: " + provider + "\n" + 
                "latitude: " + latitude + "\n" + 
                "longitude: " + longitude + "\n" + 
                "altitude: " + altitude + "\n" + 
                "speed: " + speed + "\n";
            RealTimeDisplay.updateDisplay( DISPLAY_LOCATION , displayLine );
        }
    }

    /** Called when a location provider is disabled. */
    @Override
    public void onProviderDisabled( String provider ) {
        // Ignore
    }

    /** Called when a location provider is enabled. */
    @Override
    public void onProviderEnabled( String provider ) {
        // Ignore
    }

    /** Called when a location provider's status has changed. */
    @Override
    public void onStatusChanged( String provider , 
                                 int status , 
                                 Bundle extras ) {
        // Ignore
    }

    // Sensor logging rates (millis)
    private static final long LOGGING_RATE_LOCATION = 5000L;       // 5 sec (but sampling is 10 sec)
    private static final long LOGGING_RATE_ACCELEROMETER = 25L;    // 40 Hz
    private static final long LOGGING_RATE_GRAVITY = 25L;          // 40 Hz
    private static final long LOGGING_RATE_LINEAR_ACCL = 25L;      // 40 Hz
    private static final long LOGGING_RATE_MAGNETIC = 25L;         // 40 Hz
    private static final long LOGGING_RATE_GYROSCOPE = 25L;        // 40 Hz
    private static final long LOGGING_RATE_ROTATION_VECTOR = 25L;  // 40 Hz
    private static final long LOGGING_RATE_BAROMETER = 1000L;      // 1 Hz
    private static final long LOGGING_RATE_LIGHT = 100L;           // 10 Hz
    private static final long LOGGING_RATE_PROXIMITY = 0L;         // Fastest (since it is triggered on change only)

    // Time (UNIX millis) when the last sensor data was logged
    private long prevLogTimeLocation;
    private long prevLogTimeAccelerometer;
    private long prevLogTimeGravity;
    private long prevLogTimeLinearAccelerometer;
    private long prevLogTimeMagnetic;
    private long prevLogTimeGyroscope;
    private long prevLogTimeRotationVector;
    private long prevLogTimeBarometer;
    private long prevLogTimeLight;
    private long prevLogTimeProximity;

    // Sensor display (on the GUI) rates (millis)
    private static final long DISPLAY_RATE_LOCATION = 5000L;       // 5 sec (but sampling is 10 sec)
    private static final long DISPLAY_RATE_ACCELEROMETER = 100L;   // 10 Hz
    private static final long DISPLAY_RATE_GRAVITY = 100L;         // 10 Hz
    private static final long DISPLAY_RATE_LINEAR_ACCL = 100L;     // 10 Hz
    private static final long DISPLAY_RATE_MAGNETIC = 100L;        // 10 Hz
    private static final long DISPLAY_RATE_GYROSCOPE = 100L;       // 10 Hz
    private static final long DISPLAY_RATE_ROTATION_VECTOR = 100L; // 10 Hz
    private static final long DISPLAY_RATE_BAROMETER = 1000L;      // 1 Hz
    private static final long DISPLAY_RATE_LIGHT = 100L;           // 10 Hz
    private static final long DISPLAY_RATE_PROXIMITY = 0L;         // Fastest (since it is triggered on change only)

    // Time (UNIX millis) when the last sensor data was displayed
    private long prevDisplayTimeLocation;
    private long prevDisplayTimeAccelerometer;
    private long prevDisplayTimeGravity;
    private long prevDisplayTimeLinearAccelerometer;
    private long prevDisplayTimeMagnetic;
    private long prevDisplayTimeGyroscope;
    private long prevDisplayTimeRotationVector;
    private long prevDisplayTimeBarometer;
    private long prevDisplayTimeLight;
    private long prevDisplayTimeProximity;

    /** Helper method to register sensor displays. */
    private void registerSensorDisplays() {
        RealTimeDisplay.addDisplay( DISPLAY_LOCATION , "provider: \nlatitude: \nlongitude: \naltitude: \nspeed: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_ACCELEROMETER , "x: \ny: \nz: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_GRAVITY , "x: \ny: \nz: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_LINEAR_ACCL , "x: \ny: \nz: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_MAGNETIC , "x: \ny: \nz: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_GYROSCOPE , "x: \ny: \nz: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_ROTATION_VECTOR , "x: \ny: \nz: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_BAROMETER , "pressure: \naltitude: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_LIGHT , "light: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_PROXIMITY , "proximity: \n" );
        RealTimeDisplay.addDisplay( DISPLAY_WIFI , "fingerprint: \n" );
    }

    // Sensor real-time displays
    private static final String DISPLAY_LOCATION = "LOCATION";
    private static final String DISPLAY_ACCELEROMETER = "ACCELEROMETER";
    private static final String DISPLAY_GRAVITY = "GRAVITY";
    private static final String DISPLAY_LINEAR_ACCL = "LINEAR ACCL";
    private static final String DISPLAY_MAGNETIC = "MAGNETIC";
    private static final String DISPLAY_GYROSCOPE = "GYROSCOPE";
    private static final String DISPLAY_ROTATION_VECTOR = "ROTATION VECTOR";
    private static final String DISPLAY_BAROMETER = "BAROMETER";
    private static final String DISPLAY_LIGHT = "LIGHT";
    private static final String DISPLAY_PROXIMITY = "PROXIMITY";
    private static final String DISPLAY_WIFI = "WIFI";

    /** Binder class for Service's API. */
    private final IBinder binder = new SensorLogAppServiceApi();

    /** Flag to indicate that the service is in foreground. */
    private volatile boolean isForeground = false;

    /** To format the UNIX millis time as a human-readable string. */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd-h-mm-ssa" );

    /** Handler to the main thread. */
    private Handler mainHandler;

    /** Lock tag. */
    private String lockTag = "SensorLogAppServiceLock";
    /** Wake lock for keeping sensor sampling on. */
    private PowerManager.WakeLock partialWakeLock;
    /** WiFi lock for keeping the WiFi interface on. */
    private WifiManager.WifiLock wifiLock;
    /** DDMS log tag. */
    private static final String TAG = "SensorLogApp";
}
