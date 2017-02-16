package nus.cs4222.shootingapp;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.SoundPool;
import android.media.AudioManager;
import android.util.Log;

/**
   Activity that detects a simple gesture by the user.

   <p> This activity currently uses 2 sensors: the linear accl and the
   gravity sensors. First, it uses the linear accl sensor to detect a
   'shooting' gesture. Second, it uses the gravity sensor to make sure
   the phone is face up and (almost) parallel to the ground while the
   gesture is performed. You can use any other sensors (except the
   deprecated TYPE_ORIENTATION) to detect the shooting direction and
   region.

   <p> Modify the MIN_ACCL_FORCE value to a value suitable for your
   phone.

   @author     Kartik Sankaran
 */
public class ShootingAppActivity 
    extends Activity 
    implements SensorEventListener , 
               SoundPool.OnLoadCompleteListener {

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // Get a handler to the main thread (for posting toast messages)
        handler = new Handler();

        try {

            // Initialise the GUI
            initGui();

            // Initialise sensor manager and sensors
            initSensors();
        }
        catch( Exception e ) {
            // Log the exception
            Log.e( TAG , "Unable to start activity" , e );
            // Tell the user
            createToast( "Unable to start activity, check error log" );
        }
    }

    /** Called when the activity is destroyed. */
    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            // Nothing to do here yet...
        }
        catch( Exception e ) {
            // Log the exception
            Log.e( TAG , "Unable to destroy activity" , e );
            // Tell the user
            createToast( "Unable to destroy activity, check error log" );
        }
    }

    /** Called when the activity is resumed. */
    @Override
    public void onResume() {
        super.onResume();

        try {

            // Start sensing the sensors
            startSensing();

            // Initialise the sound pool for sound effects
            initSound();
        }
        catch( Exception e ) {
            // Log the exception
            Log.e( TAG , "Unable to resume activity" , e );
            // Tell the user
            createToast( "Unable to resume activity, check error log" );
        }
    }

    /** Called when the activity is paused. */
    @Override
    public void onPause() {
        super.onPause();

        try {

            // De-initialise sound pool
            deinitSound();

            // Stop all sensing
            stopSensing();
        }
        catch( Exception e ) {
            // Log the exception
            Log.e( TAG , "Unable to pause activity" , e );
            // Tell the user
            createToast( "Unable to pause activity, check error log" );
        }
    }

    /** Initialises the GUI. */
    private void initGui() {

        // Set the main activity layout (given in main.xml)
        setContentView( R.layout.main );

        // Get references to the GUI widgets
        textView_Accl = (TextView) findViewById( R.id.TextView_Accl );
        textView_Gravity = (TextView) findViewById( R.id.TextView_Gravity );
        textView_PhoneGesture = (TextView) findViewById( R.id.TextView_PhoneGesture );
        textView_PhoneFaceUp = (TextView) findViewById( R.id.TextView_PhoneFaceUp );
        textView_PhoneShootingRegion = (TextView) findViewById( R.id.TextView_PhoneShootingRegion );
    }

    /** Initialises the linear accl and gravity sensors. */
    private void initSensors() 
        throws Exception {

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService( SENSOR_SERVICE );

        // Get references to the linear accl and gravity sensors
        acclSensor = sensorManager.getDefaultSensor( Sensor.TYPE_LINEAR_ACCELERATION );
        gravitySensor = sensorManager.getDefaultSensor( Sensor.TYPE_GRAVITY );
        // The regular acceleration sensor and magnetic sensor can be used to determine orientation
        accelerometerSensor = sensorManager.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
        magneticSensor = sensorManager.getDefaultSensor( Sensor.TYPE_MAGNETIC_FIELD );

        if( acclSensor == null ) {
            throw new Exception( "Oops, there is no linear accelerometer sensor on this device :(" );
        }
        else if( gravitySensor == null ) {
            throw new Exception( "Oops, there is no gravity sensor on this device :(" );
        }
    }

    /** Starts sampling the sensors. */
    private void startSensing() {

        // Initialise the sensor-related variables
        isFaceUp = false;
        numGestures = 0;
        shootingRegion = 1;
        shootingDirection = 0.0F;
        isAcclInPeakZone = false;

        // Start sampling the sensors
        sensorManager.registerListener( this ,                              // Listener
                                        acclSensor ,                        // Sensor to measure
                                        SensorManager.SENSOR_DELAY_GAME );  // Measurement interval (microsec)
        sensorManager.registerListener( this ,                              // Listener
                                        gravitySensor ,                     // Sensor to measure
                                        SensorManager.SENSOR_DELAY_GAME );  // Measurement interval (microsec)
        sensorManager.registerListener( this ,                              // Listener
                                        accelerometerSensor ,               // Sensor to measure
                                        SensorManager.SENSOR_DELAY_GAME );  // Measurement interval (microsec)
        sensorManager.registerListener( this ,                              // Listener
                                        magneticSensor ,                    // Sensor to measure
                                        SensorManager.SENSOR_DELAY_GAME );  // Measurement interval (microsec)
    }

    /** Stops all sensing. */
    private void stopSensing() {

        // Stop sampling all sensors
        sensorManager.unregisterListener( this );
    }

    /** Called when the sensor value has changed (not necessarily periodically). */
    public void onSensorChanged( SensorEvent event ) {

        // NOTE: Sensor callbacks are in the main UI thread, so do not
        //  do very long calculations here. A better approach would be
        //  to just store the values, and use a periodic timer to
        //  process them in another thread.

        // Case 1: Gravity sensor
        if( event.sensor.getType() == Sensor.TYPE_GRAVITY ) {
            processGravityValues( event );
        }
        // Case 2: Linear accl sensor
        else if( event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION ) {
            processAcclValues( event );
        }
        else if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
            detectShootingDirectionAndRegion( event );
        }
        else if( event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD ) {
            System.arraycopy(event.values, 0, magnometerReading, 0, magnometerReading.length);
            detectShootingDirectionAndRegion( event );
        }
    }

    /** Process the gravity sensor. */
    private void processGravityValues( SensorEvent event ) {

        // Use the gravity sensor to detect whether the phone is
        //  face up and nearly parallel to the ground.
        // When the phone is in this position, the gravity should be
        //  (0 , 0 , g). Otherwise, it is at an angle to the ground,
        //  which can be calculated as the angle between the sampled
        //  gravity vector and the (0 , 0 , 1) phone's z-axis.

        // Store the gravity readings
        float[] gravityValues = event.values.clone();

        // Positive z-axis (0, 0, 1)
        float[] zaxis = new float[] { 0.0F , 0.0F , 1.0F };

        // Calculate the angle between z-axis and gravity sensor vector.
        // NOTE: This calculation can be easily optimized since there
        //       are 0s and 1s, but is shown here in full for clarity.
        // Use Dot product formula to find angle between two vectors.
        float[] v1 = zaxis , v2 = gravityValues;
        float dotProduct =
            v1[0] * v2[0] +
            v1[1] * v2[1] +
            v1[2] * v2[2];
        float mag1 = (float) Math.sqrt( v1[0] * v1[0] +
                                        v1[1] * v1[1] +
                                        v1[2] * v1[2] );
        float mag2 = (float) Math.sqrt( v2[0] * v2[0] +
                                        v2[1] * v2[1] +
                                        v2[2] * v2[2] );
        float cosValue = dotProduct / ( mag1 * mag2 );
        // Clamp the cos value to [-1,1]
        if ( cosValue > 1.0F ) {
            cosValue = 1.0F;
        }
        else if ( cosValue < -1.0F ) {
            cosValue = -1.0F;
        }
        float radianAngle = (float) Math.acos( cosValue );
        // Convert to degrees
        float angle = (float) Math.toDegrees( radianAngle );

        // Check if the phone is face up
        if( angle <= MAX_FACE_UP_ANGLE_ERROR ) {
            isFaceUp = true;
        }
        else {
            isFaceUp = false;
        }

        // Update the GUI (at a slower rate easy for the user to see on screen)
        long currentTime = System.currentTimeMillis();
        if( currentTime - lastPhoneAngleTime > MAX_UPDATE_INTERVAL_PHONE_ANGLE ) {

            // Update the text view
            textView_Gravity.setText ( "\nGravity Sensor" +
                                       "\nX: " + gravityValues[0] +
                                       "\nY: " + gravityValues[1] +
                                       "\nZ: " + gravityValues[2] +
                                       "\nAngle of phone with horizontal plane: " + angle + " degrees" +
                                       "\nIs phone face up?: " + isFaceUp );
            textView_PhoneFaceUp.setText( "\nIs phone face up?: " + isFaceUp );

            // Set the last GUI update time
            lastPhoneAngleTime = currentTime;
        }
    }

    /** Detect the shooting direction and region. */
    private void detectShootingDirectionAndRegion( SensorEvent event ) {
        sensorManager.getRotationMatrix(rotationMatrix, null,
                                        accelerometerReading, magnometerReading);
        sensorManager.getOrientation(rotationMatrix, orientationAngles);

        // Convert the direction the user is facing to degrees away from North
        float shootingDirection = (float)Math.toDegrees(orientationAngles[0]);
        // Add 360 degrees to negative numbers so we get a range from 0-360
        if (shootingDirection < 0) {
            shootingDirection += 360;
        }

        shootingRegion = (int)Math.floor(shootingDirection/45) + 1;

        // PA3: After you have detected the shooting region, assign the
        //  region number (in the range 1 to 8) to the member variable
        //  'shootingRegion', and the shooting direction (in the range
        //  0 to 360 deg) to the member variable 'shootingDirection',
        //  both variables are defined at the end of the Java code.
        // The processAcclValues() method produces the gunshot sound
        //  based on the value of 'shootingRegion' (which is set to 1
        //  by default).
        // Also, display the shooting direction and the shooting region
        //  in the text view below.

        // Update the GUI (at a slower rate easy for the user to see on screen)
        long currentTime = System.currentTimeMillis();
        if( currentTime - lastPhoneDirectionTime > MAX_UPDATE_INTERVAL_PHONE_DIRECTION ) {

            // Update the text view
            textView_PhoneShootingRegion.setText( "\nShooting direction: " +
                                                    String.format("%.1f", shootingDirection) +
                                                    " degrees" +
                                                    "\nShooting region: " + shootingRegion );

            // Set the last GUI update time
            lastPhoneDirectionTime = currentTime;
        }
    }

    /** Process the linear accl sensor. */
    private void processAcclValues( SensorEvent event ) {

        // This uses the accl to detect if a 'shooting' gesture has been made
        //  (by moving the phone sharply upwards/downwards/forwards with large force
        //   while face up)

        // If the phone is not face up, then skip the calculation
        if( ! isFaceUp ) {
            return;
        }

        // We use only the accl's z-axis (of the phone's co-ordinate system).
        // You can visualize accl data using the Sensor Kinetics app in
        //  the Google play store.
        float zAccl = event.values[2];

        // NOTE: Smoothing may not be required for linear accl since it
        //  is typically a processed sensor. If not, then a bit of smoothing
        //  will be required to remove erroneous accl peaks. Note that smoothing
        //  would increase the latency, so too much smoothing is also not
        //  advisable.

        // Here we are using a simple ad-hoc technique utilizing two thresholds
        //  for gesture detection.
        //  More advanced approaches include machine learning and dynamic time
        //  warping for gesture recognition.

        // Check if the force exerted by the accl is large enough.
        // The thresholds used below should normally be based on data collected
        //  from different users performing the gesture, but for this assignment,
        //  you can manually set thresholds that work reasonably ok for your
        //  phone.
        if( zAccl >= MIN_ACCL_FORCE ) {

            // Since the accl can be noisy around the threshold (even with smoothing)
            //  we use a state machine to check if the accl has oscillated between
            //  two thresholds in the accl peak before we detect the gesture.
            if( ! isAcclInPeakZone ) {
                ++numGestures;
                isAcclInPeakZone = true;

                // Play gunshot sound according to
                //  the user's shooting direction (region).
                // If there are more shooting regions than gun types, then
                //  repeat gun types in more than one shooting region. Note
                //  that the shootingRegions are numbered from 1, and sound
                //  numbers are numbered from 0.
                int soundNumber = ( ( shootingRegion - 1 ) % soundResourceList.length );
                playSound( soundNumber );
            }
        }
        // Check if the accl has finished oscillating from the
        //  top threshold near the top of the peak to the bottom
        //  threshold at the bottom of the peak.
        else if( isAcclInPeakZone ) {
            if( zAccl <= MIN_ACCL_PEAK_TROUGH ) {
                isAcclInPeakZone = false;
            }
        }

        // Store the linear accl readings
        float[] acclValues = event.values.clone();

        // Update the GUI (at a slower rate easy for the user to see on screen)
        long currentTime = System.currentTimeMillis();
        if( currentTime - lastPhoneGestureTime > MAX_UPDATE_INTERVAL_PHONE_GESTURE ) {

            // Update the text view
            textView_Accl.setText ( "\nLinear Accelerometer Sensor" +
                                    "\nX: " + acclValues[0] +
                                    "\nY: " + acclValues[1] +
                                    "\nZ: " + acclValues[2] +
                                    "\nNumber of gestures: " + numGestures );
            textView_PhoneGesture.setText( "\nNumber of gestures: " + numGestures );

            // Set the last GUI update time
            lastPhoneGestureTime = currentTime;
        }

    }

    /** TODO */
    /** Called when the accuracy changes. */
    public void onAccuracyChanged ( Sensor sensor , int accuracy ) {
        // Ignore (except for magnetic sensor for figure 8 calibration)
    }

    /** Initialises the sound pool for sound effects. */
    private void initSound() {

        // Initialise sound-related variables
        areSoundsLoaded = false;
        numSoundsLoaded = 0;
        soundStreamIdList = new ArrayList< Integer >();

        // Create a sound pool (to play max 1 sound stream at a give time)
        soundPool = new SoundPool( 1 ,                           // Max number of streams playing at same time
                                   AudioManager.STREAM_MUSIC ,   // Stream type
                                   0 );                          // Source quality (unused, need to just pass 0)

        // After loading each sound file, this callback is invoked
        soundPool.setOnLoadCompleteListener( this );

        // Load the sound files one by one
        for( int soundResource : soundResourceList ) {
            int streamID = soundPool.load( this ,            // GUI Context
                                           soundResource ,   // Sound file resource ID (in res/raw/ folder)
                                           1 );              // Priority (unused, need to just pass 1)
            soundStreamIdList.add( streamID );
        }
    }

    /** De-initialises the sound pool. */
    private void deinitSound() {

        try {

            // Release all resources
            soundPool.release();
        }
        finally {

            // Set the objects to null
            soundPool = null;
            soundStreamIdList = null;
        }
    }

    /** Called when a sound file has been loaded. */
    @Override
    public void onLoadComplete( SoundPool pool ,
                                int sampleID ,
                                int status ) {

        // Check if the load was OK
        if( status != 0 ) {
            createToast( "Sorry, the sound effects could not be loaded" );
            return;
        }

        // Check if all sound files have been loaded
        ++numSoundsLoaded;
        if( numSoundsLoaded == soundResourceList.length ) {
            areSoundsLoaded = true;
        }
    }

    /** Plays a sound stream (sound number is in the range 0 .. soundResourceList.length-1). */
    private void playSound( int soundNumber ) {

        // Check if all the sound files are loaded
        if( ! areSoundsLoaded ) {
            return;
        }
        // Check if the sound number is valid
        else if( soundNumber >= soundResourceList.length ||
                 soundNumber < 0 ) {
            createToast( "Invalid sound number passed to playSound()" );
            return;
        }

        // Play the sound (according to the media volume settings set by the user)
        int streamID = soundStreamIdList.get( soundNumber );
        AudioManager audioManager = (AudioManager) getSystemService( Context.AUDIO_SERVICE );
        float curVolume = audioManager.getStreamVolume( AudioManager.STREAM_MUSIC );
        float maxVolume = audioManager.getStreamMaxVolume( AudioManager.STREAM_MUSIC );
        float leftVolume = curVolume / maxVolume;
        float rightVolume = curVolume / maxVolume;
        int priority = 1;
        int noLoop = 0;
        float normalPlaybackRate = 1.0F;
        soundPool.play( streamID ,
                        leftVolume ,
                        rightVolume ,
                        priority ,
                        noLoop ,
                        normalPlaybackRate );
    }

    /** Helper method to create toasts. */
    private void createToast( final String toastMessage ) {

        // Post a runnable in the Main UI thread
        handler.post( new Runnable() {
                @Override
                public void run() {
                    Toast.makeText ( getApplicationContext() ,
                                     toastMessage ,
                                     Toast.LENGTH_SHORT ).show();
                }
            } );
    }

    // Sampled Sensors
    /** Sensor manager. */
    private SensorManager sensorManager;
    /** (Linear) Accl sensor. */
    private Sensor acclSensor;
    /** Gravity sensor. */
    private Sensor gravitySensor;
    /** (Regular) Accl sensor. */
    private Sensor accelerometerSensor;
    /** Magnetometer Sensor */
    private Sensor magneticSensor;

    // Gravity sensor
    /** Last time the GUI was updated about phone angle (UNIX millisec). */
    private long lastPhoneAngleTime = 0L;
    /** Max delay before GUI is updated about phone angle (millisec). */
    private static final long MAX_UPDATE_INTERVAL_PHONE_ANGLE = 250L;
    /** Face up angle error allowed in deg (since it is difficult for the user to place phone at perfect 90 deg). */
    private static final float MAX_FACE_UP_ANGLE_ERROR = 30.0F;
    /** Flag to indicate whether the phone is face up (and nearly parallel to the ground). */
    private boolean isFaceUp;

    // Linear accl sensor
    /** Last time the GUI was updated about number of gestures (UNIX millisec). */
    private long lastPhoneGestureTime = 0L;
    /** Max delay before GUI is updated about the gestures (millisec). */
    private static final long MAX_UPDATE_INTERVAL_PHONE_GESTURE = 250L;
    /** Minimum gesture force (m/sec^2). */
    private static final float MIN_ACCL_FORCE = 7.0F;
    /** Minimum accl peak trough value (m/sec^2). */
    private static final float MIN_ACCL_PEAK_TROUGH = 1.0F;
    /** Flag to indicate if the accl is in the peak area. */
    private boolean isAcclInPeakZone;
    /** Number of gestures performed by the user. */
    private int numGestures;

    // Shooting direction and region
    /** Last time the GUI was updated about phone direction (UNIX millisec). */
    private long lastPhoneDirectionTime = 0L;
    /** Max delay before GUI is updated about phone direction (millisec). */
    private static final long MAX_UPDATE_INTERVAL_PHONE_DIRECTION = 250L;
    /** Number of shooting regions (in the 360 deg shooting region around the user). */
    private static final int NUM_SHOOTING_REGIONS = 8;
    /** Shooting direction the user is pointing at (in the range 0 .. 360 degrees). */
    private float shootingDirection;
    /** Shooting region the user is pointing at (numbered from 1 .. NUM_SHOOTING_REGIONS). */
    private int shootingRegion;
    /** Stores the readings from the accelerometer */
    private final float[] accelerometerReading = new float[3];
    /** Stores the readings from the magnometer */
    private final float[] magnometerReading = new float[3];
    /** The rotation matrix for computing orientation */
    private final float[] rotationMatrix = new float[9];
    /** The angles of orientation */
    private final float[] orientationAngles = new float[3];

    // GUI widgets
    /** Text view displaying the linear accl processing. */
    private TextView textView_Accl;
    /** Text view displaying the gravity processing. */
    private TextView textView_Gravity;
    /** Text view displaying the number of gestures. */
    private TextView textView_PhoneGesture;
    /** Text view displaying whether the phone is facing up. */
    private TextView textView_PhoneFaceUp;
    /** Text view displaying the shooting region. */
    private TextView textView_PhoneShootingRegion;

    // For sound effects (gunshots)
    // http://soundscrate.com/gun-related.html
    // http://www.findsounds.com/ISAPI/search.dll
    /** Sound pool for fast sound playback. */
    private SoundPool soundPool;
    /** List of sound stream IDs. */
    private ArrayList< Integer > soundStreamIdList;
    /** List of sound files resource IDs in the /res/raw resource folder. */
    private static final int[] soundResourceList = 
        new int[] { R.raw.rifle , 
                    R.raw.missile1 , 
                    R.raw.handgun , 
                    R.raw.machinegun , 
                    R.raw.torpedo , 
                    R.raw.rocket1 , 
                    R.raw.rifle2 , 
                    R.raw.weirdmachinegun };
    /** Flag to indicate whether all the sound files are loaded. */
    private boolean areSoundsLoaded;
    /** Number of sound files loaded. */
    private int numSoundsLoaded;

    // For DDMS Logging and Toasts
    /** Handler to the main thread. */
    private Handler handler;
    /** TAG used for ddms logging. */
    private static final String TAG = "ShootingApp";
}
