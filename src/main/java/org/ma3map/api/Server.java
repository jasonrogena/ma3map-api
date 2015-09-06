package org.ma3map.api;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.ma3map.api.carriers.Route;
import org.ma3map.api.carriers.Stop;
import org.ma3map.api.carriers.StopPair;
import org.ma3map.api.handlers.Data;
import org.ma3map.api.handlers.Graph;
import org.ma3map.api.handlers.Log;
import org.neo4j.graphdb.Node;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Server class.
 *
 */
public class Server {
    // Base URI the Grizzly HTTP server will listen on
    private static final String TAG = "ma3map.Server";
    public static final String BASE_URI = ":8080/ma3map/";

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() throws UnknownHostException, SocketException {
        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        String ip = null;
        for (; n.hasMoreElements();) {
            NetworkInterface e = n.nextElement();
            System.out.println("Interface: " + e.getName());
            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements();) {
                InetAddress addr = a.nextElement();
                if(!addr.isLoopbackAddress() && addr instanceof Inet4Address){
                    ip = addr.getHostAddress();
                    break;
                }
            }
            if(ip != null) break;
        }

        // create a resource config that scans for JAX-RS resources and providers
        // in org.ma3map.api package
        final ResourceConfig rc = new ResourceConfig().packages("org.ma3map.api");
        rc.register(new GraphBinder());
        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        Log.d(TAG, "About to start API on "+"http://"+ip+BASE_URI);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create("http://"+ip+BASE_URI), rc);
    }

    /**
     * Server method.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL\nHit enter to stop it..."));
        System.in.read();
        server.shutdown();
    }

    private static Graph buildGraph() {
        Data dataHandler = new Data();
        ArrayList<Stop> stops = dataHandler.getStopData();
        ArrayList<Route> routes = dataHandler.getRouteData();
        final HashMap<String, ArrayList<String>> stopRoutes = getStopRoutes(routes, stops);
        Log.i(TAG, "Number of indexed stops = " + String.valueOf(stopRoutes.size()));
        Graph graph = new Graph(routes, stops, false);
        if(!dataHandler.fileExists(Data.BLOCK_GRAPH_CREATION)) {
            dataHandler.createFile(Data.BLOCK_GRAPH_CREATION);
            graph.deleteGraph();

            //for each of the stops, get a list of all the routes that contain it
            ArrayList<StopPair> stopPairs = new ArrayList<StopPair>();
            //now compare pairs of all the stops and add them to the graph
            int totalRels = 0;
            int sisterRels = 0;
            int sisterRels2 = 0;
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
                                    sisterRels++;
                                }
                                Node nodeA = graph.createNode(currPair.getA().getId());
                                Node nodeB = graph.createNode(currPair.getB().getId());
                                boolean result = graph.createRelationship(nodeA, nodeB, currPair.getA().getDistance(currPair.getB().getLatLng()), areSisters);
                                if(result == true) {
                                    totalRels++;
                                    if(areSisters){
                                        sisterRels2++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            graph.printGraphStats();
            dataHandler.deleteFile(Data.BLOCK_GRAPH_CREATION);
            Log.i(TAG, "Done creating the graph");
        }
        return graph;
    }

    private static HashMap<String, ArrayList<String>> getStopRoutes(ArrayList<Route> routes, ArrayList<Stop> stops) {
        HashMap<String, ArrayList<String>> stopRoutes = new HashMap<String, ArrayList<String>>();
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
        return stopRoutes;
    }

    public static class GraphBinder extends AbstractBinder {

        @Override
        protected void configure() {
            Graph graph = buildGraph();
            bind(graph).to(Graph.class);
        }
    }
}

