package nus.cs4222.activitytrackapp;

import java.util.*;

/**
   For displaying real-time sensor data on the GUI.

   <p> The GUI can listen for added displays and display updates
   by registering a {@code DisplayListener} using the 
   {@link #getDisplayList(DisplayListener)} method.

   <p> The service can add a display using the 
   {@link #addDisplay(String,String)} method, and update it using
   the {@link #updateDisplay(String,String)} method.
 */
public class RealTimeDisplay {

    /** Adds a new real-time display to the GUI. */
    public synchronized static void 
        addDisplay( String displayName , String initialText ) {

        // Add the display to the list of displays
        displayMap.put( displayName , initialText );

        // Call the display listener
        if( displayListener != null ) {
            displayListener.onDisplayAdded( displayName , initialText );
        }
    }

    /** Clears all added displays (for cleanup). */
    public synchronized static void
        clearDisplays() {

        // Clear the display list
        displayMap.clear();
        // Clear the listener
        displayListener = null;
    }

    /** Updates a display's text. */
    public synchronized static void
        updateDisplay( String displayName , String updatedText ) {

        // Update the text in the display map
        displayMap.put( displayName , updatedText );

        // Call the display listener
        if( displayListener != null ) {
            displayListener.onDisplayUpdated( displayName , updatedText );
        }
    }

    /** Gets a snapshot of the display list, and adds the listener. */
    public synchronized static List< String > 
        getDisplayList( DisplayListener listener ) {

        // Add the listener
        displayListener = listener;

        // Return a snapshot of the display list
        return new LinkedList< String >( displayMap.keySet() );
    }

    /** Removes a display listener. */
    public synchronized static void 
        removeDisplayListener( DisplayListener displayListener ) {

        // Remove the listener
        displayListener = null;
    }

    /** Gets the text in a display. */
    public synchronized static String 
        getDisplayText( String displayName ) {
        return displayMap.get( displayName );
    }

    /** Listener for real-time display additions/updates. */
    public static interface DisplayListener {
        /** Called when a real-time display is added. */
        void onDisplayAdded( String displayName , String initialText );
        /** Called when a real-time display is updated. */
        void onDisplayUpdated( String displayName , String updatedText );
    }

    /** Map of Display names {Key} ==> Displayed Text {value}. */
    private static Map< String , String > displayMap = 
        new LinkedHashMap< String , String >();
    /** Listener for real-time display additions/updates. */
    private static DisplayListener displayListener = null;
}
