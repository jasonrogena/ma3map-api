
package org.ma3map.api;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;
import org.ma3map.api.carriers.Route;
import org.ma3map.api.carriers.Stop;
import org.ma3map.api.carriers.StopPair;
import org.ma3map.api.handlers.Data;
import org.ma3map.api.handlers.Graph;
import org.ma3map.api.handlers.Log;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;


public class Main {
    public static final String TAG = "ma3map.Main";
    public static final URI BASE_URI = UriBuilder.fromUri("http://localhost/").port(9998).build();

    protected static SelectorThread startServer() throws IOException {
        final Map<String, String> initParams = new HashMap<String, String>();

        initParams.put("com.sun.jersey.config.property.packages", 
                "org.ma3map.api");

        System.out.println("Starting grizzly...");
        SelectorThread threadSelector = GrizzlyWebContainerFactory.create(BASE_URI, initParams);
        return threadSelector;
    }

    private static void buildGraph(){
        Data dataHandler = new Data();
        ArrayList<Stop> stops = dataHandler.getStopData();
        ArrayList<Route> routes = dataHandler.getRouteData();
        Graph graph = new Graph(routes, stops, false);
        if(!dataHandler.fileExists(Data.BLOCK_GRAPH_CREATION)) {
            dataHandler.createFile(Data.BLOCK_GRAPH_CREATION);
            graph.deleteGraph();

            //for each of the stops, get a list of all the routes that contain it
            Map<String, ArrayList<String>> stopRoutes = new HashMap<String, ArrayList<String>>();
            for (int sIndex = 0; sIndex < stops.size(); sIndex++) {
                Log.i(TAG, "Indexing stops and routes", (sIndex + 1), stops.size());
                Stop currStop = stops.get(sIndex);
                ArrayList<String> routesWithCurrStop = new ArrayList<String>();
                for (int rIndex = 0; rIndex < routes.size(); rIndex++) {
                    if (routes.get(rIndex).isStopInRoute(currStop)) {
                        routesWithCurrStop.add(routes.get(rIndex).getId());
                    }
                }
                stopRoutes.put(currStop.getId(), routesWithCurrStop);
            }
            Log.i(TAG, "Number of indexed stops = " + String.valueOf(stopRoutes.size()));
            ArrayList<StopPair> stopPairs = new ArrayList<StopPair>();
            //now compare pairs of all the stops and add them to the graph
            for (int sIndex = 0; sIndex < stops.size(); sIndex++) {
                Log.i(TAG, "Adding stops to graph", (sIndex + 1), stops.size());
                Stop currStop = stops.get(sIndex);
                if (stopRoutes.get(currStop.getId()).size() > 0) {
                    for (int oIndex = 0; oIndex < stops.size(); oIndex++) {
                        if (!currStop.equals(stops.get(oIndex)) && stopRoutes.get(stops.get(oIndex).getId()).size() > 0) {//make sure you dont compare a stop to itself
                            StopPair currPair = new StopPair(currStop, stops.get(oIndex));
                            if (!stopPairs.contains(currPair)) {
                                stopPairs.add(currPair);
                                //check if the two stops have at least one common route
                                ArrayList<String> commonRouteIds = stopRoutes.get(currPair.getA().getId());
                                commonRouteIds.retainAll(stopRoutes.get(currPair.getB().getId()));
                                boolean areSisters = false;
                                if (commonRouteIds.size() > 0) {
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
            dataHandler.deleteFile(Data.BLOCK_GRAPH_CREATION);
            Log.i(TAG, "Done creating the graph");
        }
    }
    
    public static void main(String[] args) throws IOException {
        buildGraph();
        SelectorThread threadSelector = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl",
                BASE_URI));
    }
}
