package com.example.joginstructor;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kbieniek on 2015-01-05.
 */

public class LocationTraceService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    private Intent localIntent;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private List<Location> locationList = new ArrayList<>();

    public LocationTraceService(){
        super("LocationTraceService");
    }
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public LocationTraceService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        buildGoogleApiClient();
        createLocationRequest();

        if(mGoogleApiClient != null){
            mGoogleApiClient.connect();
        }

        while (intent.getBooleanExtra("gatherLocation", false)) {
            Toast.makeText(getBaseContext(), "OnHandleIntent", Toast.LENGTH_SHORT).show();

            String status = "success";
            double[]tmpLatLng = new double[2];
            for(int i = 0; i < locationList.size(); i++) {
                tmpLatLng[0] = locationList.get(i).getLatitude();
                tmpLatLng[1] = locationList.get(i).getLongitude();
                localIntent = new Intent(Constants.BROADCAST_ACTION)
                        // Puts the status into the Intent
                        .putExtra("location", tmpLatLng);
                // Broadcasts the Intent to receivers in this app.
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
                locationList.remove(i);
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this.getApplicationContext(), "onConnected in Service", Toast.LENGTH_LONG).show();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        locationList.add(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}

