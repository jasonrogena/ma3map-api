package org.ma3map.api.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.ma3map.api.carriers.Route;
import org.ma3map.api.carriers.Stop;
import org.ma3map.api.helpers.JSONArray;
import org.ma3map.api.listeners.ProgressListener;

import org.json.JSONException;

public class Data extends ProgressHandler {
	private static final String TAG = "ma3map.Data";

    private static final String URL_MA3MAP = "http://api.ma3map.org";
    private static final int HTTP_POST_TIMEOUT = 20000;
    private static final int HTTP_RESPONSE_TIMEOUT = 200000;

    private static final String API_GOOGLE_PLACES_URL = "https://maps.googleapis.com/maps/api/place";
    private static final String API_GOOGLE_DISTANCE_MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix";
    private static final String API_GOOGLE_DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions";
    private static final String API_MA3MAP_URI_GET_ROUTES = "/get/routes";
    private static final String API_MA3MAP_URI_GET_STOPS = "/get/stops";
    private static final String API_MA3MAP_URI_SEARCH = "/search";

    public static final String DIRECTIONS_WALKING = "walking";
    public static final String DIRECTIONS_DRIVING = "driving";

    private static final String CACHE_ROUTES = "cache/route.json";
    private static final String CACHE_STOPS = "cache/stops.json";

    /**
     * Default constructor for this class;
     */
    public Data(){
    	
    }
    
