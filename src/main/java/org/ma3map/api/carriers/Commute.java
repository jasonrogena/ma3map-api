package org.ma3map.api.carriers;

import java.util.ArrayList;
import java.util.Comparator;

import org.ma3map.api.handlers.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable
public class Commute {
	public static final String PARCELABLE_KEY = "Commute";
    private static final int PARCELABLE_DESC = 2112;
    private static final String TAG = "ma3map.Commute";

    private final double SCORE_STEP = 10;//score given for each step in commute
    private final double SCORE_WALKING = 0.1;//score given for each meter walked
    private final double SCORE_STOP = 2;//score given for each stop in commute
    private final double SPEED_WALKING = 2.77778;//average walking speed in m/s
    private final double SPEED_MATATU = 5.55556;//average value in m/s that can be used to estimate how long it would take a matatu to cover some distance

    private Long id;
    private LatLng from;//actual point on map use wants to go from
    private LatLng to;//actual point on map user want to go to
    private ArrayList<Step> steps;
    private double time;
    private int noStops;
    
    public Commute(LatLng from, LatLng to){
        this.from = from;
        this.to = to;
        this.steps = new ArrayList<Step>();
        time = -1;
    }

    public JSONObject getJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("score", getScore());
            JSONObject startJO = new JSONObject();
            startJO.put("start", from.getJSONObject());
            startJO.put("destination", steps.get(0).getStart().getJSONObject());
            startJO.put("type", "walking");
            startJO.put("sequence", 0);
            startJO.put("text", "Walk to "+steps.get(0).getStart().getName());
        
            JSONObject destJO = new JSONObject();
            destJO.put("start", steps.get(steps.size() - 1).getDestination().getJSONObject());
            destJO.put("destination", to.getJSONObject());
            destJO.put("type", "walking");
            destJO.put("sequence", steps.size() + 1);
            destJO.put("text", "Walk from "+steps.get(steps.size() - 1).getDestination().getName()+" to your final destination");

