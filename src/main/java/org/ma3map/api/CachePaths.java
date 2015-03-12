package org.ma3map.api;

import java.io.IOException;
import java.util.ArrayList;
import java.lang.System;
import java.lang.Object;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;

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

import org.apache.commons.lang.StringEscapeUtils;
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
    private static final String CACHE_DB = "db";
    private static final String CACHE_FILE = "file";
    private String cacheType;
    

    /**
    * Entry point for the /cache_paths endpoint
    *
    * @return Stringified JSONArray of the alternative paths
    */
    @GET
    @Produces("application/json")
    public String start(@QueryParam("to") String to) {
        Log.i(TAG, "API called");
        cacheType = CACHE_FILE;//default cache type
        if(to != null && to.equals(CACHE_DB)) {
            cacheType = CACHE_DB;
        }
        Data dataHandler = new Data();
        if(!dataHandler.fileExists(Data.BLOCK_PATH_CACHING)) {
            dataHandler.createFile(Data.BLOCK_PATH_CACHING);
            if(cacheType.equals(CACHE_FILE)) {
                dataHandler.deleteFile(Data.CACHE_PATHS);
            }
            GetStopsProgressListener getStopsProgressListener = new GetStopsProgressListener();
            dataHandler.addProgressListener(getStopsProgressListener);
            dataHandler.getStopData();
            return "DONE";
        }
        return "RUNNING";
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
                Database databaseHandler = null;
                if(cacheType.equals(CACHE_DB)) databaseHandler = new Database();
                int commuteIndex = 1;
                int pathIndex = 1;
                int stepIndex = 1;
                for(int i = 0; i < stops.size(); i++) {
                    Log.i(TAG, "Currently at "+String.valueOf(i+1)+" of "+String.valueOf(stops.size())+" stops");
                    for(int j = 0; j < stops.size(); j++) {
                        if(i != j) {
                            JSONObject pathObject = dataHandler.getPaths(stops.get(i).getLatLng(), 1, stops.get(j).getLatLng(), 2);
                            try {
                                JSONArray paths = pathObject.getJSONArray("paths");
                                String rawTime = pathObject.getString("time_taken");
                                double time = Double.parseDouble(rawTime.replace("ms", ""));
                                int commuteId = commuteIndex;
                                commuteIndex++;
                                if(cacheType.equals(CACHE_DB)) {
                                    //commute(id serial primary key, start_id varchar, destination_id varchar, processing_time double precision)
                                    String cQuery = "insert into commute(start_id, destination_id, processing_time) values(?, ?, ?)";
                                    Log.d(TAG, cQuery);
                                    PreparedStatement cps = databaseHandler.getConnection().prepareStatement(cQuery, Statement.RETURN_GENERATED_KEYS);
                                    cps.setString(1, stops.get(i).getId());
                                    cps.setString(2, stops.get(j).getId());
                                    cps.setDouble(3, time);
                                    commuteId = databaseHandler.execInsertQuery(cps);
                                }
                                else if(cacheType.equals(CACHE_FILE)) {
                                    String cQuery = "insert into commute(id, start_id, destination_id, processing_time) values(";
                                    cQuery = cQuery + String.valueOf(commuteId);
                                    cQuery = cQuery + ", '" + StringEscapeUtils.escapeSql(stops.get(i).getId()) + "'";
                                    cQuery = cQuery + ", '" + StringEscapeUtils.escapeSql(stops.get(j).getId()) + "'";
                                    cQuery = cQuery + ", " + String.valueOf(time) + ");";
                                    dataHandler.addStringToFile(Data.CACHE_PATHS, cQuery);
                                }
                                //cps.close();
                                for(int pI = 0; pI < paths.length(); pI++) {
                                    JSONObject currPath = paths.getJSONObject(pI);
                                    double score = currPath.getDouble("score");
                                    int pathId = pathIndex;
                                    pathIndex++;
                                    if(cacheType.equals(CACHE_DB)) {
                                        String pQuery = "insert into commute_path(score, commute_id) values(?, ?)";
                                        Log.d(TAG, pQuery);
                                        PreparedStatement pps = databaseHandler.getConnection().prepareStatement(pQuery, Statement.RETURN_GENERATED_KEYS);
                                        pps.setDouble(1, score);
                                        pps.setInt(2, commuteId);
                                        pathId = databaseHandler.execInsertQuery(pps);
                                    }
                                    else if(cacheType.equals(CACHE_FILE)) {
                                        String pQuery = "insert into commute_path(id, score, commute_id) values(";
                                        pQuery = pQuery + String.valueOf(pathId);
                                        pQuery = pQuery + ", " + String.valueOf(score);
                                        pQuery = pQuery + ", " + String.valueOf(commuteId) + ");";
                                        dataHandler.addStringToFile(Data.CACHE_PATHS, pQuery);
                                    }
                                    //pps.close();
                                    JSONArray steps = currPath.getJSONArray("steps");
                                    int stepSeq = 0;
                                    for(int sI = 0; sI < steps.length(); sI++) {
                                        JSONObject currStep = steps.getJSONObject(sI);
                                        if(currStep.getString("type").equals("matatu")) {
                                            //commute_step(id serial primary key, commute_path_id integer references commute_path(id), text varchar, sequence integer, start_id varchar, destination_id varchar, route_id varchar);
                                            int stepId = stepIndex;
                                            stepIndex++;
                                            if(cacheType.equals(CACHE_DB)) {
                                                String sQuery = "insert into commute_step(commute_path_id, sequence, start_id, destination_id, route_id) values(?, ?, ?, ?, ?)";
                                                PreparedStatement sps = databaseHandler.getConnection().prepareStatement(sQuery, Statement.RETURN_GENERATED_KEYS);
                                                sps.setInt(1, pathId);
                                                sps.setInt(3, stepSeq);
                                                sps.setString(4, currStep.getJSONObject("start").getString("id"));
                                                sps.setString(5, currStep.getJSONObject("destination").getString("id"));
                                                sps.setString(6, currStep.getJSONObject("route").getString("id"));
                                                databaseHandler.execInsertQuery(sps);
                                            }
                                            else if(cacheType.equals(CACHE_FILE)) {
                                                String sQuery = "insert into commute_step(id, commute_path_id, sequence, start_id, destination_id, route_id) values(";
                                                sQuery = sQuery + String.valueOf(stepId);
                                                sQuery = sQuery + ", " + String.valueOf(pathId);
                                                sQuery = sQuery + ", " + String.valueOf(stepSeq);
                                                sQuery = sQuery + ", '" + StringEscapeUtils.escapeSql(currStep.getJSONObject("start").getString("id")) + "'";
                                                sQuery = sQuery + ", '" + StringEscapeUtils.escapeSql(currStep.getJSONObject("destination").getString("id")) + "'";
                                                sQuery = sQuery + ", '" + StringEscapeUtils.escapeSql(currStep.getJSONObject("route").getString("id")) + "');";
                                                dataHandler.addStringToFile(Data.CACHE_PATHS, sQuery);
                                            }
                                            //sps.close();
                                            stepSeq++;
                                        }
                                    }
                                }
                            } catch(JSONException e) {
                                Log.e(TAG, "An error occurred while trying to parse JSON data from /get_paths endpoint");
                                e.printStackTrace();
                            }
                            catch(SQLException e) {
                                Log.e(TAG, "An error occurred while trying to access the database");
                                e.printStackTrace();
                            }
                        }
                    }
                }
                dataHandler.deleteFile(Data.BLOCK_PATH_CACHING);
            }
            else {
                Log.e(TAG, "Could not fetch stop data from the server");
            }
        }
    }
}
