package org.ma3map.api;

import java.io.IOException;
import java.util.ArrayList;
import java.lang.System;
import java.lang.Object;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;

import org.ma3map.api.handlers.Database;
import org.ma3map.api.handlers.Data;
import org.ma3map.api.handlers.Log;
import org.ma3map.api.listeners.ProgressListener;
import org.ma3map.api.carriers.Stop;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author Jason Rogena <jasonrogena@gmail.com>
 * @since 2015-02-13
 *
 * This class implements the /cache_paths endpoint for the ma3map API.
 * The /cache_paths endpoint calculates the best path from each of the stops to each of the other
 * stops
 */
@Path("/cache_paths")
public class CachePaths {

    private static final String TAG = "ma3map.CachePaths";


    /**
    * Entry point for the /cache_paths endpoint
    *
    * @return Stringified JSONArray of the alternative paths
    */
    @GET
    @Produces("application/json")
    public String start(@QueryParam("from") String fromString, @QueryParam("to") String toString, @QueryParam("no_from_stops") String noFromStops, @QueryParam("no_to_stops") String noToStops) {
        Log.i(TAG, "API called");
        Data dataHandler = new Data();
        GetStopsProgressListener getStopsProgressListener = new GetStopsProgressListener();
        dataHandler.addProgressListener(getStopsProgressListener);
        dataHandler.getStopData();
        return "DONE";
    }

    private class GetStopsProgressListener implements ProgressListener {

        @Override
        public void onProgress(int progress, int end, String message, int flag) {
        }

        @Override
        public void onDone(Object output, String message, int flag) {
            if(output != null) {
                ArrayList<Stop> stops = (ArrayList<Stop>) output;
                Log.d(TAG, "Number of stops = "+String.valueOf(stops.size()));
                Data dataHandler = new Data();
                for(int i = 0; i < stops.size(); i++) {
                    Log.i(TAG, "Currently at "+String.valueOf(i+1)+" of "+String.valueOf(stops.size())+" stops");
                    for(int j = 0; j < stops.size(); j++) {
                        if(i != j) {
                            JSONObject paths = dataHandler.getPaths(stops.get(i).getLatLng(), 1, stops.get(j).getLatLng(), 1);
                            Log.d(TAG, paths.toString());
                        }
                    }
                }
            }
            else {
                Log.e(TAG, "Could not fetch stop data from the server");
            }
        }
    }
}
