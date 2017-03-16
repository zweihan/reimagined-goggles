package nus.cs4222.sensorlogapp;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class nus.cs4222.sensorlogapp.SensorLogAppActivityTest \
 * nus.cs4222.sensorlogapp.tests/android.test.InstrumentationTestRunner
 */
public class SensorLogAppActivityTest extends ActivityInstrumentationTestCase2<SensorLogAppActivity> {

    public SensorLogAppActivityTest() {
        super("nus.cs4222.sensorlogapp", SensorLogAppActivity.class);
    }

}
