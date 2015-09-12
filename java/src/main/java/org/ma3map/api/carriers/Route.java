package org.ma3map.api.carriers;

import java.util.ArrayList;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.ma3map.api.helpers.JSONArray;
import org.ma3map.api.helpers.JSONObject;
import org.ma3map.api.handlers.Log;

import org.json.JSONException;

/**
 * @author Jason Rogena <jasonrogena@gmail.com>
 * @since 2015-02-13
 * 
 * This is a carrier class for a route.
 * <p>
 * Routes in ma3map are made up of lines. This is in line with
 *  
 */
@PersistenceCapable
public class Route {
	public static final String TAG = "ma3map.Route";
    public static final String PARCELABLE_KEY = "Route";
    private static final int PARCELABLE_DESC = 7522;
    public static final String[] ALL_COLUMNS = new String[]{"route_id", "route_short_name", "route_long_name", "route_desc", "route_type", "route_url", "route_color", "route_text_color"};

    @Persistent
    private String shortName;
    
    @Persistent
    private String longName;
    
    @PrimaryKey
    @Persistent
    private String id;
    
    @Persistent
    private String desc;
    
    @Persistent
    private int type;
    
    @Persistent
    private String url;
    
    @Persistent
    private String color;
    
    @Persistent
    private String textColor;
    
    @Persistent
    private ArrayList<Line> lines;
    
    /**
     * Default constructor for this class
     */
    public Route() {
        shortName = null;
        longName = null;
        id = null;
        desc = null;
        type = -1;
        url = null;
        color = null;
        textColor = null;
        lines = new ArrayList<Line>();
    }
    
    public Route(JSONObject routeData) throws JSONException{
        shortName = routeData.getString("route_short_name");
        longName = routeData.getString("route_long_name");
        id = routeData.getString("route_id");
        desc = routeData.getString("route_desc");
        type = routeData.getInt("route_type");
        url = routeData.getString("route_url");
        color = routeData.getString("route_color");
        textColor = routeData.getString("route_text_color");

        lines = new ArrayList<Line>();
        JSONArray lineData = routeData.getJSONArray("lines");
        for(int lIndex  = 0; lIndex < lineData.length(); lIndex++){
            lines.add(new Line(lineData.getJSONObject(lIndex)));
        }
    }
    
    public org.json.JSONObject getJSONObject() {
        org.json.JSONObject jsonObject = new org.json.JSONObject();
        try {
            jsonObject.put("short_name", shortName);
            jsonObject.put("long_name", longName);
            jsonObject.put("id", id);
            jsonObject.put("description", desc);
            jsonObject.put("type", type);
            jsonObject.put("url", url);
            jsonObject.put("color", color);
            jsonObject.put("text_color", textColor);
            org.json.JSONArray lineArray = new org.json.JSONArray();
            for(int i = 0; i < lines.size(); i++) {
                lineArray.put(lines.get(i).getJSONObject());
            }
            jsonObject.put("lines", lineArray);
            return jsonObject;
        }
        catch (JSONException e) {
            Log.e(TAG, "An error occurred while trying to create a JSONObject for this object");
            e.printStackTrace();
        }
        return null;
    }
    
    public org.json.JSONObject getJSONObject(Stop endPointA, Stop endPointB) {
        org.json.JSONObject jsonObject = new org.json.JSONObject();
        try {
            jsonObject.put("short_name", shortName);
            jsonObject.put("long_name", longName);
            jsonObject.put("id", id);
            jsonObject.put("description", desc);
            jsonObject.put("type", type);
            jsonObject.put("url", url);
            jsonObject.put("color", color);
            jsonObject.put("text_color", textColor);
            org.json.JSONArray lineArray = new org.json.JSONArray();
            for(int i = 0; i < lines.size(); i++) {
                lineArray.put(lines.get(i).getJSONObject(endPointA, endPointB));
            }
            jsonObject.put("lines", lineArray);
            return jsonObject;
        }
        catch (JSONException e) {
            Log.e(TAG, "An error occurred while trying to create a JSONObject for this object");
            e.printStackTrace();
        }
        return null;
    }
    
    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public String getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    public int getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getColor() {
        return color;
    }

    public String getTextColor() {
        return textColor;
    }

    public ArrayList<Line> getLines() {
        return lines;
    }

    public ArrayList<Stop> getStops(int lineIndex){
        return lines.get(lineIndex).getStops();
    }

    public boolean isStopInRoute(Stop stop){
        for(int index = 0; index < lines.size(); index++){
            if(lines.get(index).isStopInLine(stop)){
                return true;
            }
        }
        return false;
    }

    public double getDistanceToStop(Stop stop){
        double closest = -1;
        for(int index = 0; index < lines.size(); index++){
            double currDistance = lines.get(index).getDistanceToStop(stop);
            if(closest == -1 || currDistance < closest){
                closest = currDistance;
            }
        }
        return closest;
    }

    public Stop getClosestStop(Stop stop){
        Stop closestStop = null;
        double closestDistance = -1;
        for(int index = 0; index < lines.size(); index++){
            Stop currCloseStop = lines.get(index).getClosestStop(stop);

            if(closestDistance == -1 || (currCloseStop != null && currCloseStop.getDistance(stop.getLatLng()) < closestDistance)){
                closestStop = currCloseStop;
                if(currCloseStop != null){
                    closestDistance = currCloseStop.getDistance(stop.getLatLng());
                }
            }
        }

        return closestStop;
    }
   
    /**
    * This methods gets stops shared between two routes
    * 
    * @param r1     The first route
    * @param r2     The second route
    * @return A list of the shared stops
    */
    public static ArrayList<Stop> getSharedStops(Route r1, Route r2) {
        ArrayList<Stop> s1 = r1.getStops(0);
        ArrayList<Stop> s2 = r2.getStops(0);
        ArrayList<Stop> s = null;
        Route r = null;
        //determine which route stop list pair will have less comparisons to make
        if(s1.size() < s2.size()) {
            s = s1;
            r = r2;
        }
        else {
            s = s2;
            r = r1;
        }
        ArrayList<Stop> commonS = new ArrayList<Stop>();
        for(int i = 0; i < s.size(); i++) {
            if(r.isStopInRoute(s.get(i))){
                commonS.add(s.get(i));
            }
        }
        return commonS;
    }

    /**
    * This method returns a list of all the routes containing both the first and second provided stops
    * 
    * @param allRoutes  All the routes
    * @param a          The first stop
    * @param b          The second stop
    * @return A list of all the routes containing both stop a and b
    */
    public static ArrayList<Route> getDirectServiceRoutes(ArrayList<Route> allRoutes, Stop a, Stop b) {
        ArrayList<Route> serviceRoutes = new ArrayList<Route>();
        for(int i = 0; i < allRoutes.size(); i++) {
            if(allRoutes.get(i).isStopInRoute(a) && allRoutes.get(i).isStopInRoute(b)){
                serviceRoutes.add(allRoutes.get(i));
            }
        }
        return serviceRoutes;
    } 
    
    /**
     * This method gets the GIS polyline corresponding to this route. Start and destination will be
     * the endpoints in the polyline
     *
     * @param endPointA     The first endpoint in the polyline
     * @param endPointB     The second enpoint in the polyline
     * @return
     */
    public ArrayList<LatLng> getPolyline(Stop endPointA, Stop endPointB){
        ArrayList<LatLng> polyline = new ArrayList<LatLng>();
        for(int lineIndex = 0; lineIndex < lines.size(); lineIndex++){
            polyline.addAll(lines.get(lineIndex).getPolyline(endPointA, endPointB));
        }

        return polyline;
    }
}