            JSONArray stepsArray = new JSONArray();
            stepsArray.put(startJO);
            for(int i = 0; i < steps.size(); i++){
                stepsArray.put(steps.get(i).getJSONObject(i + 1));
            }
            stepsArray.put(destJO);
            jsonObject.put("steps", stepsArray);
            return jsonObject;
        }
        catch (JSONException e) {
            Log.e(TAG, "An error occurred while trying to create a JSONObject for this object");
            e.printStackTrace();
        } 
        return null;
    }
    public void setNoStops(int noStops) {
        this.noStops = noStops;
    }
    public Long getId(){
    	return this.id;
    }
    
    public void setId(Long id){
    	this.id = id;
    }
    
    public LatLng getFrom(){
        return from;
    }

    public LatLng getTo(){
        return to;
    }

    public Step getStep(int index){
        return steps.get(index);
    }

    public ArrayList<Step> getSteps(){
        return steps;
    }

    public void setSteps(ArrayList<Step> steps){
        this.steps = new ArrayList<Step>();
        for(int index = 0; index < steps.size(); index++){
            this.steps.add(new Step(steps.get(index).getStepType(), steps.get(index).getRoute(), steps.get(index).getStart(), steps.get(index).getDestination()));
        }
    }

    public double getTime(){
        return time;
    }

    public void setStep(int index, Step step){
        if(index < steps.size() && index >= 0){
            steps.set(index, step);
        }
        else {
            Log.e(TAG, "Unable to set step to index "+index+" because index is out of bounds");
        }
    }
    
    public void addStep(Step step){
        this.steps.add(step);
    }
    
    public ArrayList<Route> getMatatuRoutes(){
        ArrayList <Route> matatuRoutes = new ArrayList<Route>();

        for(int index = 0; index < steps.size(); index++){
            if(steps.get(index).getStepType() == Step.TYPE_MATATU){
                matatuRoutes.add(steps.get(index).getRoute());
            }
        }

        return matatuRoutes;
    }
    
    public double getScore(){
        /*
        1. number of steps (five points per step)
        2. total number of stops in between (two points per stop)
        3. total distance walked (one point per 10m)
         */

        double stepScore = SCORE_STEP * steps.size();
        double totalDistanceWalked = 0;

        //get distances from actual from and to points
        if(steps.get(0).getStepType() == Step.TYPE_MATATU){
            if(steps.get(0).getStart() != null){
                totalDistanceWalked = totalDistanceWalked + steps.get(0).getStart().getDistance(from);
            }
        }

        if(steps.get(steps.size() - 1).getStepType() == Step.TYPE_MATATU){
            if(steps.get(steps.size() - 1).getDestination() != null){
                totalDistanceWalked = totalDistanceWalked + steps.get(steps.size() - 1).getDestination().getDistance(to);
            }
        }

        for(int index = 0; index < steps.size(); index++){
            if(steps.get(index).getStepType() == Step.TYPE_WALKING){
                totalDistanceWalked = totalDistanceWalked + steps.get(index).getStart().getDistance(steps.get(index).getDestination().getLatLng());
            }
        }
        double stopScore = noStops * SCORE_STOP;
        //TODO: get the actual route stops in the commute routes and not just all the stops
        double walkingScore = SCORE_WALKING * totalDistanceWalked;

        return stepScore + stopScore + walkingScore;
    }

    public ArrayList<LatLngPair> getStepLatLngPairs(ArrayList<LatLngPair> currLatLngPairs){
        if(currLatLngPairs == null){
            currLatLngPairs = new ArrayList<LatLngPair>();
        }

        for(int stepIndex = 0; stepIndex < steps.size(); stepIndex++){
            LatLngPair currLatLngPair = new LatLngPair(steps.get(stepIndex).getStart().getLatLng(), steps.get(stepIndex).getDestination().getLatLng(), -1);

            LatLngPair startLatLngPair = null;
            LatLngPair destinationLatLngPair = null;
            boolean add = true;
            boolean addStart = false;
            boolean addDestination = false;
            if(stepIndex == 0){
                startLatLngPair = new LatLngPair(from, steps.get(stepIndex).getStart().getLatLng(), -1);
                addStart = true;
            }
            if(stepIndex == steps.size() - 1){
                destinationLatLngPair = new LatLngPair(steps.get(stepIndex).getDestination().getLatLng(), to, -1);
                addDestination = true;
            }
            for(int lIndex = 0; lIndex < currLatLngPairs.size(); lIndex++){
                if(startLatLngPair != null && currLatLngPairs.get(lIndex).equals(startLatLngPair)){
                    addStart = false;
                }
                if(destinationLatLngPair != null && currLatLngPairs.get(lIndex).equals(destinationLatLngPair)){
                    addDestination = false;
                }
                if(currLatLngPairs.get(lIndex).equals(currLatLngPair)){
                    add = false;
                    break;
                }
            }

            if(add == true){
                currLatLngPairs.add(currLatLngPair);
            }
            if(addStart == true && startLatLngPair != null){
                currLatLngPairs.add(startLatLngPair);
            }
            if(addDestination == true && destinationLatLngPair != null){
                currLatLngPairs.add(destinationLatLngPair);
            }
        }

        return currLatLngPairs;
    }

    /**
     * Please run this method in an thread running asynchronously from the main thread
     * This method calculates the time it would take for the commute.
     *
     * @param latLngPairs   Dictionary of LatLngPairs where you can get distances for all the steps
     */
    public void setCommuteTime(ArrayList<LatLngPair> latLngPairs){
        time = -1;
        double totalTime = 0;

        //get distance between start and first step
        LatLngPair startLatLngPair = new LatLngPair(from, steps.get(0).getStart().getLatLng(), -1);
        double startDistance = -1;
        for(int lIndex = 0; lIndex < latLngPairs.size(); lIndex++){
            if(latLngPairs.get(lIndex).equals(startLatLngPair)){
                startDistance = latLngPairs.get(lIndex).getDistance();
                break;
            }
        }
        if(startDistance == -1){
            return;
        }
        else {
            Log.d(TAG, "Start distance is "+startDistance);
            totalTime = totalTime + (startDistance/SPEED_WALKING);
        }

        //get distance between last step and destination
        LatLngPair destinationLatLngPair = new LatLngPair(steps.get(steps.size() - 1).getDestination().getLatLng(), to, -1);
        double destinationDistance = -1;
        for(int lIndex = 0; lIndex < latLngPairs.size(); lIndex++){
            if(latLngPairs.get(lIndex).equals(destinationLatLngPair)){
                destinationDistance = latLngPairs.get(lIndex).getDistance();
                break;
            }
        }
        if(destinationDistance == -1){
            return;
        }
        else {
            Log.d(TAG, "Destination distance is "+destinationDistance);
            totalTime = totalTime + (destinationDistance/SPEED_WALKING);
        }

        //get distances in between steps
        for(int stepIndex = 0; stepIndex < steps.size(); stepIndex++){
            LatLngPair stepLatLngPair = new LatLngPair(steps.get(stepIndex).getStart().getLatLng(), steps.get(stepIndex).getDestination().getLatLng(), -1);
            double distance = -1;
            for(int lIndex = 0; lIndex < latLngPairs.size(); lIndex++){
                if(latLngPairs.get(lIndex).equals(stepLatLngPair)){
                    Log.d(TAG, latLngPairs.get(lIndex).getPointA()+" : "+latLngPairs.get(lIndex).getPointB()+" matches with "+stepLatLngPair.getPointA()+" : "+stepLatLngPair.getPointB());
                    distance = latLngPairs.get(lIndex).getDistance();
                    break;
                }
            }

            //return -1. Total distance should be atomic
            if(distance == -1){
                return;
            }

            if(steps.get(stepIndex).getStepType() == Step.TYPE_MATATU){
                totalTime = totalTime + (distance/SPEED_MATATU);
            }
            else if(steps.get(stepIndex).getStepType() == Step.TYPE_WALKING){
                totalTime = totalTime + (distance/SPEED_WALKING);
            }
        }
        Log.d(TAG, "Estimated time for commute is "+String.valueOf(totalTime));
        time = totalTime;
    }

    /**
     * This data carrier class store commute steps. Steps can either be walking or matatu steps
     */
    @PersistenceCapable
    public static class Step {
        public static  final String PARCELABLE_KEY = "Commute.Step";
        private static final int PARCELABLE_DESC = 4322;

        public static final int TYPE_MATATU = 0;
        public static final int TYPE_WALKING = 1;

        @PrimaryKey
        @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
        private Long id;
        
        @Persistent
        private Route route;
        
        @Persistent
        private Stop start;
        
        @Persistent
        private Stop destination;//destination stop regardless of whether current step is walking or in a matatu
        
        @Persistent
        private int stepType;
        
        @Persistent
        private ArrayList<LatLng> polyline;

        public Step(){
            route = null;
            start = null;
            destination = null;
            stepType = -1;
            polyline = new ArrayList<LatLng>();
        }

        public Step(int stepType){
            this.stepType = stepType;
            this.route = null;
            this.start = null;
            this.destination = null;
            polyline = new ArrayList<LatLng>();
        }

        public Step(int stepType, Route route, Stop start, Stop destination){
            this.stepType = stepType;
            this.route = route;
            this.start = start;
            this.destination = destination;
            polyline = new ArrayList<LatLng>();
        }
        
        public Long getId(){
        	return this.id;
        }
        
        public void setId(Long id){
        	this.id = id;
        }

        public void setPolyline(ArrayList<LatLng> polyline){
            this.polyline = polyline;
        }

        public int getStepType(){
            return stepType;
        }

        public Route getRoute() {
            return route;
        }

        public Stop getStart() {
            return start;
        }

        public Stop getDestination() {
            return destination;
        }
        
        public JSONObject getJSONObject(int sequence) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("sequence", sequence);
                jsonObject.put("start", start.getJSONObject());
                jsonObject.put("destination", destination.getJSONObject());
                if(stepType == TYPE_MATATU) {
                    jsonObject.put("type", "matatu");
                    jsonObject.put("route", route.getJSONObject(start, destination));
                    jsonObject.put("text", "Take a "+route.getLongName()+" ("+route.getShortName()+") from "+start.getName()+" to "+destination.getName());
                }
                else if(stepType == TYPE_WALKING) {
                    jsonObject.put("type", "walking");
                    jsonObject.put("text", "Walk to "+destination.getName());
                }
                return jsonObject;
            }
            catch (JSONException e) {
                Log.e(TAG, "An error occurred while trying to create a JSONObject for this object");
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * This data carrier class stores a commute segment which is essentially a GIS polyline
     */
    public static class Segment {

        public static final int TYPE_MATATU = 0;
        public static final int TYPE_WALKING = 1;
        public static final String PARCELABLE_KEY = "Commute.Segment";
        public static final int PARCELABLE_DESC = 2213;

        private ArrayList<LatLng> polyline;
        private int type;

        /**
         * Constructor for the Segment class.
         *
         * @param polyline  List of LatLngs that make the segment's polyline
         * @param type      Type of segment. Can either be TYPE_WALKING or TYPE_MATATU
         */
        public Segment(ArrayList<LatLng> polyline, int type){
            this.polyline = polyline;
            this.type = type;
            Log.d(TAG, "Commute segment initialized using an already decoded polyline. Polyline has "+polyline.size() +" points");
        }

        public Segment(JSONObject directionsAPIJsonObject, int type){
            this.type = type;
            polyline = new ArrayList<LatLng>();
            try {
                if(directionsAPIJsonObject.getString("status").equals("OK")){
                    JSONArray routes = directionsAPIJsonObject.getJSONArray("routes");
                    if(routes.length() > 0){
                        String encodedPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points");
                        Log.d(TAG, "Encoded polyline = " + encodedPolyline);
                        polyline = decodePolyline(encodedPolyline);
                        Log.d(TAG, "Commute segment initialized using an encoded polyline. Polyline has "+polyline.size()+" points");
                    }
                    else {
                        Log.e(TAG, "Directions API provided to constructor does not have routes");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "An error occurred while trying to parse JSON from Google Directions API while trying to initialize Segment using Directions API json output");
            }
        }

        /**
         * This method decodes polylines encoded using the algorithm described in
         * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
         * Code obtained from https://github.com/googlemaps/android-maps-utils
         *
         * @param encodedPath   String representing an encoded polyline
         * @return  A list of the decoded LatLngs representing the polyline
         */
        private ArrayList<LatLng> decodePolyline(final String encodedPath) {
            int len = encodedPath.length();

            // For speed we preallocate to an upper bound on the final length, then
            // truncate the array before returning.
            final ArrayList<LatLng> path = new ArrayList<LatLng>();
            int index = 0;
            int lat = 0;
            int lng = 0;

            while (index < len) {
                int result = 1;
                int shift = 0;
                int b;
                do {
                    b = encodedPath.charAt(index++) - 63 - 1;
                    result += b << shift;
                    shift += 5;
                } while (b >= 0x1f);
                lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

                result = 1;
                shift = 0;
                do {
                    b = encodedPath.charAt(index++) - 63 - 1;
                    result += b << shift;
                    shift += 5;
                } while (b >= 0x1f);
                lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

                path.add(new LatLng(lat * 1e-5, lng * 1e-5));
            }

            return path;
        }

        public ArrayList<LatLng> getPolyline() {
            return polyline;
        }

        public int getType() {
            return type;
        }
    }

    /**
     * This class is used to compare two commutes' scores.
     * Can be used with List.sort
     */
    public static class ScoreComparator implements Comparator<Commute> {

        @Override
        public int compare(Commute c0, Commute c1) {
            double s0 = c0.getScore();
            double s1 = c1.getScore();

            if(s0 < s1){
                return -1;
            }
            else if(s0 == s1){
                return 0;
            }
            else {
                return 1;
            }
        }
    }
}
