package android.util;

/**
   Dummy Android Log class.

   @author   Kartik S
   @version  1.0.0, 1st July 2013
   @since    1.0.0, 1st July 2013
 */
public class Log {

    /** Verbose level. */
    public static final int VERBOSE = 2;
    /** Debug level. */
    public static final int DEBUG = 3;
    /** Info level. */
    public static final int INFO = 4;
    /** Warning level. */
    public static final int WARN = 5;
    /** Error level. */
    public static final int ERROR = 6;
    /** Assert level. */
    public static final int ASSERT = 7;

    /** Create a VERBOSE log. */
    public static int v( String tag , 
                         String message ) {
        logToConsole( "VERBOSE" , tag , message , null );
        return 0;
    }

    /** Create a VERBOSE log with exception. */
    public static int v( String tag , 
                         String message , 
                         Throwable throwable ) {
        logToConsole( "VERBOSE" , tag , message , throwable );
        return 0;
    }

    /** Create a DEBUG log. */
    public static int d( String tag , 
                         String message ) {
        logToConsole( "DEBUG" , tag , message , null );
        return 0;
    }

    /** Create a DEBUG log with exception. */
    public static int d( String tag , 
                         String message , 
                         Throwable throwable ) {
        logToConsole( "DEBUG" , tag , message , throwable );
        return 0;
    }

    /** Create an INFO log. */
    public static int i( String tag , 
                         String message ) {
        logToConsole( "INFO" , tag , message , null );
        return 0;
    }

    /** Create an INFO log with exception. */
    public static int i( String tag , 
                         String message , 
                         Throwable throwable ) {
        logToConsole( "INFO" , tag , message , throwable );
        return 0;
    }

    /** Create a WARNING log. */
    public static int w( String tag , 
                         String message ) {
        logToConsole( "WARNING" , tag , message , null );
        return 0;
    }

    /** Create a WARNING log with exception. */
    public static int w( String tag , 
                         String message , 
                         Throwable throwable ) {
        logToConsole( "WARNING" , tag , message , throwable );
        return 0;
    }

    /** Create an ERROR log. */
    public static int e( String tag , 
                         String message ) {
        logToConsole( "ERROR" , tag , message , null );
        return 0;
    }

    /** Create an ERROR log with exception. */
    public static int e( String tag , 
                         String message , 
                         Throwable throwable ) {
        logToConsole( "ERROR" , tag , message , throwable );
        return 0;
    }

    /** Lower-level logging method. */
    public static int println( int level , 
                               String tag , 
                               String message ) {

        // Convert the 'int' log level to 'string'
        String levelString = null;
        switch ( level ) {
        case VERBOSE: levelString = "VERBOSE";
        case DEBUG:   levelString = "DEBUG";
        case INFO:    levelString = "INFO";
        case WARN:    levelString = "WARNING";
        case ERROR:   levelString = "ERROR";
        case ASSERT:  levelString = "ASSERT";
        }

        // Log to console
        logToConsole( levelString , tag , message , null );

        // Returns number of bytes written, but in this we just return 0
        return 0;
    }

    /** Gets a loggable stack trace string. */
    public static String getStackTraceString( Throwable throwable ) {

        // Build the Stack trace string
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] backtrace = throwable.getStackTrace();
        String tab = "    ";
        for ( StackTraceElement ste : backtrace ) {
            sb.append( tab );
            sb.append( tab );
            sb.append( ste.toString() );
            sb.append( "\n" );
        }

        // Return it
        return sb.toString();
    }

    /** Helper method to log onto the stdout (typically console). */
    private static void logToConsole( String level ,
                                      String tag , 
                                      String message , 
                                      Throwable throwable ) {

        // Get the log string
        String logString = createLogString( level , 
                                            tag , 
                                            message , 
                                            throwable );

        // Log to the console
        System.out.print( logString );
    }

    /** Helper method to create a loggable string. */
    private static String createLogString( String level , 
                                           String tag , 
                                           String message , 
                                           Throwable throwable ) {

        // Create the string to be logged
        StringBuilder sb = new StringBuilder();
        String tab = "    ";  // Don't want to use \t
        // Level
        sb.append( level );
        sb.append( ": " );
        // Tag
        sb.append( "Tag: " );
        sb.append( tag );
        sb.append( "\n" );
        // Message
        sb.append( tab );
        sb.append( message );
        sb.append( "\n" );
        // Exception
        if ( throwable != null ) {
            // Exception name
            sb.append( tab );
            sb.append( "Caused by: " );
            sb.append( throwable.toString() );
            // Exception message
            if ( throwable.getMessage() != null ) 
                sb.append ( ": " + throwable.getMessage() );
            sb.append( "\n" );
            // Exception stack trace
            sb.append( getStackTraceString( throwable ) );
        }

        // Return the log string
        return sb.toString();
    }

    /** Main method (does nothing though). */
    public static void main( String[] args ) {
        System.out.println( "Android jar running" );
    }
}
