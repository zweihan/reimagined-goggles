package nus.cs4222.sensorlogapp;

import java.util.*;
import java.text.*;

import android.app.*;
import android.os.*;
import android.content.*;
import android.widget.*;
import android.util.*;
import android.view.*;
import android.preference.*;
import android.graphics.drawable.*;

/**
   Activity that displays the sensor data values.
 */
public class SensorLogAppActivity 
    extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // Get a handler to the main UI thread
        handler = new Handler();

        try {

            // Set up the GUI
            setUpGui();

            // Start the service (foreground service)
            startSensorLogService();
            // Bind to the service
            bindToSensorLogService();

            // Register a listener for real-time sensor data displays
            registerDisplayListener();
        }
        catch( Exception e ) {
            // Log the exception
            Log.e ( TAG , "Unable to start service" , e );
            // Display a toast to the user
            createToast( "Unable to start service" );
        }
    }

    /** Called when the activity is destroyed. */
    @Override
    public void onDestroy() {
        super.onDestroy();

        try {

            // Remove the display listener
            RealTimeDisplay.removeDisplayListener( displayListener );

            // Un-Bind from the service
            unbindFromSensorLogService();
            // Stop the service (if the data collection has stopped)
            stopSensorLogService();
        }
        catch ( Exception e ) {
            // Log the exception
            Log.e ( TAG , "Unable to destroy activity" , e );
            // Tell the user
            createToast ( "Unable to destroy activity" );
        }
    }

    /** Called when the application loses focus. */
    @Override
    protected void onPause() {
        super.onPause();

        try {

            // Save the GUI state
            saveGuiState();
        }
        catch( Exception e ) {
            // Log the exception
            Log.e ( TAG , "Unable to pause activity" , e );
            // Tell the user
            createToast ( "Unable to pause activity, check error log" );
        }
    }

    /** Helper method to set up the GUI. */
    private void setUpGui() {
        setContentView( R.layout.main );

        // Get a reference to the main layout where we will add buttons
        mainLayout = (ViewGroup) findViewById( R.id.main_layout );
        // Get a reference to the start and stop buttons
        startButton = (Button) findViewById( R.id.start_button );
        stopButton = (Button) findViewById( R.id.stop_button );
        // Get an XML inflater to inflate XML into GUI widget objects
        inflater = (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        // Add activity buttons for logging ground truth
        addGroundTruthButtons();

        // Set the start and stop button listeners
        startButton.setOnClickListener( new View.OnClickListener() {
                public void onClick( View view ) {
                    try {
                        // Create a timestamped log name
                        String logName = sdf.format( new Date( System.currentTimeMillis() ) );
                        // Start the logging with the current timestamp
                        serviceApi.startDataCollection( logName );
                        // Set the button states
                        startButton.setEnabled( false );
                        stopButton.setEnabled( true );
                        hasDataCollectionStarted = true;
                    }
                    catch( Exception e ) {
                        // Log the exception
                        Log.e ( TAG , "Unable to start data collection" , e );
                        // Inform the user
                        createToast( "Exception while starting data collection" );
                    }
                }
            } );
        stopButton.setOnClickListener( new View.OnClickListener() {
                public void onClick( View view ) {
                    // Set the button states
                    hasDataCollectionStarted = false;
                    startButton.setEnabled( true );
                    stopButton.setEnabled( false );
                    confirmButton.setEnabled( false );
                    if( prevButton != null ) {
                        prevButton.setBackground( prevButtonBackground );
                    }
                    // Stop data collection
                    serviceApi.stopDataCollection();
                }
            } );

        // Loads the saved GUI state (or default state for the first launch)
        loadGuiState();
    }

    /** Saves the GUI state. */
    private void saveGuiState() {

        // Use shared preferences to save the GUI state
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // TODO: What if the service crashes or the shared preferences are cleared?

        // Store the GUI state using key-value pairs
        // Whether the data collection has started or not
        editor.putBoolean( "data_collection_started" , hasDataCollectionStarted );
        // Whether the confirm button is enabled
        editor.putBoolean( "confirm_button_enabled" , confirmButton.isEnabled() );
        // The last activity that was logged
        if( hasDataCollectionStarted ) {
            boolean groundTruthPressed = ( prevButton != null );
            editor.putBoolean( "ground_truth_pressed" , groundTruthPressed );
            if( groundTruthPressed ) {
                editor.putString( "last_ground_truth" , ( (UserActivities) prevButton.getTag() ).toString() );
                editor.putLong( "last_timestamp" , prevTimestamp );
            }
        }
        else {
            editor.putBoolean( "ground_truth_pressed" , false );
        }

        // Commit (store) the key-value pairs
        // UPDATE: While commit() is synchronous and eats up UI thread time, 
        //  apply() is async and is performed in another thread.
        //editor.commit();
        editor.apply();
    }

    /** Loads the saved GUI state (or default state for the very first launch). */
    private void loadGuiState() {

        // Load the saved GUI state from shared preferences
        // NOTE: getPreferences() accesses the flash, which may be slow, 
        //  making the UI appear slow.
        //  So, once called in loadGuiState(), save a reference to the
        //  preferences object, avoiding the call to getPreferences() in saveGuiState().
        //  See: http://stackoverflow.com/questions/4371273/should-accessing-sharedpreferences-be-done-off-the-ui-thread
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

        // Get the last saved GUI state
        // Whether the data collection was started or not
        hasDataCollectionStarted = 
            sharedPreferences.getBoolean( "data_collection_started" , false );
        if( hasDataCollectionStarted ) {
            startButton.setEnabled( false );
            stopButton.setEnabled( true );
        }
        else {
            startButton.setEnabled( true );
            stopButton.setEnabled( false );
        }

        // Whether the confirm button is enabled
        boolean isConfirmButtonEnabled = 
            sharedPreferences.getBoolean( "confirm_button_enabled" , false );
        confirmButton.setEnabled( isConfirmButtonEnabled );

        // Mark the last activity that was logged
        if( hasDataCollectionStarted ) {
            boolean groundTruthPressed = 
                sharedPreferences.getBoolean( "ground_truth_pressed" , false );
            if( groundTruthPressed ) {
                String activityStr = 
                    sharedPreferences.getString( "last_ground_truth" , "IDLE_INDOOR" );
                prevTimestamp = sharedPreferences.getLong( "last_timestamp" , System.currentTimeMillis() );
                UserActivities activity = UserActivities.valueOf( activityStr );
                prevButton = contextToButtonMap.get( activity );
                prevButtonBackground = stopButton.getBackground();
                if( isConfirmButtonEnabled ) {
                    prevButton.setBackgroundColor( chosenButtonColor );
                }
                else {
                    prevButton.setBackgroundColor( loggedButtonColor );
                }
            }
        }

        // Set the button states based on data collection and confirm enabled
    }

    /** Helper method to add ground truth buttons for every activity. */
    private void addGroundTruthButtons() {

        // Create a ground truth button for each activity
        UserActivities activities[] = UserActivities.values();
        for ( UserActivities activity : activities ) {

            // Inflate the ground truth button from the xml file
            // CAUTION: If you specify the parent argument here,
            //          then view group gets returned
            //          instead of the inflated view!
            View groundTruthView = 
                inflater.inflate( R.layout.ground_truth_button , 
                                  null );
            // Attach the inflated view to the main layout view group
            mainLayout.addView( groundTruthView );
            // Get a reference to the button
            Button button = (Button) groundTruthView.findViewById( R.id.ground_truth_button );
            // Tag the button with the activity type
            button.setTag( activity );
            // Set the text for the button as the activity name
            button.setText( activity.toString() );
            // Add the button to a map
            contextToButtonMap.put( activity , button );
            // Save a reference to the confirm button
            if( activity.equals( UserActivities.Confirm ) ) {
                confirmButton = button;
            }

            // Add a button listener to log ground truth
            button.setOnClickListener( new View.OnClickListener() {
                    public void onClick( View view ) {

                        // If the data collection has not started, then do nothing
                        if( ! hasDataCollectionStarted ) {
                            createToast( "Start the data collection before logging ground truth" );
                            return;
                        }

                        // Get the activity for this button
                        UserActivities activity = (UserActivities) view.getTag();

                        // If the user pressed the confirm button, then log the ground truth
                        if( activity.equals( UserActivities.Confirm ) ) {

                            // Check if an activity button was clicked previously
                            if( prevButton == null ) {
                                createToast( "Choose the current activity before confirming to log" );
                                return;
                            }

                            // Set the previously chosen button to a new color to indicate that it is logged
                            prevButton.setBackgroundColor( loggedButtonColor );
                            // Write to log file (with the appropriate timestamp)
                            UserActivities loggedActivity = (UserActivities) prevButton.getTag();
                            serviceApi.logGroundTruth( prevTimestamp , loggedActivity );
                            // Disable the confirm button
                            confirmButton.setEnabled( false );
                            // Inform the user
                            createToast( "Logged user activity: " + loggedActivity.toString() );
                            return;
                        }

                        // If the user clicked an activity button, then need to confirm before logging
                        // Also, need to set the button colour
                        // First, set the previous button to the old background
                        if( prevButton != null ) {
                            prevButton.setBackground( prevButtonBackground );
                        }
                        // Second, save the chosen button and its background with the timestamp
                        Button clickedButton = (Button) view;
                        prevButton = clickedButton;
                        prevButtonBackground = clickedButton.getBackground();
                        prevTimestamp = System.currentTimeMillis();
                        // Third, set the color of the clicked button (alphaRGB)
                        clickedButton.setBackgroundColor( chosenButtonColor );
                        // Fourth, enable the confirm button
                        confirmButton.setEnabled( true );
                        // Fifth, inform the user
                        createToast( "Press the CONFIRM button to log the ground truth" );
                    }
                } );
        }

        // Invalidate the main layout, it must be re-drawn
        mainLayout.invalidate();
    }

    /** Helper method to start the service. */
    private void startSensorLogService() {

        // Start the service (foreground service)
        Intent intent = new Intent( this , SensorLogAppService.class );
        startService( intent );
    }

    /** Helper method to stop the service. */
    private void stopSensorLogService() {

        if( ! hasDataCollectionStarted ) {

            // Stop the service
            Intent intent = new Intent( this , SensorLogAppService.class );
            stopService( intent );
        }
    }

    /** Helper method to bind to the service. */
    private void bindToSensorLogService() {
        if( ! isBound ) {
            // Bind to the service
            Intent intent = new Intent( this , SensorLogAppService.class );
            bindService( intent , connection , Context.BIND_AUTO_CREATE );
        }
    }

    /** Helper method to unbind from the service. */
    private void unbindFromSensorLogService() {
        if( isBound ) {
            // Un-Bind from the service
            isBound = false;
            unbindService( connection );
            serviceApi = null;
        }
    }

    /** Connection to bound service. */
    private ServiceConnection connection = new ServiceConnection() {

            /** Called when connection made to the service. */
            @Override
            public void onServiceConnected( ComponentName className , 
                                            IBinder binder ) {

                // Get the service's API
                serviceApi = (SensorLogAppService.SensorLogAppServiceApi) binder;
                // Set flag
                isBound = true;
            }

            /** Called when the service crashes. */
            @Override
            public void onServiceDisconnected( ComponentName className ) {
                // Set flag
                isBound = false;
            }
        };

    /** Helper method that registers a display listener. */
    private void registerDisplayListener() {

        // Create a display listener
        displayListener = 
            new RealTimeDisplay.DisplayListener() {
                /** {@inheritDoc} */
                public void onDisplayAdded( final String displayName , 
                                            final String initialText ) {
                    addDisplayView( displayName , initialText );
                }
                /** {@inheritDoc} */
                public void onDisplayUpdated( final String displayName , 
                                              final String updatedText ) {
                    updateDisplayView( displayName , updatedText );
                }
            };

        // Register the display listener
        List< String > displayList = 
            RealTimeDisplay.getDisplayList( displayListener );

        // For the displays already added before, 
        //  create the required display views.
        for( String displayName : displayList ) {
            addDisplayView( displayName , 
                            RealTimeDisplay.getDisplayText( displayName ) );
        }
    }

    /** Helper method that creates display views. */
    private void addDisplayView( String displayName , 
                                 String initialText ) {

        // Create a real-time display view by inflating 
        //  from the xml file
        // CAUTION: If you specify the parent here,
        //          then the view group gets returned
        //          instead of the inflated view!
        View displayView = 
            inflater.inflate( R.layout.real_time_display , 
                              null );
        // Attach the inflated display view as a child 
        //  to the main layout view group
        mainLayout.addView( displayView );

        // Get references to the textview in the display view
        TextView displayText = 
            (TextView) displayView.findViewById( R.id.real_time_display );
        // Set the initial text for this display
        displayText.setText( displayName + "\n" + initialText );

        // Add the display and its text view to the map
        displayMap.put( displayName , displayText );

        // Invalidate the main layout, it must be re-drawn
        mainLayout.invalidate();
    }

    /** Helper method that updates display views. */
    private void updateDisplayView( String displayName , 
                                    String updatedText ) {

        // Get the text view for this display name
        TextView displayText = displayMap.get( displayName );

        // Update the text view
        // BUG: Race condition here, displayText is null on exit,
        //      need to check if null
        if( displayText != null ) {
            displayText.setText( displayName + "\n" + updatedText );
        }
    }

    /** Helper method to create toasts. */
    private void createToast( final String toastMessage ) {

        // Post a runnable in the Main UI thread
        handler.post( new Runnable() {
                @Override
                public void run() {
                    Toast.makeText( getApplicationContext() , 
                                    toastMessage , 
                                    Toast.LENGTH_SHORT ).show();
                }
            } );
    }

    /** Main layout group. */
    private ViewGroup mainLayout;
    /** XML inflater. */
    private LayoutInflater inflater;

    /** Previous button that user clicked. */
    private Button prevButton = null;
    /** Previous button background. */
    private Drawable prevButtonBackground = null;
    /** Timestamp when previous button was clicked. */
    private long prevTimestamp;
    /** Chosen button color. */
    private int chosenButtonColor = 0xAAFF0000;
    /** Logged button color. */
    private int loggedButtonColor = 0xAA00FF00;

    /** Display Listener. */
    private RealTimeDisplay.DisplayListener displayListener;
    /** Map of Display name {key} ==> Text View {value}. */
    private Map< String , TextView > displayMap = 
        new HashMap< String , TextView >();

    /** Confirm button. */
    private Button confirmButton;
    /** Map of Context state {key} ==> Corresponding button {value}. */
    private Map< UserActivities , Button > contextToButtonMap = 
        new HashMap< UserActivities , Button >();

    /** Start logging button. */
    private Button startButton;
    /** Stop logging button. */
    private Button stopButton;

    /** Flag to indicate whether the data collection has stopped or not. */
    private boolean hasDataCollectionStarted = false;
    /** Flag to indicate whether we are bound to service. */
    private boolean isBound = false;
    /** Service's API. */
    private SensorLogAppService.SensorLogAppServiceApi serviceApi = null;

    /** To format the UNIX millis time as a human-readable string. */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd-h-mm-ssa" );

    /** Shared preferences (to save app state). */
    private SharedPreferences sharedPreferences;

    /** Handler to the main thread. */
    private Handler handler;
    /** DDMS log tag. */
    private static final String TAG = "SensorLogApp";
}
