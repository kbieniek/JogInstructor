package com.example.joginstructor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class JogInstructorActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Location mCurrentLocation;
    private boolean mRequestedLocationUpdates = false;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String mLastUpdateTime;
    private long startTime = 0L;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    private Button startStop;
    private TextView mTimeLabel;
    private TextView distance;
    private List<LatLng> routePoint = new ArrayList<>();
    private Polyline route;

    private TextView speed;
    private double mCurrentDistance = 0;
    private boolean setStartMarker = true;
    private Intent mIntentService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jog_instructor);
        setUpMapIfNeeded();
        setMapOptions();
        updateValuesFromBundle(savedInstanceState);
        createLocationRequest();
        buildGoogleApiClient();


        startStop = (Button)findViewById(R.id.startStop);
        mTimeLabel = (TextView)findViewById(R.id.mTimeLabel);
        distance = (TextView)findViewById(R.id.distance);
        speed = (TextView)findViewById(R.id.speed);


        // The filter's action is BROADCAST_ACTION
        IntentFilter mStatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);
        // Instantiates a new DownloadStateReceiver
        DownloadStateReceiver mDownloadStateReceiver =
                new DownloadStateReceiver();
        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mDownloadStateReceiver,
                mStatusIntentFilter);


        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startStop.getText().toString().equalsIgnoreCase("Start"))
                {

                    mRequestedLocationUpdates = true;
                    if(mGoogleApiClient != null){
                        mGoogleApiClient.connect();
                    }
                    route = mMap.addPolyline(new PolylineOptions().width(10).color(Color.RED).geodesic(true));
                    startTime = SystemClock.uptimeMillis();
                    customHandler.postDelayed(updateTimerThread, 0);
                    triggerServiceAction();
                    startStop.setText("Pause");
                }
                else if(startStop.getText().toString().equalsIgnoreCase("Pause")){
                    mMap.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())).title("Stop").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    timeSwapBuff += timeInMilliseconds;
                    customHandler.removeCallbacks(updateTimerThread);
                    startStop.setText("Reset");
                    mGoogleApiClient.disconnect();
                }
                else
                {
                    route.remove();
                    mMap.clear();
                    routePoint.removeAll(routePoint);
                    setStartMarker = true;
                    mCurrentDistance = 0;
                    startStop.setText("Start");
                }
            }
        });

    }

    private void triggerServiceAction() {
        mIntentService = new Intent(this, LocationTraceService.class);
        mIntentService.setData(Uri.parse("https://developer.android.com/training/run-background-service/report-status.html"));
        mIntentService.putExtra("gatherLocation", true);
        startService(mIntentService);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setMapOptions();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && !mRequestedLocationUpdates) {
            startLocationUpdates();
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(Constants.REQUESTING_LOCATION_UPDATES_KEY,
                mRequestedLocationUpdates);
        savedInstanceState.putParcelable(Constants.LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(Constants.LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(Constants.REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestedLocationUpdates = savedInstanceState.getBoolean(
                        Constants.REQUESTING_LOCATION_UPDATES_KEY);
                //setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(Constants.LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocation is not null.
                mCurrentLocation = savedInstanceState.getParcelable(Constants.LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(Constants.LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        Constants.LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())).title("MyLocation"));
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(15)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east (90)
                    .tilt(0)                   // Sets the tilt of the camera to 30 degrees (40)
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        }
    }

    private void setMapOptions() {
        GoogleMapOptions options = new GoogleMapOptions();
        options.mapType(GoogleMap.MAP_TYPE_SATELLITE)
                .compassEnabled(true)
                .rotateGesturesEnabled(true)
                .zoomGesturesEnabled(true)
                .tiltGesturesEnabled(true);

        MapFragment.newInstance(options);
        mMap.setMyLocationEnabled(true);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this.getApplicationContext(), "onConnected", Toast.LENGTH_LONG).show();
        if(mRequestedLocationUpdates)
        {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        if(setStartMarker)
        {
            mMap.addMarker(new MarkerOptions().position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude())).title("Start").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            setStartMarker = false;
        }
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        routePoint.add(new LatLng(location.getLatitude(),location.getLongitude()));
        speed.setText(String.format("%.1f", (location.getSpeed()*3.6)) + "km/h");

        if(routePoint.size() > 1) {
            float[] result = new float[3];
            Location.distanceBetween(routePoint.get(routePoint.size()-2).latitude, routePoint.get(routePoint.size()-2).longitude,
                    routePoint.get(routePoint.size()-1).latitude, routePoint.get(routePoint.size()-1).longitude, result);
            mCurrentDistance += result[0];
        }
        if(mCurrentDistance > 1000)
        {
            distance.setText(String.format("%.2f", mCurrentDistance/1000) + "km");
        }else {
            distance.setText(String.format("%.1f", mCurrentDistance) + "m");
        }
        updateUI();
    }

    private void updateUI() {
        route.setPoints(routePoint);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))      // Sets the center of the map to location user
                .zoom(15)                   // Sets the zoom
                .bearing(0)                // Sets the orientation of the camera to east (90)
                .tilt(0)                   // Sets the tilt of the camera to 30 degrees (40)
                .build();                   // Creates a CameraPosition from the builder
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        if(mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    private Runnable updateTimerThread = new Runnable() {

        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int hours = secs / 3600;
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            mTimeLabel.setText(String.format("%02d", hours) + ":"
                    + String.format("%02d", mins) + ":"
                    + String.format("%02d", secs) + ":"
                    + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 0);
        }
    };

    // Broadcast receiver for receiving status updates from the IntentService
    private class DownloadStateReceiver extends BroadcastReceiver
    {
        // Prevents instantiation
        private DownloadStateReceiver() {
        }
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {

            double[] tmpArray = intent.getDoubleArrayExtra("location");
            routePoint.add(new LatLng(tmpArray[0], tmpArray[1]));

        }
    }
}