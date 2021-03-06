package org.ma3map.api;

import java.util.ArrayList;
import java.lang.System;
import java.lang.Object;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ma3map.api.carriers.Commute;
import org.ma3map.api.carriers.LatLng;
import org.ma3map.api.carriers.Route;
import org.ma3map.api.carriers.Stop;
import org.ma3map.api.handlers.Path;
import org.ma3map.api.handlers.Data;
import org.ma3map.api.handlers.Graph;
import org.ma3map.api.handlers.Log;
import org.ma3map.api.listeners.ProgressListener;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author Jason Rogena <jasonrogena@gmail.com>
 * @since 2015-02-13
 *
 * This class implements the /get_paths endpoint for the ma3map API.
 * The /get_paths API calculates the best path from the source to the destination
 * points
 * <p>
 * The API expects the following variables from the client:
 *  -   <code>from</code>   The start point for the commute
 *  -   <code>to</code>     The end point for the commute
 *  <p>
 * GPS coordinates from the client should be of the form <code>latitude,longitude</code>
 */
@javax.ws.rs.Path("/get-paths")
public class GetPaths {

    private static final String TAG = "ma3map.GetPaths";

    private LatLng fromPoint;
    private LatLng toPoint;
    private boolean isWorking;
    private ArrayList<Commute> commutePath;
    private GetPathsProgressListener getPathsProgressListener;
    private long timeTaken;
    private final Object lock = new Object();
    private final ArrayList<Stop> stops;
    private final ArrayList<Route> routes;
    private Data dataHandler;

    @Inject
    Graph graph;

    public GetPaths() {
        dataHandler = new Data();
        this.stops = dataHandler.getStopData();
        this.routes = dataHandler.getRouteData();
    }

