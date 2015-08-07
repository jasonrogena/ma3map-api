package org.ma3map.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.lang.System;
import java.lang.Object;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;

import org.ma3map.api.carriers.Route;
import org.ma3map.api.carriers.Stop;
import org.ma3map.api.carriers.StopPair;
import org.ma3map.api.handlers.Data;
import org.ma3map.api.handlers.Graph;
import org.ma3map.api.handlers.Log;
import org.ma3map.api.listeners.ProgressListener;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;

import org.neo4j.graphdb.Node;

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
@Path("/create_graph")
public class CreateGraph {

    private static final String TAG = "ma3map.CreateGraph";

    private long timeTaken;

    /**
    * Entry point for the /get_paths endpoint
    *
    * @return Stringified JSONArray of the alternative paths
    */
    @GET
    @Produces("application/json")
    public String start(@QueryParam("force") Boolean force) {
        Log.i(TAG, "API called");
        Data dataHandler = new Data();
        if(!dataHandler.fileExists(Data.BLOCK_GRAPH_CREATION)) {
            dataHandler.createFile(Data.BLOCK_GRAPH_CREATION);
            constructGraph();
            return "DONE";
        }
        return "RUNNING";
    }

    private void constructGraph() {
        Data data = new Data();
        Log.i(TAG, "Getting all routes");
        ArrayList<Route> routes = data.getRouteData();
        Log.i(TAG, "Getting all stops");
        ArrayList<Stop> stops = data.getStopData();
        Graph graph = new Graph(routes, stops, false);
        graph.deleteGraph();

        //for each of the stops, get a list of all the routes that contain it
        Map<String, ArrayList<String>> stopRoutes = new HashMap<String, ArrayList<String>>();
        for(int sIndex = 0; sIndex < stops.size(); sIndex++) {
            Log.i(TAG, "Indexing stops and routes", (sIndex + 1), stops.size());
            Stop currStop = stops.get(sIndex);
            ArrayList<String> routesWithCurrStop = new ArrayList<String>();
            for(int rIndex = 0; rIndex < routes.size(); rIndex++) {
                if(routes.get(rIndex).isStopInRoute(currStop)){
                    routesWithCurrStop.add(routes.get(rIndex).getId());
                }
            }
            stopRoutes.put(currStop.getId(), routesWithCurrStop);
        }
        Log.i(TAG, "Number of indexed stops = "+String.valueOf(stopRoutes.size()));
        ArrayList<StopPair> stopPairs = new ArrayList<StopPair>();
        //now compare pairs of all the stops and add them to the graph
        for(int sIndex = 0; sIndex < stops.size(); sIndex++) {
            Log.i(TAG, "Adding stops to graph", (sIndex + 1), stops.size());
            Stop currStop = stops.get(sIndex);
            if(stopRoutes.get(currStop.getId()).size() > 0){
                for(int oIndex = 0; oIndex < stops.size(); oIndex++) {
                    if(!currStop.equals(stops.get(oIndex)) && stopRoutes.get(stops.get(oIndex).getId()).size() > 0) {//make sure you dont compare a stop to itself
                        StopPair currPair = new StopPair(currStop, stops.get(oIndex));
                        if(!stopPairs.contains(currPair)) {
                            stopPairs.add(currPair);
                            //check if the two stops have at least one common route
                            ArrayList<String> commonRouteIds = stopRoutes.get(currPair.getA().getId());
                            commonRouteIds.retainAll(stopRoutes.get(currPair.getB().getId()));
                            boolean areSisters = false;
                            if(commonRouteIds.size() > 0) {
                                areSisters = true;
                            }
                            Node nodeA = graph.createNode(currPair.getA().getId());
                            Node nodeB = graph.createNode(currPair.getB().getId());
                            graph.createRelationship(nodeA, nodeB, currPair.getA().getDistance(currPair.getB().getLatLng()), areSisters);
                        }
                    }
                }
            }
        }
        graph.printGraphStats();
        Node node1 = graph.getNode("0311BAO");
        Node node2 = graph.getNode("0210KNJ");
        ArrayList<org.ma3map.api.carriers.Path> paths = graph.getPaths(node1, node2);

        graph.close();
        data.deleteFile(Data.BLOCK_GRAPH_CREATION);
        Log.i(TAG, "Done creating the graph");
    }
}
