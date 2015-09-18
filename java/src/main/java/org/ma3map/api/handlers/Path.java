package org.ma3map.api.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ma3map.api.carriers.*;
import org.ma3map.api.listeners.ProgressListener;


/**
 * @author  Jason Rogena <jasonrogena@gmail.com>
 * @since   2014-11-17
 *
 * This Class is responsible for determining the best path between source and destination.
 *
 * <strong>ALGORITHM</strong>
 * <p></p>
 * <strong>Problem</strong>
 * 1. Given a set with source route stops S and a set with destinations route stops D, find paths
 *      between members of set S and set D in the least time possible.
 * 2. Order determined paths using predetermined path weights (not to be confused with edge weights
 *      explained in the next section).
 * <p>
 * <strong>Solution</strong>
 * The class Implements Batched Shortest Path Computation using Dijkstra's Algorithm.
 * Key elements in Dijkstra's Algorithm are nodes, edges/arcs and weights. In our case:
 *  - nodes: A section of a route, terminated on both ends by stops
 *  - edges/arcs: two nodes are connected to each other if the corresponding routes share the
 *                  terminating stop or the terminating stops for each of the two routes are
 *                  MAX_WALKING_DISTANCE away from each other
 *  - weight: the weight of the edge joining stop A and B is dependent on the walking distance from
 *              terminating stop at A to terminating stop at B
 *  - path: A plot from one of the potential starting points in the graph to a potential finishing
 *              point in the graph
 * <p>
 * <strong>Considerations</strong>
 * 1. The possibility of getting more than one edge with the least weight from a node is very high.
 *  In this algorithm, all the edges with the least weights from a node are considered. It is therefore
 *  possible to create more than one paths from a node.
 * <p>
 * <strong>Optimizations</strong>
 * 1. Path should have a maximum of MAX_NODES nodes
 * 2. Once the algorithm obtains MAX_COMMUTES paths, it terminates and does not continue traversing
 *      the graph
 * <p>
 * <strong>Future work</strong>
 * Since the classic Djkstra's algorithm is considered a naive algorithm [2], we consider solving the
 * same problem using faster algorithms presented in [1, 2]
 * <p>
 * <strong>References</strong>
 * @see <a href="https://dl.google.com/eclipse/plugin/4.4">1. D. Delling, A. Goldberg, and R. Werneck. Faster Batched Shortest Paths in Road Networks. (Accessed 18th Jan 2015)</a>
 * @see <a href="http://algo2.iti.kit.edu/download/diss_geisberger.pdf">2. R. Geisberger, et al. Advanced Route Planning in Transportation Networks. (Accessed 16th Jan 2015)</a>
 */
public class Path extends ProgressHandler {
	private static final String TAG = "ma3map.Path";
    private static final int MAX_COMMUTES = 10;//the maximum number of commutes to be generated
    private static final int MAX_NODES = 2;//the maximum number of routes (nodes) that should be in a commute
    private static final double MAX_WALKING_DISTANCE = 1000;//the maximum distance allowed for waliking when connecting nodes (routes)
    public static final int MAX_FROM_POINTS = 5;
    public static final int MAX_TO_POINTS = 10;
    private final ArrayList<Stop> from;
    private final LatLng actualFrom;
    private final ArrayList<Stop> to;
    private final LatLng actualTo;
    private final ArrayList<Route> routes;
    private final HashMap<String, Route> routeMap;
    private final ArrayList<Stop> stops;
    private final ArrayList<Commute> allCommutes;
    private int bestPathThreadIndex;
    private int noFromStops;
    private int noToStops;
    private Graph graph;
    private final HashMap<String, ArrayList<String>> stopRoutes;
    
