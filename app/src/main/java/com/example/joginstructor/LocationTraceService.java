package com.example.joginstructor;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

/**
 * Created by kbieniek on 2015-01-05.
 */

public class LocationTraceService extends IntentService{


    private Intent localIntent;

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
        // Gets data from the incoming Intent
        String dataString = intent.getDataString();
        //...
        // Do work here, based on the contents of dataString
        //...
        while (intent.getBooleanExtra("gatherLocation", false)) {
            Toast.makeText(getBaseContext(), "OnHandleIntent", Toast.LENGTH_SHORT).show();

            String status = "success";
            localIntent = new Intent(Constants.BROADCAST_ACTION)
                    // Puts the status into the Intent
                    .putExtra(Constants.EXTENDED_DATA_STATUS, status);
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }
    }

}

