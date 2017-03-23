package org.nus.cirlab.mapactivity;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.IndoorLevel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.nus.cirlab.mapactivity.DataStructure.Fingerprint;
import org.nus.cirlab.mapactivity.DataStructure.RadioMap;
import org.nus.cirlab.mapactivity.DataStructure.StepInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import static org.nus.cirlab.mapactivity.R.id.map;

public class MapsActivity extends AppCompatActivity implements OnMarkerDragListener, OnMapLongClickListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private UiSettings mUiSettings;
    private final String mServerIP = "piloc.d1.comp.nus.edu.sg";//
    private String floorPlanID = "";
    private String floorLevel = "1";
    private RadioMap mRadioMap = null;

    // These are simply the string resource IDs for each of the style names. We use them
    // as identifiers when choosing which style to apply.
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final String SELECTED_OPERATION = "selected_operation";
    // Stores the ID of the currently selected style, so that we can re-apply it when
    // the activity restores state, for example when the device changes orientation.
    private int mSelectedOperationId = R.string.operation_label_default;
    private ArrayList<DraggablePoint> mCircles = new ArrayList<>();
    private ArrayList<LatLng> mConfig = new ArrayList<>();
//    private CheckBox mWiFiScanCheckbox;
    private RadioMapCollectionService mPilocService = null;
    private TextView mAccuracyText ;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private int mOperationIds[] = {
            R.string.operation_label_config,
            R.string.operation_label_cancel,
            R.string.operation_label_save,
            R.string.operation_label_upload,
            R.string.operation_label_upload_local_file,
            R.string.operation_label_load_local_file,
            R.string.operation_label_show,
            R.string.operation_label_localization,
            R.string.operation_get_fingerprints,
            R.string.operation_label_default
    };
    int pointSize = 1;

    private static final LatLng nus_com1 = new LatLng(1.294867, 103.773938);
    private static final int MY_LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int LOCATION_LAYER_PERMISSION_REQUEST_CODE = 2;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mLocationPermissionDenied = false;
    private boolean isUpload = true;
    private boolean isUploadRadioMap = true;
    private boolean isSave = false;
    //    private boolean isDownload = false;
    private boolean isShowRadioMap = false;
//    private boolean isWiFiScanOn =false;
    private static final int REQUEST_PLACE_PICKER_UPLOAD = 1;
    private static final int REQUEST_PLACE_PICKER_DOWNLOAD = 2;
    private static final int REQUEST_PLACE_PICKER_SAVE = 3;
    private static final int FILE_SELECT_CODE = 4;



    private ServiceConnection conn = new ServiceConnection() {
        public void onServiceDisconnected(ComponentName name) {
            mPilocService.onDestroy();
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            RadioMapCollectionService.MyBinder binder = (RadioMapCollectionService.MyBinder) service;
            mPilocService = binder.getService();
            // Start collecting only WiFi fingerprints

            mPilocService.startCollectingFingerprints();
            mPilocService.startCollection();
        }
    };

    private class DraggablePoint {
        private Circle circle;
//        private Marker centerMarker;

        public DraggablePoint(LatLng center, double radius, boolean clickable, int color) {

//            centerMarker = mMap.addMarker(new MarkerOptions()
//                    .position(center)
//                    .draggable(true)
//                    .title(center.toString()));
            circle = mMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radius)
                    .strokeColor(color)
                    .fillColor(color)
                    .clickable(clickable));
        }

        public LatLng getCenterLatLng(){
            return circle.getCenter();
        }



    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
