package org.ma3map.api.carriers;

import java.util.ArrayList;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.ma3map.api.helpers.JSONArray;
import org.ma3map.api.helpers.JSONObject;
import org.ma3map.api.handlers.Log;

import org.json.JSONException;

@PersistenceCapable
public class Line {
	private static final String TAG = "ma3map.Line";

    public static final String PARCELABLE_KEY = "Line";
    private static final int PARCELABLE_DESC = 9324;
    public static final String[] ALL_COLUMNS = new String[]{"line_id", "route_id", "direction_id"};

    @PrimaryKey
    @Persistent
    private String id;
    
    @Persistent
    private int directionID;
    
    @Persistent
    private ArrayList<Stop> stops;
    
    @Persistent
    private ArrayList<Point> points;
    
    public Line() {
        id = null;
        directionID = -1;
        stops = new ArrayList<Stop>();
        points = new ArrayList<Point>();
    }
    

    public Line(JSONObject lineData) throws JSONException{
        id = lineData.getString("line_id");
        directionID = lineData.getInt("direction_id");

        stops = new ArrayList<Stop>();
        JSONArray stopData = lineData.getJSONArray("stops");
        for(int sIndex = 0; sIndex < stopData.length(); sIndex++){
            stops.add(new Stop(stopData.getJSONObject(sIndex)));
        }

        points = new ArrayList<Point>();
        JSONArray pointData = lineData.getJSONArray("points");
        for(int pIndex = 0; pIndex < pointData.length(); pIndex++){
            points.add(new Point(pointData.getJSONObject(pIndex)));
        }
    }
    
    public  org.json.JSONObject getJSONObject() {
        org.json.JSONObject jsonObject = new org.json.JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("direction_id", directionID);
            org.json.JSONArray stopArray = new org.json.JSONArray();
            for(int i = 0; i < stops.size(); i++){
                stopArray.put(stops.get(i).getJSONObject());
            }
            jsonObject.put("stops", stopArray);
            org.json.JSONArray pointArray = new org.json.JSONArray();
            for(int i = 0; i < points.size(); i++){
                pointArray.put(points.get(i).getJSONObject());
            }
            jsonObject.put("", pointArray);
            return jsonObject;
        }
        catch(JSONException e) {
            Log.e(TAG, "An error occurred while trying to create a JSONObject for this object");
        }
        return null;
    }

    public  org.json.JSONObject getJSONObject(Stop endPointA, Stop endPointB) {
        org.json.JSONObject jsonObject = new org.json.JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("direction_id", directionID);

            ArrayList<LatLng> polyline = getPolyline(endPointA, endPointB);
            ArrayList<Stop> includedStops = new ArrayList<Stop>();
            for(int i = 0; i < polyline.size(); i++){
                LatLng currPoint = polyline.get(i);
                for(int j = 0; j < stops.size(); j++) {
                    if(stops.get(j).getDistance(currPoint) < 100) {//current stop is at most 100m from the polyline
                        includedStops.add(stops.get(j));
                    }
                }
            }
            org.json.JSONArray stopArray = new org.json.JSONArray();
            for(int i = 0; i < includedStops.size(); i++){
                stopArray.put(includedStops.get(i).getJSONObject());
            }
            jsonObject.put("stops", stopArray);
            
            jsonObject.put("polyline", LatLng.encodePolyline(polyline));
            return jsonObject;
        }
        catch(JSONException e) {
            Log.e(TAG, "An error occurred while trying to create a JSONObject for this object");
        }
        return null;
    }

    public String getId(){
    	return this.id;
    }
    
    public void setId(String id){
    	this.id = id;
    }
    
    public boolean isStopInLine(Stop stop){
        for(int index = 0; index < stops.size(); index++){
            //if(stops.get(index).getLat().equals(stop.getLat()) && stops.get(index).getLon().equals(stop.getLon())) {
            if(stops.get(index).equals(stop)) {
                return true;
            }
        }
        return false;
    }

    public double getDistanceToStop(Stop stop){
        //first check if stop is in line
        if(isStopInLine(stop)){
            return 0;
        }
        else {
            double closest = -1;
            for(int index = 0; index < stops.size(); index++){
                double currDistance = stops.get(index).getDistance(stop.getLatLng());
                if(closest == -1 || currDistance < closest){
                    closest = currDistance;
                }
            }
            return closest;
        }
    }

    public Stop getClosestStop(Stop stop){
        Stop closestStop = null;
        double closestDistance = -1;
        for(int index = 0; index < stops.size(); index++){
            double currDistance = stops.get(index).getDistance(stop.getLatLng());
            if(closestDistance == -1 || currDistance < closestDistance){
                closestStop = stops.get(index);
                closestDistance = currDistance;
            }
        }

        return closestStop;
    }
    
    /**
     * Returns the GIS polyline corresponding to this line with endPointA and endPointB
     * being the endpoints in the polyline
     *
     * @param endPointA     The first endpoint in the polyline
     * @param endPointB     The second endpoint in hte polyline
     *
     * @return  The GIS line corresponding to this line (ordered by the points' sequence ids)
     */
    public ArrayList<LatLng> getPolyline(Stop endPointA, Stop endPointB){
        ArrayList<LatLng> polyline = new ArrayList<LatLng>();

        if(points != null){
            //get the sequence number for the point closest to endPointA
            int endPointASN = points.get(0).getSequence();
            double endPointACD = endPointA.getDistance(points.get(0).getLatLng());
            for(int pointIndex = 1; pointIndex < points.size(); pointIndex++){
                double currDistance = endPointA.getDistance(points.get(pointIndex).getLatLng());
                if(currDistance < endPointACD){
                    endPointACD = currDistance;
                    endPointASN = points.get(pointIndex).getSequence();
                }
            }

            //get the sequence number for the point closest to endPointB
            int endPointBSN = points.get(0).getSequence();
            double endPointBCD = endPointB.getDistance(points.get(0).getLatLng());
            for(int pointIndex = 1; pointIndex < points.size(); pointIndex++){
                double currDistance = endPointB.getDistance(points.get(pointIndex).getLatLng());
                if(currDistance < endPointBCD){
                    endPointBCD = currDistance;
                    endPointBSN = points.get(pointIndex).getSequence();
                }
            }

            int seq1 = -1;
            int seq2 = -1;

            if(endPointASN < endPointBSN){
                seq1 = endPointASN;
                seq2 = endPointBSN;
            }
            else {
                seq2 = endPointASN;
                seq1 = endPointBSN;
            }

            for(int pointIndex = 0; pointIndex < points.size(); pointIndex++){
                if(points.get(pointIndex).getSequence() >= seq1 && points.get(pointIndex).getSequence() <= seq2){
                    polyline.add(points.get(pointIndex).getLatLng());
                }
            }
        }
        return polyline;
    }
    
    public ArrayList<Stop> getStops(){
        return this.stops;
    }
}