    /**
    * Entry point for the /get-paths endpoint
    *
    * @return Stringified JSONArray of the alternative paths
    */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String start(@QueryParam("from") String fromString, @QueryParam("to") String toString, @QueryParam("no_from_stops") String noFromStops, @QueryParam("no_to_stops") String noToStops) {
        Log.i(TAG, "API called");
        graph.printGraphStats();
        int noFrom = Path.MAX_FROM_POINTS;
        int noTo = Path.MAX_TO_POINTS;
        if(noFromStops != null && noFromStops.length() > 0) {
            noFrom = Integer.valueOf(noFromStops);
        }
        if(noToStops != null && noToStops.length() > 0) {
            noTo = Integer.valueOf(noToStops);
        }
        long startTime = System.currentTimeMillis();
        isWorking = true;
        if(fromString != null && toString != null) {
            //process fromString
            String[] fromSegments = fromString.split(",");
            if(fromSegments.length == 2){
                double fromLat = Double.parseDouble(fromSegments[0].trim());
                double fromLng = Double.parseDouble(fromSegments[1].trim());

                fromPoint = new LatLng(fromLat, fromLng);
            }
            else {
                Log.e(TAG, "GPS point in from attribute is mulformed");
                //TODO: send back error message to client
            }

            //process toString
            String[] toSegments = toString.split(",");
            if(toSegments.length == 2){
                double toLat = Double.parseDouble(toSegments[0].trim());
                double toLng = Double.parseDouble(toSegments[1].trim());

                toPoint = new LatLng(toLat, toLng);
            }
            else {
                Log.e(TAG, "GPS point in to attribute is mulformed");
                //TODO: send back error message to client
            }    
            
            //check if to and from LatLngs are initialised
            if(toPoint != null && fromPoint != null) {
                //2. calculate best path
                Path pathHandler = new Path(fromPoint, noFrom, toPoint, noTo, graph, routes, stops);
                getPathsProgressListener = new GetPathsProgressListener(System.currentTimeMillis());
                pathHandler.addProgressListener(getPathsProgressListener);
                pathHandler.calculatePaths();

                //3. wait for calculation to finish
                int s = 0;
				synchronized(lock){
                    while(isWorking){
                        try {
                            lock.wait();
                        }
                        catch(InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //convert commutePath to JSONArray
                JSONObject jsonObject = new JSONObject();
                try {
                    //jsonObject.put("time_taken", String.valueOf(timeTaken)+"ms");
                    JSONArray pathArray = new JSONArray();
                    for(int i = 0; i < commutePath.size(); i++){
                        pathArray.put(commutePath.get(i).getJSONObject());
                    }
                    //jsonObject.put("paths", pathArray);
                    //graph.close();
                    return pathArray.toString();
                }
                catch (JSONException e) {
                    Log.e(TAG, "An error occurred while trying to create a JSONObject for the commute paths");
                    e.printStackTrace();
                }
            }
            else {
                Log.e(TAG, "Unable to initialise either the to or the from LatLng from the provided attributes");
                //TODO: send back error message to client
            }
        }
        else {
            Log.e(TAG, " client provided an unsupported attribute set for this API");
            //TODO: send back error message to client
        }

        return "error occurred";//TODO: replace this with something better
    }

    private class GetPathsProgressListener implements ProgressListener {
        private long startTime;
        
        public GetPathsProgressListener(long startTime) {
            this.startTime = startTime;
        }        

        public void onProgress(int progress, int end, String message, int flag) {
        }

        public void onDone(Object output, String message, int flag) {
            ArrayList<Commute> commutes = (ArrayList<Commute>) output;

            if(commutes != null && flag == ProgressListener.FLAG_DONE){
                Log.i(TAG, "Sending back commute paths to client");
                Log.d(TAG, "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
                for(int commuteIndex = 0; commuteIndex < commutes.size(); commuteIndex++){
                    Log.d(TAG, "Commute with index as "+commuteIndex+" has score of "+commutes.get(commuteIndex).getScore());
                    for(int stepIndex = 0; stepIndex < commutes.get(commuteIndex).getSteps().size(); stepIndex++){
                        Commute currCommute = commutes.get(commuteIndex);
                        if(currCommute.getSteps().get(stepIndex).getStepType() == Commute.Step.TYPE_WALKING){
                            Log.d(TAG, "  step "+stepIndex+" is walking from "+currCommute.getSteps().get(stepIndex).getStart().getName()+" to "+currCommute.getSteps().get(stepIndex).getDestination().getName());
                        }
                        else if(currCommute.getSteps().get(stepIndex).getStepType() == Commute.Step.TYPE_MATATU){
                            Log.d(TAG, "Step "+String.valueOf(stepIndex));
                            if(currCommute.getSteps().get(stepIndex).getRoute() == null) {
                                Log.e(TAG, "Route is null");
                            }
                            else {
                                Log.d(TAG, "  step "+stepIndex+" is using route '"+currCommute.getSteps().get(stepIndex).getRoute().getLongName()+"("+currCommute.getSteps().get(stepIndex).getRoute().getShortName()+")'");
                                if(currCommute.getSteps().get(stepIndex).getStart() != null)
                                    Log.d(TAG, "    from "+currCommute.getSteps().get(stepIndex).getStart().getName()+" "+currCommute.getSteps().get(stepIndex).getStart().getLat()+","+currCommute.getSteps().get(stepIndex).getStart().getLon());
                                if(currCommute.getSteps().get(stepIndex).getDestination() != null)
                                    Log.d(TAG, "    to "+currCommute.getSteps().get(stepIndex).getDestination().getName()+" "+currCommute.getSteps().get(stepIndex).getDestination().getLat()+","+currCommute.getSteps().get(stepIndex).getDestination().getLon());
                            }
                        }
                    }

                    Log.d(TAG, "------------------------------------------------------");
                }
                Log.d(TAG, "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
                commutePath = commutes;
                isWorking = false;
                timeTaken = System.currentTimeMillis() - startTime;
                Log.i(TAG, "Time taken = "+String.valueOf(timeTaken)+" milliseconds");
                synchronized(lock) {
                    lock.notifyAll();
                }
           }
        }
    }

    private class StopPair {
        private final Stop a;
        private final Stop b;
        public StopPair(Stop a, Stop b){
            this.a = a;
            this.b = b;
        }

        public boolean equals(StopPair otherPair) {
            Stop otherA = otherPair.getA();
            Stop otherB = otherPair.getB();
            if(otherA.equals(a)){
                if(otherB.equals(b)) {
                    return true;
                }
            }
            else if(otherA.equals(b)) {
                if(otherB.equals(a)) {
                    return true;
                }
            }
            return false;
        }

        public Stop getA() {
            return a;
        }

        public Stop getB() {
            return b;
        }
    }
}