//        mWiFiScanCheckbox = (CheckBox)findViewById(R.id.data_collection);
        mAccuracyText = (TextView)findViewById(R.id.textView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            // Create and bind the PiLoc service, make some change here
            Intent intent = new Intent(MapsActivity.this, RadioMapCollectionService.class);
            this.getApplicationContext().bindService(intent, conn, Context.BIND_AUTO_CREATE);
        }
    }
        /**
         * Shows a dialog listing the styles to choose from, and applies the selected
         * style when chosen.
         */
    private void showOperationDialog() {
        // mStyleIds stores each style's resource ID, and we extract the names here, rather
        // than using an XML array resource which AlertDialog.Builder.setItems() can also
        // accept. We do this since using an array resource would mean we would not have
        // constant values we can switch/case on, when choosing which style to apply.
        List<String> operationNames = new ArrayList<>();
        for (int operation : mOperationIds) {
            operationNames.add(getString(operation));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.operation_choose));
        builder.setItems(operationNames.toArray(new CharSequence[operationNames.size()]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectedOperationId = mOperationIds[which];
                        String msg = getString(R.string.operation_set_to, getString(mSelectedOperationId));
//                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);
                        runSelectedOperation();
                    }
                });
        builder.show();
    }

    /**
     * Creates a {@link MapStyleOptions} object via loadRawResourceStyle() (or via the
     * constructor with a JSON String), then sets it on the {@link GoogleMap} instance,
     * via the setMapStyle() method.
     */
    private void runSelectedOperation() {
        switch (mSelectedOperationId) {
            case R.string.operation_label_config:
                isUpload = true;
                isSave = false;
                isUploadRadioMap = false;
//                isDownloadRadioMap = false;
                new GetMapIDTask().execute(null, null, null);
//                UploadRadioMap();
                break;
            case R.string.operation_label_cancel:
                CancelMapping();
                break;
            case R.string.operation_label_save:
                isSave = true;
                isUpload = false;
                SaveSelectedTrace();
                break;
            case R.string.operation_label_upload:
                isUpload =true;
                isSave = false;
                isUploadRadioMap = true;
//                isDownloadRadioMap = false;
//                UploadRadioMap();
                new GetMapIDTask().execute(null, null, null);
                break;
            case R.string.operation_label_upload_local_file:
                isUpload =true;
                showFileChooser();
                break;

            case R.string.operation_label_load_local_file:
                isUpload =false;
                showFileChooser();
                break;

            case R.string.operation_label_show:
                if (mIsLocating)
                    mIsLocating = false;
//                isDownloadRadioMap = true;
                isUpload = false;
                isSave = false;
                isShowRadioMap = true;
                new GetMapIDTask().execute(null, null, null);

                break;
            case R.string.operation_label_localization:
//                isDownloadRadioMap = true;
                isUpload = false;
                isSave = false;
                isShowRadioMap = false;
                if (mIsLocating) {
                    mIsLocating = false;
                    if(locationMarker!=null){
                        locationMarker.remove();
                    }
                }
                else {
                    mIsLocating = true;
                    new GetMapIDTask().execute(null, null, null);
//                    startLocalization();

                }
                break;
            case R.string.operation_get_fingerprints:
                getFingerprints();
                break;
            case R.string.operation_label_default:
                this.finish();
                break;
            default:
                return;
        }
    }

    public void getFingerprints() {
        try {
            Vector<Fingerprint> fp = mPilocService.getFingerprint();
            String data = "";
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            SimpleDateFormat mdformat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String strDate = mdformat.format(calendar.getTime());
            data = data + strDate + "\n\n";
            for (Fingerprint fingerprint : fp) {
                data = data + fingerprint.mMac.toString() + " " + fingerprint.mRSSI.toString() + "\n";
            }
            data = data + "-----------------------------------\n";

            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/";
            verifyStoragePermissions(this);
            // Create the parent path
            File dir = new File(path);
            if (!dir.exists()) {
                Boolean val = dir.mkdirs();
            }
            File f = new File(path + "fingerprint.txt");
            if (!f.exists()) {
                Boolean val = f.createNewFile();
            }

            FileOutputStream stream = new FileOutputStream(f, true);
            try {
                stream.write(data.getBytes());
            } finally {
                stream.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mUiSettings = mMap.getUiSettings();

        // Keep the UI Settings state in sync.
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setScrollGesturesEnabled(true);
        mUiSettings.setZoomGesturesEnabled(true);
        mUiSettings.setTiltGesturesEnabled(false);
        mUiSettings.setRotateGesturesEnabled(true);
        mMap.setOnMarkerDragListener(this);
        mMap.setOnMapLongClickListener(this);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mUiSettings.setMyLocationButtonEnabled(true);
        } else {
            requestLocationPermission(MY_LOCATION_PERMISSION_REQUEST_CODE);
        }
        Location location = mMap.getMyLocation();
        if (location != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                    location.getLongitude()), 19));
        }else
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nus_com1, 19));

        // Set up the click listener for the circle.
        googleMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                // Flip the r, g and b components of the circle's stroke color.
                circle.setStrokeColor(Color.BLUE);
            }
        });

        // Create and bind the PiLoc service, make some change here