    public Path(LatLng actualFrom, int noFromStops, LatLng actualTo, int noToStops, Graph graph, ArrayList<Route> routes, ArrayList<Stop> stops){
        this.graph = graph;
    	this.actualFrom = actualFrom;
        this.noFromStops = noFromStops;
        this.noToStops = noToStops;
    	this.actualTo = actualTo;
    	this.routes = routes;
        routeMap = new HashMap<String, Route>();
        for(int index = 0; index < routes.size(); index++) {
            routeMap.put(routes.get(index).getId(), routes.get(index));
        }
        this.stops = stops;
    	allCommutes = new ArrayList<Commute>();
    	bestPathThreadIndex = 0;

        //for each of the stops, get a list of all the routes that contain it
        /*stopRoutes = new HashMap<String, ArrayList<String>>();
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
        }*/
        stopRoutes = graph.getStopRoutes();
    	
    	//get closest stops to source and destination
        Log.d(TAG, "Getting all stops");
        ArrayList<Stop> fromStops = new ArrayList<Stop>();
        ArrayList<Stop> toStops = new ArrayList<Stop>();

        //get all stops
        for(int routeIndex = 0; routeIndex < routes.size(); routeIndex++){
            Log.d(TAG, "*");
            ArrayList<Stop> routeStops = routes.get(routeIndex).getStops(0);
            //Log.d(TAG, "Route has "+routeStops.size()+" stops");

            for(int rStopIndex = 0 ; rStopIndex < routeStops.size(); rStopIndex++){
                boolean isThere = false;
                //Log.d(TAG, "Comparing current stop in route with "+fromStops.size()+" other stops");
                for(int aStopIndex = 0; aStopIndex < fromStops.size(); aStopIndex++){
                    if(routeStops.get(rStopIndex).getLat().equals(fromStops.get(aStopIndex).getLat())
                            && routeStops.get(rStopIndex).getLon().equals(fromStops.get(aStopIndex).getLon())){
                        isThere = true;
                        break;
                    }
                }

                if(isThere == false){
                    fromStops.add(routeStops.get(rStopIndex));
                    toStops.add(routeStops.get(rStopIndex));
                }
            }
        }
        Log.d(TAG, "Done getting all stops");
        
        //get stops closest to source
        Collections.sort(fromStops, new Stop.DistanceComparator(actualFrom));//stop closest to from becomes first
        from = new ArrayList<Stop>();
        from.addAll(fromStops.subList(0, this.noFromStops));
        
        Collections.sort(toStops, new Stop.DistanceComparator(actualTo));//stop closest to destination becomes first
        to = new ArrayList<Stop>();
        to.addAll(toStops.subList(0, this.noToStops));
        Log.d(TAG, "Done initializing Path");
    }
    
    public void calculatePaths(){
    	ArrayList<BestPathThread> threads = new ArrayList<BestPathThread>();
        
        //TODO: Provide mechanism for queueing and specifying maximum threads    	
    	//create threads equal to the number of start stops
    	for(int tIndex = 0; tIndex < from.size(); tIndex++){
    		BestPathThread currThread = new BestPathThread(from.get(tIndex), from.size());
            threads.add(currThread);	
    		new Thread(currThread).start();
    	}
    }
    
    /**
     * Gets the next possible <code>routes</code> to be added to the provided <code>Commute</code>.
     * <p>
     * This method is recursive and will terminate when:
     *  -   All possible <code>routes</code> have been considered
     *  -   Number of <code>routes</code> in provided <code>commute</code> have surpassed {@link #MAX_NODES}
     * <p>
     * @param commute       Commute object carrying the current route path being constructed
     *
     * @return  A <code>Commute</code> object containing a complete path from a potential from <code>Stop</code>
     *          to a potential to <code>Stop</code> or <code>null</code> if no path is found
     *
     * @see org.ma3map.api.carriers.Commute
     * @see org.ma3map.api.carriers.Route
     * @see org.ma3map.api.carriers.Stop
     */
    private Commute getBestCommute(Stop firstStop){
        Commute commute = new Commute(actualFrom, actualTo);
        org.ma3map.api.carriers.Path bestPath = null;
        for(int toIndex = 0; toIndex < to.size(); toIndex++){//still assumes that to stops are ordered in terms of closeness to actual destination
            Stop currTo = to.get(toIndex);
            ArrayList<org.ma3map.api.carriers.Path> currPaths = graph.getPaths(graph.getNode(firstStop.getId()), graph.getNode(currTo.getId()));
            org.ma3map.api.carriers.Path currBestPath = null;
            for (int pathIndex = 0; pathIndex < currPaths.size(); pathIndex++){
                if(currBestPath == null || currBestPath.getStops().size() > currPaths.get(pathIndex).getStops().size()) {
                    currBestPath = currPaths.get(pathIndex);
                }
            }
            if(currBestPath != null) {
                if(bestPath == null || bestPath.getStops().size() > currBestPath.getStops().size()) {
                    bestPath = currBestPath;
                }
            }
        }
        if(bestPath != null) {
            ArrayList<Stop> stops = bestPath.getStops();
            commute.setNoStops(stops.size());
            if(stops.get(0).getId().equals(firstStop.getId())) {
                for(int stopIndex = 1; stopIndex < stops.size(); stopIndex++) {
                    Stop currStop = stops.get(stopIndex);
                    Stop previousStop = stops.get(stopIndex - 1);
                    ArrayList<Route> commonRoutes = getCommonRoutes(previousStop, currStop);
                    if(commonRoutes.size() > 0) {
                        if(commonRoutes.get(0) == null) Log.w(TAG, "First common route is null. Bound to crash");
                        if(commute.getLastStep() != null && commute.getLastStep().getStepType() == Commute.Step.TYPE_MATATU) {
                            commute.setStep(commute.getLastStepIndex(), new Commute.Step(Commute.Step.TYPE_MATATU,
                                    commonRoutes.get(0),
                                    commute.getLastStep().getStart(),
                                    currStop));
                        }
                        else {
                            commute.addStep(new Commute.Step(Commute.Step.TYPE_MATATU, commonRoutes.get(0), previousStop, currStop));
                        }
                    }
                    else {
                        if(commute.getLastStep() != null && commute.getLastStep().getStepType() == Commute.Step.TYPE_WALKING) {
                            if(new LatLngPair(commute.getLastStep().getStart().getLatLng(), currStop.getLatLng()).getDistance() <= Commute.MAX_WALKING_DISTANCE){
                                commute.setStep(commute.getLastStepIndex(), new Commute.Step(Commute.Step.TYPE_WALKING,
                                        commute.getLastStep().getRoute(),
                                        commute.getLastStep().getStart(),
                                        currStop));
                            }
                            else {
                                return null;
                            }
                        }
                        else {
                            commute.addStep(new Commute.Step(Commute.Step.TYPE_WALKING, null, previousStop, currStop));
                        }
                    }
                }
            }
            else {
                Log.e(TAG, "The first stop in the path is not the expected one");
                return null;
            }
        }
        else {
            return null;
        }
        return commute;
    }
    
