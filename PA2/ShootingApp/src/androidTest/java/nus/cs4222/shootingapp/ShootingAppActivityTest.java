package nus.cs4222.shootingapp;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class nus.cs4222.shootingapp.ShootingAppActivityTest \
 * nus.cs4222.shootingapp.tests/android.test.InstrumentationTestRunner
 */
public class ShootingAppActivityTest extends ActivityInstrumentationTestCase2<ShootingAppActivity> {

    public ShootingAppActivityTest() {
        super("nus.cs4222.shootingapp", ShootingAppActivity.class);
    }

}
