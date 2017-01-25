package nus.cs4222.lightanalyzer;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class nus.cs4222.lightanalyzer.LightAnalyzerActivityTest \
 * nus.cs4222.lightanalyzer.tests/android.test.InstrumentationTestRunner
 */
public class LightAnalyzerActivityTest extends ActivityInstrumentationTestCase2<LightAnalyzerActivity> {

    public LightAnalyzerActivityTest() {
        super("nus.cs4222.lightanalyzer", LightAnalyzerActivity.class);
    }

}