//        Intent intent = new Intent(MapsActivity.this, RadioMapCollectionService.class);
//        this.getApplicationContext().bindService(intent, conn, Context.BIND_AUTO_CREATE);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.operation_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_operation_choose) {
            showOperationDialog();
        }
        return true;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        onMarkerMoved(marker);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        onMarkerMoved(marker);
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        onMarkerMoved(marker);
    }

    private void onMarkerMoved(Marker marker) {
        for (DraggablePoint draggablePoint : mCircles) {
//            if (draggablePoint.onMarkerMoved(marker)) {
//                break;
//            }
        }
    }

    private boolean isStartCollecting = false;
    private LatLng mStartLoc = null;
    private LatLng mEndLoc = null;
    private Boolean mIsRedoMapping = false;
    private Vector<StepInfo> mCurrentMappedSteps = null;


    @Override
    public void onMapLongClick(LatLng point) {
        DraggablePoint circle =
                new DraggablePoint(point, pointSize, true, Color.GREEN);
        mCircles.add(circle);
//        if(isWiFiScanOn){
            // No starting location yet, set current point as the starting location
            if (mStartLoc == null) {
                mPilocService.setStartCountingStep(true);
                isStartCollecting = true;
                mStartLoc = point;
            } else {
                // If it is not re-mapping, set previous ending point as the
                // starting location
                if (!mIsRedoMapping && mEndLoc != null)
                    mStartLoc = mEndLoc;

                // Set current point as the ending location
                mEndLoc = point;
            }


            if (mStartLoc != null && mEndLoc != null) {
                // If it is not re-mapping, confirm the previous mapping
                if (!mIsRedoMapping)
                    mPilocService.confirmCurrentMapping();
                else
                    mIsRedoMapping = false;

                // Get mapping for the newly collected annotated walking trajectory
                mCurrentMappedSteps = mPilocService.mapCurrentTrajectory(mStartLoc, mEndLoc);
                if (mCurrentMappedSteps != null) {
                    // Set newly mapped points to green color on the bitmap
                    for (StepInfo s : mCurrentMappedSteps) {
                        DraggablePoint tempDP = new DraggablePoint(new LatLng(s.mPosX,s.mPosY), pointSize, true, Color.GREEN);
                        mCircles.add(tempDP);
                    }
                }
            }
//        }
    }


    /**
     * Checks if the map is ready (which depends on whether the Google Play services APK is
     * available. This should be called prior to calling any methods on GoogleMap.
     */
    private boolean checkReady() {
        if (mMap == null) {
            Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Requests the fine location permission. If a rationale with an additional explanation should
     * be shown to the user, displays a dialog that triggers the request.
     */
    public void requestLocationPermission(int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Display a dialog with rationale.
            PermissionUtils.RationaleDialog
                    .newInstance(requestCode, false).show(
                    getSupportFragmentManager(), "dialog");
        } else {
            // Location permission has not been granted yet, request it.
            PermissionUtils.requestPermission(this, requestCode,
                    Manifest.permission.ACCESS_FINE_LOCATION, false);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == MY_LOCATION_PERMISSION_REQUEST_CODE) {
            // Enable the My Location button if the permission has been granted.
            if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                mUiSettings.setMyLocationButtonEnabled(true);
            } else {
                mLocationPermissionDenied = true;
            }

        } else if (requestCode == LOCATION_LAYER_PERMISSION_REQUEST_CODE) {
            // Enable the My Location layer if the permission has been granted.
            if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                try {
                    mMap.setMyLocationEnabled(true);
                } catch (SecurityException e) {
                    Toast.makeText(this, "Not enough permission to run this application!", Toast.LENGTH_SHORT).show();
                }
            } else {
                mLocationPermissionDenied = true;
            }
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mLocationPermissionDenied) {
            PermissionUtils.PermissionDeniedDialog
                    .newInstance(false).show(getSupportFragmentManager(), "dialog");
            mLocationPermissionDenied = false;
        }
    }


    String chosenFileName = "";
    private void showFileChooser() {

        SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(MapsActivity.this, "FileOpen..",
                new SimpleFileDialog.SimpleFileDialogListener()
                {
                    @Override
                    public void onChosenDir(String chosenDir)
                    {
                        // The code in this function will be executed when the dialog OK button is pushed
                        chosenFileName = chosenDir;
                        String[] pathSegment = chosenFileName.split("/");
                        floorPlanID = pathSegment[pathSegment.length-3];
                        floorLevel = pathSegment[pathSegment.length-2];
                        fileName = pathSegment[pathSegment.length-1];

//                        Toast.makeText(MapsActivity.this, floorPlanID+" "+floorLevel+" "+fileName, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "File Uri: " + chosenFileName);

                        if(isUpload)
                            new UploadTraceTask().execute(null, null, null);
                        else
                            new LoadTraceFromLocalTask().execute(null, null, null);


                    }
                });

        FileOpenDialog.default_file_name = "";
        FileOpenDialog.chooseFile_or_Dir();
    }


    List<String> locationNames = null;
    private void showLocationDialog( Vector<String> locationList) {
        // mStyleIds stores each style's resource ID, and we extract the names here, rather
        // than using an XML array resource which AlertDialog.Builder.setItems() can also
        // accept. We do this since using an array resource would mean we would not have
        // constant values we can switch/case on, when choosing which style to apply.
        locationNames = new ArrayList<>();
        for (String place : locationList) {
            locationNames.add(place);
        }
        locationNames.add(getString(R.string.location_pick));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.location_choose));
        builder.setItems(locationNames.toArray(new CharSequence[locationNames.size()]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String msg = locationNames.get(which);
                        Log.d(TAG, msg);
                        if(isUpload)
                            UploadSelectedRadioMap(locationNames.get(which));
                        else
                            DownloadSelectedRadioMap(locationNames.get(which));
                    }
                });
        builder.show();
    }


    private void DownloadSelectedRadioMap(String locationString) {

        if(locationString.contains("Level")){
            String[] ls =  locationString.split(", ");
            floorPlanID = ls[0];
            floorLevel = ls[1].substring(6);

            Log.d(TAG, floorPlanID+" "+floorLevel);
            if(floorPlanID.length()>0) {
                File filename = new File(this.getExternalCacheDir()  + "/PiLoc/"+floorPlanID+"/"+floorLevel+"/radiomap.rm");
                if(filename.exists() && !isShowRadioMap)
                    new LoadRadioMapFromLocalTask().execute(null, null, null);
                else
                    new GetRadioMapTask().execute(null, null, null);
                while (mCircles.size()>0) {
                    DraggablePoint freePoint = mCircles.remove(mCircles.size() - 1);
                    freePoint.circle.remove();
                }
            }

        }else{
            try {
                PlacePicker.IntentBuilder intentBuilder =
                        new PlacePicker.IntentBuilder();
                Intent intent = intentBuilder.build(this);
                // Start the intent by requesting a result,
                // identified by a request code.

                startActivityForResult(intent, REQUEST_PLACE_PICKER_DOWNLOAD);

            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
        }


    }

    private void SaveSelectedTrace() {
            try {
                PlacePicker.IntentBuilder intentBuilder =
                        new PlacePicker.IntentBuilder();
                Intent intent = intentBuilder.build(this);
                startActivityForResult(intent, REQUEST_PLACE_PICKER_SAVE);

            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
    }


    private void UploadSelectedRadioMap(String locationString) {

        if(locationString.contains("Level")){
            String[] ls =  locationString.split(", ");
            floorPlanID = ls[0];
            floorLevel = ls[1].substring(6);

            Log.d(TAG, floorPlanID+" "+floorLevel);
            if(floorPlanID.length()>0) {
                if(isUploadRadioMap){
                    new UploadRadioMapTask().execute(null, null, null);
                }else{
                    mConfig.clear();
                    for(DraggablePoint dp: mCircles){
                        mConfig.add(dp.getCenterLatLng());
                    }

                    new UploadRadioMapConfigTask().execute(null, null, null);
                }
                while (mCircles.size()>0) {
                    DraggablePoint freePoint = mCircles.remove(mCircles.size() - 1);
                    freePoint.circle.remove();
                }
            }

        }else{
            try {
                PlacePicker.IntentBuilder intentBuilder =
                        new PlacePicker.IntentBuilder();
                Intent intent = intentBuilder.build(this);
                // Start the intent by requesting a result,
                // identified by a request code.

                startActivityForResult(intent, REQUEST_PLACE_PICKER_UPLOAD);

            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
        }


    }

    private Boolean mIsRadioMapReady = false;
    private String fileName = null;
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {

        if (requestCode == REQUEST_PLACE_PICKER_UPLOAD
                && resultCode == Activity.RESULT_OK) {
            // The user has selected a place. Extract the name and address.
            final Place place = PlacePicker.getPlace(data, this);

            floorPlanID = place.getName().toString();

            IndoorBuilding building = mMap.getFocusedBuilding();
            if (building != null) {
                IndoorLevel level =
                        building.getLevels().get(building.getActiveLevelIndex());

                if (level != null) {
                    floorLevel = level.getShortName();
                } else {
                    floorLevel = "1";
                }
            }

            Log.d(TAG, floorPlanID+" "+floorLevel);
            if(floorPlanID.length()>0) {
                if(isUploadRadioMap){
                    new UploadRadioMapTask().execute(null, null, null);
                }else{
                    mConfig.clear();
                    for(DraggablePoint dp: mCircles){
                        mConfig.add(dp.getCenterLatLng());
                    }

                    new UploadRadioMapConfigTask().execute(null, null, null);
                }
                while (mCircles.size()>0) {
                    DraggablePoint freePoint = mCircles.remove(mCircles.size() - 1);
                    freePoint.circle.remove();
                }
            }
        }else if (requestCode == REQUEST_PLACE_PICKER_DOWNLOAD
                && resultCode == Activity.RESULT_OK) {

            // The user has selected a place. Extract the name and address.
            final Place place = PlacePicker.getPlace(data, this);

            floorPlanID = place.getName().toString();

            IndoorBuilding building = mMap.getFocusedBuilding();
            if (building != null) {
                IndoorLevel level =
                        building.getLevels().get(building.getActiveLevelIndex());

                if (level != null) {
                    floorLevel = level.getShortName();
                } else {
                    floorLevel = "1";
                }
            }

            Log.d(TAG, floorPlanID+" "+floorLevel);
            if(floorPlanID.length()>0) {
                File filename = new File(this.getExternalCacheDir()  + "/PiLoc/"+floorPlanID+"/"+floorLevel+"/radiomap.rm");
                if(filename.exists() && !isShowRadioMap)
                    new LoadRadioMapFromLocalTask().execute(null, null, null);
                else
                    new GetRadioMapTask().execute(null, null, null);
                while (mCircles.size()>0) {
                    DraggablePoint freePoint = mCircles.remove(mCircles.size() - 1);
                    freePoint.circle.remove();
                }
            }
        }else if (requestCode == REQUEST_PLACE_PICKER_SAVE
                && resultCode == Activity.RESULT_OK) {

            // The user has selected a place. Extract the name and address.
            final Place place = PlacePicker.getPlace(data, this);

            floorPlanID = place.getName().toString();

            IndoorBuilding building = mMap.getFocusedBuilding();
            if (building != null) {
                IndoorLevel level =
                        building.getLevels().get(building.getActiveLevelIndex());

                if (level != null) {
                    floorLevel = level.getShortName();
                } else {
                    floorLevel = "1";
                }
            }

            Log.d(TAG, floorPlanID+" "+floorLevel);
            if(floorPlanID.length()>0) {
                new SaveTraceTask().execute(null, null, null);
                while (mCircles.size()>0) {
                    DraggablePoint freePoint = mCircles.remove(mCircles.size() - 1);
                    freePoint.circle.remove();
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private class UploadRadioMapTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... f) {
            try {
                if(mPilocService!=null){
                    // Append the newly mapped fingerprints to current radiomap
                    mPilocService.appendRadioMapFromMapping();
                    // Upload the current radiomap to the server
                    mPilocService.uploadRadioMap(mServerIP, floorPlanID,  floorLevel);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) { }
    }

    private  boolean isUploadSuccess = false;
    private class UploadTraceTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... f) {
            try {
                if(mPilocService!=null) {
                    isUploadSuccess = mPilocService.uploadTraceFile(mServerIP, floorPlanID, floorLevel, fileName);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (isUploadSuccess) {
                Toast.makeText(getBaseContext(), "Upload successfully", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getBaseContext(), "Upload failed", Toast.LENGTH_SHORT).show();
        }
    }

    private class UploadRadioMapConfigTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... f) {
            try {
                    // Upload the current radio map to the server
                mPilocService.uploadRadioMapConfig(mServerIP, floorPlanID,  floorLevel, mConfig);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) { }
    }

    private class GetRadioMapTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... s) {
            try {
                // Get radio map using the floor ID from server
                mRadioMap = mPilocService.getRadioMap(mServerIP, floorPlanID, floorLevel);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (mRadioMap != null) {
                Toast.makeText(getBaseContext(), "Get radioMap successfully", Toast.LENGTH_SHORT).show();
                if(isShowRadioMap)
                    ShowRadioMap();
                else if (mIsLocating)
                    startLocalization();
            } else
                Toast.makeText(getBaseContext(), "Get radioMap failed", Toast.LENGTH_SHORT).show();

        }
    }


    private ArrayList<LatLng> NodeList = new ArrayList<>();
    private class LoadTraceFromLocalTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... s) {
            try {
                // Get radio map using the floor ID from server
                NodeList = mPilocService.loadTrace(floorPlanID, floorLevel, fileName);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (NodeList!=null && NodeList.size()>0) {
                ShowTrace();
            } else
                Toast.makeText(getBaseContext(), "Load trace failed", Toast.LENGTH_SHORT).show();

        }
    }

    private class LoadRadioMapFromLocalTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... s) {
            try {
                // Get radio map using the floor ID from server
                mRadioMap = mPilocService.loadRadioMap(floorPlanID, floorLevel);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (mRadioMap != null) {
                if (mIsLocating)
                    startLocalization();
                Toast.makeText(getBaseContext(), "Load radioMap successfully", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getBaseContext(), "Load radioMap failed", Toast.LENGTH_SHORT).show();

        }
    }

    private class SaveTraceTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... s) {
            try {
                // Get radio map using the floor ID from server
                mPilocService.saveNewlyCollectedTrace(floorPlanID, floorLevel);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private Vector<String> locationList= null;
    private class GetMapIDTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... s) {
            try {
                locationList = mPilocService.getCurrentFloorIDList(mServerIP, mPilocService.getFingerprint());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (locationList != null) {
//                Toast.makeText(getBaseContext(), "Get map id successfully", Toast.LENGTH_SHORT).show();
                showLocationDialog(locationList);
            } else
                Toast.makeText(getBaseContext(), "Get map id failed", Toast.LENGTH_SHORT).show();

        }
    }

    private Boolean mIsLocating = false;
    private LatLng mCurrentLocation ;
    private Marker locationMarker = null;

    public void startLocalization() {
        Log.d(TAG, "start localization");
        new Thread(new Runnable() {
            public void run() {
                try {
                    while (mIsLocating) {
                        // Get current fingerprints
                        Vector<Fingerprint> fp = mPilocService.getFingerprint();

                        if(fp!=null && fp.size()>0){
                            Log.d(TAG, "finger print size: "+fp.size());
                            mCurrentLocation = getLocation(mRadioMap, fp);

                            if (mCurrentLocation == null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run(){
                                        Toast.makeText(getBaseContext(), "Not enough APs to determine location.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                Thread.sleep(500);
                                continue;
                            } else {
                                Log.d(TAG, mCurrentLocation.toString());

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(locationMarker!=null){
                                            locationMarker.remove();
                                        }
                                        locationMarker = mMap.addMarker(new MarkerOptions()
                                                .position(mCurrentLocation)
                                                .title("Your Indoor Location"));
                                    }
                                });

                                Thread.sleep(3000);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public LatLng getLocation(RadioMap mRadioMap, Vector<Fingerprint> fp) {


        if(mRadioMap == null || fp == null){
            return null;
        }

        HashMap<String, Double> map = getFingerprintMap(fp);
        LatLng geo = null;
        double minError = Double.MAX_VALUE;
        int minErrorNumAP = 0;

        for (Map.Entry<LatLng, Vector<Fingerprint>> entry
                : mRadioMap.mLocFingerPrints.entrySet()) {

            Vector<Fingerprint> fingerprints = entry.getValue();
            HashSet<String> uniqueFingerprints = new HashSet<String>(map.size()*2);
            double totalError = 0;

            for( Fingerprint f : fingerprints ){
                String mac = f.mMac.toLowerCase();
                if( map.containsKey(mac) ){
                    double mRSSI = map.get(mac);
                    totalError += ((f.mRSSI-mRSSI)*(f.mRSSI-mRSSI));
                    uniqueFingerprints.add(mac);
                }
            }

            if( uniqueFingerprints.size() > (map.size() / 2.0) ){

                totalError = Math.sqrt(totalError);
                if( uniqueFingerprints.size() > minErrorNumAP
                        || (uniqueFingerprints.size() == minErrorNumAP && totalError < minError) ){
                    geo = entry.getKey();
                    minError = totalError;
                    minErrorNumAP = uniqueFingerprints.size();
                }
            }
        }

        return geo;
    }

    private HashMap<String, Double> getFingerprintMap(Vector<Fingerprint> fp) {

        HashMap<String, Double> fingerprints
                = new HashMap<String, Double>(fp.size()*2);

        HashMap<String, Integer> counts
                = new HashMap<String, Integer>(fp.size()*2);

        for( Fingerprint fingerprint : fp ){
            if(fingerprint != null){
                String mac = fingerprint.mMac;
                if(mac != null){
                    mac = mac.toLowerCase();
                    if(!fingerprints.containsKey(mac)){
                        fingerprints.put(mac, (double)fingerprint.mRSSI);
                        counts.put( mac, 1 );
                    } else {
                        double sum = fingerprints.get(mac) + fingerprint.mRSSI;
                        fingerprints.put(mac, sum);
                        counts.put(mac, counts.get(mac)+1);
                    }
                }
            }
        }

        Set<Map.Entry<String, Double>> tempSet = fingerprints.entrySet();
        for(Map.Entry<String, Double> entry : tempSet){
            String mac = entry.getKey();
            int count = counts.get(mac);
            double mRSSI = entry.getValue();
            entry.setValue(mRSSI/count);
        }

        return fingerprints;
    }

    public void ShowRadioMap() {
        Log.d(TAG, "show radio map on UI");
        while (mCircles.size()>0) {
            DraggablePoint freePoint = mCircles.remove(mCircles.size() - 1);
            freePoint.circle.remove();
        }

        if(locationMarker!=null){
            locationMarker.remove();
        }

        if (mRadioMap != null) {
            Log.d(TAG, mRadioMap.mLocFingerPrints.size()+" ");
            // Set newly mapped points to green color on the bitmap
            for (LatLng latLng : mRadioMap.mLocFingerPrints.keySet()) {
                DraggablePoint tempDP = new DraggablePoint(latLng, pointSize, true, Color.GRAY);
                mCircles.add(tempDP);
            }
//            mAccuracyText.setText("Error: "+mPilocService.getLocalizationError()+" m");
        }
    }

    public void ShowTrace() {
        Log.d(TAG, "show trace on UI");
        while (mCircles.size()>0) {
            DraggablePoint freePoint = mCircles.remove(mCircles.size() - 1);
            freePoint.circle.remove();
        }

        if(locationMarker!=null){
            locationMarker.remove();
        }

        if (NodeList != null) {
            Log.d(TAG, NodeList.size()+" ");
            // Set newly mapped points to green color on the bitmap
            for (LatLng latLng :NodeList) {
                DraggablePoint tempDP = new DraggablePoint(latLng, pointSize, true, Color.GREEN);
                mCircles.add(tempDP);
            }
        }
    }

    public void CancelMapping( ) {
        // If it is already in the re-mapping state, remove the current mapping
        if (mIsRedoMapping) {
            mPilocService.removeCurrentMapping();
            mIsRedoMapping = false;
            Toast.makeText(getBaseContext(), "Previous mapping removed", Toast.LENGTH_SHORT).show();
        } else {
            // No mapped steps, return immediately
            if (mCurrentMappedSteps == null)
                return;

            // Set the remap flag
            mIsRedoMapping = true;

            for (int i=0;i<= mCurrentMappedSteps.size();i++) {
                DraggablePoint freePoint = mCircles.remove(mCircles.size()-1);
//                freePoint.centerMarker.remove();
                freePoint.circle.remove();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPilocService != null) {
            // Stop collecting annotated walking trajectories
            mPilocService.stopCollection();
        }

        // Unbind the service
        while (mCircles.size()>0) {
            DraggablePoint freePoint = mCircles.remove(mCircles.size() - 1);
            freePoint.circle.remove();
        }
        mCircles.clear();
        if(conn != null)
            getApplicationContext().unbindService(conn);
        isStartCollecting = false;
    }


}