    public ArrayList<Route> getRouteData(){
        //check if cache exists
             
    	try {
            String jsonString = null;
            File cache = new File(CACHE_ROUTES);
            if(cache.exists()) {
                Log.i(TAG, "Route data cache exists. No need to get from get/routes API");
                BufferedReader br = new BufferedReader(new FileReader(CACHE_ROUTES));
                try {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                        line = br.readLine();
                    }
                    jsonString = sb.toString();
                } 
                finally {
                   br.close();
                }
            }
            else {
                //no route data cache. Fetch route data from the getRouteData API
                Log.i(TAG, "Initialising connection to ma3map get route data API");
                updateProgressListeners(0, 0, "Getting route data from the getRouteData API", ProgressListener.FLAG_WORKING);
                URL apiURL = new URL(URL_MA3MAP+API_MA3MAP_URI_GET_ROUTES);
                BufferedReader reader = new BufferedReader(new InputStreamReader(apiURL.openStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    output.append(line);
                }
                reader.close();
                jsonString = output.toString();
                BufferedWriter writer = null;
                try {
                    //create file and write json
                    cache.getParentFile().mkdirs();
                    cache.createNewFile();
                    writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(CACHE_ROUTES), "utf-8"));
                    writer.write(jsonString);
                }
                finally {
                    if(writer != null)
                        writer.close();
                }
            }	
			Log.i(TAG, "Data gotten from ma3map get route data API. Processing it starting now");
            if(jsonString != null){
               try {
                   ArrayList<Route> routes = null;
                   updateProgressListeners(0, 0, "Decoding the route data", ProgressListener.FLAG_WORKING);
                   JSONArray rawRouteData = new JSONArray(jsonString);
                   for(int index = 0; index < rawRouteData.length(); index++){
                       updateProgressListeners((index+1), rawRouteData.length(), "Initialising routing objects", ProgressListener.FLAG_WORKING);
                       Route currRoute = new Route(rawRouteData.getJSONObject(index));
                       
                       if(routes == null){
                           routes = new ArrayList<Route>();
                       }
                       
                       routes.add(currRoute);
                   }
                   
                   Log.i(TAG, "Finished initialising route data");
                   finalizeProgressListeners(routes, "Done getting and initialising route data", ProgressListener.FLAG_DONE);
                   return routes;
               } catch (JSONException e) {
                   // TODO Auto-generated catch block
                   finalizeProgressListeners(null, "An error occurred while trying to decode the route data", ProgressListener.FLAG_ERROR);
                   Log.e(TAG, "JSONException thrown while trying to generate a JSONArray using data from the ma3map get route data API");
                   e.printStackTrace();
               }
            }
			
		} catch (MalformedURLException e) {
			finalizeProgressListeners(null, "An error occurred while trying to connect to the server hosting the getRouteData API", ProgressListener.FLAG_ERROR);
			Log.e(TAG, "MalformedURLException thrown while trying to conntect to the get route data url");
			e.printStackTrace();
		}
    	catch (IOException e){
    		finalizeProgressListeners(null, "An error occurred while trying to get data from the getRouteData API", ProgressListener.FLAG_ERROR);
    		Log.e(TAG, "IOException thrown while trying to initialise an IOStream to the get route data url");
    		e.printStackTrace();
    	}
    	return null;
    }
    
    public ArrayList<Stop> getStopData(){
        //check if cache exists

    	try {
            String jsonString = null;
            File cache = new File(CACHE_STOPS);
            if(cache.exists()) {
                Log.i(TAG, "Stop data cache exists. No need to get from get/stops API");
                BufferedReader br = new BufferedReader(new FileReader(CACHE_STOPS));
                try {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                        line = br.readLine();
                    }
                    jsonString = sb.toString();
                } 
                finally {
                   br.close();
                }
            }
            else {
                //no stop data cache. Fetch stop data from the getStopData API
                Log.i(TAG, "Initialising connection to ma3map get stop data API");
                updateProgressListeners(0, 0, "Getting stop data from the getStopData API", ProgressListener.FLAG_WORKING);
                URL apiURL = new URL(URL_MA3MAP+API_MA3MAP_URI_GET_STOPS);
                BufferedReader reader = new BufferedReader(new InputStreamReader(apiURL.openStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null){
                    output.append(line);
                }
                reader.close();
                jsonString = output.toString();
                BufferedWriter writer = null;
                try {
                    //create file and write json
                    cache.getParentFile().mkdirs();
                    cache.createNewFile();
                    writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(CACHE_STOPS), "utf-8"));
                    writer.write(jsonString);
                }
                finally {
                    if(writer != null)
                        writer.close();
                }
            }	
			Log.i(TAG, "Data gotten from ma3map get stop data API. Processing it starting now");
            if(jsonString != null){
               try {
                   ArrayList<Stop> stops = null;
                   updateProgressListeners(0, 0, "Decoding the stop data", ProgressListener.FLAG_WORKING);
                   JSONArray rawStopData = new JSONArray(jsonString);
                   for(int index = 0; index < rawStopData.length(); index++){
                       updateProgressListeners((index+1), rawStopData.length(), "Initialising stop objects", ProgressListener.FLAG_WORKING);
                       Stop currStop = new Stop(rawStopData.getJSONObject(index));
                       
                       if(stops == null){
                           stops = new ArrayList<Stop>();
                       }
                       
                       stops.add(currStop);
                   }
                   
                   Log.i(TAG, "Finished initialising stop data");
                   finalizeProgressListeners(stops, "Done getting and initialising stop data", ProgressListener.FLAG_DONE);
                   return stops;
               } catch (JSONException e) {
                   // TODO Auto-generated catch block
                   finalizeProgressListeners(null, "An error occurred while trying to decode the stop data", ProgressListener.FLAG_ERROR);
                   Log.e(TAG, "JSONException thrown while trying to generate a JSONArray using data from the ma3map get stop data API");
                   e.printStackTrace();
               }
            }
			
		} catch (MalformedURLException e) {
			finalizeProgressListeners(null, "An error occurred while trying to connect to the server hosting the getStopData API", ProgressListener.FLAG_ERROR);
			Log.e(TAG, "MalformedURLException thrown while trying to conntect to the get stop data url");
			e.printStackTrace();
		}
    	catch (IOException e){
    		finalizeProgressListeners(null, "An error occurred while trying to get data from the getStopData API", ProgressListener.FLAG_ERROR);
    		Log.e(TAG, "IOException thrown while trying to initialise an IOStream to the get stop data url");
    		e.printStackTrace();
    	}
    	return null;
    }
}