    private class BestPathThread implements Runnable {
    	private final Stop from;
    	private final int threadCount;
    	
    	public BestPathThread(Stop from, int threadCount) {
    		super();
    		this.from = from;
    		this.threadCount = threadCount;
    	}

		@Override
		public void run() {
			final ArrayList<Commute> commutes = new ArrayList<Commute>();
            Log.d(TAG, "########################################");
            Commute resultantBestCommute = getBestCommute(from);
            if(resultantBestCommute != null) {
                commutes.add(resultantBestCommute);
            }
            Log.d(TAG, "########################################");
            
            allCommutes.addAll(commutes);
            
            bestPathThreadIndex++;
            Log.i(TAG, String.valueOf(bestPathThreadIndex)+" of "+String.valueOf(threadCount)+" best path threads have completed");
            if(bestPathThreadIndex == threadCount){
                Collections.sort(allCommutes, new Commute.ScoreComparator());
            	finalizeProgressListeners(allCommutes, "Done calculating commute paths", ProgressListener.FLAG_DONE);
            }
            else {
            	updateProgressListeners(bestPathThreadIndex, threadCount, "Calculating best commute paths", ProgressListener.FLAG_WORKING);
            }
		}
    }

    /**
     * Gets all <code>routes</code> in {@link #allCommutes} that contain the provided <code>stop</code>.
     * <p>
     * @param stop  The <code>stop</code> to be checked against all available <code>routes</code>
     *              to this class
     *
     * @return  An ArrayList with <code>routes</code> that contain the provided <code>stop</code>
     * @see org.ma3map.api.carriers.Route
     * @see org.ma3map.api.carriers.Stop
     */
    private ArrayList<Route> getRoutesWithStop(Stop stop){
        ArrayList<Route> stopRoutes = new ArrayList<Route>();

        for(int index = 0; index < routes.size(); index++){
            if(routes.get(index).isStopInRoute(stop)){
                stopRoutes.add(routes.get(index));
            }
        }

        return stopRoutes;
    }

    private ArrayList<Route> getCommonRoutes(Stop stopA, Stop stopB) {
        ArrayList<Route> commonRoutes = new ArrayList<Route>();
        ArrayList<String> commonRouteIds = stopRoutes.get(stopA.getId());
        commonRouteIds.retainAll(stopRoutes.get(stopB.getId()));
        for(int index = 0; index < commonRouteIds.size(); index++) {
            Log.d(TAG, "Route has id = "+commonRouteIds.get(index));
            if(routeMap.get(commonRouteIds.get(index)) != null) {
                commonRoutes.add(routeMap.get(commonRouteIds.get(index)));
            }
            else{
                Log.e(TAG, "One of the common routes is null. Not adding it");
            }
        }
        return commonRoutes;
    }
}
