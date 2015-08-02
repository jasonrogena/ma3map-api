package org.ma3map.api.handlers;

import org.ma3map.api.carriers.*;
import org.ma3map.api.carriers.Path;
import org.ma3map.api.handlers.Log;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.*;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.neo4j.tooling.GlobalGraphOperations;

public class Graph {
    private final String GRAPH_PATH = "cache/ma3map.neo4j";
    private final String PROPERTY_DISTANCE = "distance";
    private final String UNIQUE_INDEX_STOPS = "stops";
    private final String NODE_LABEL_STOP = "stop";
    private final String NODE_PROPERTY_ID = "id";
    private static final String TAG = "ma3map.Graph";
    private final GraphDatabaseService graphDatabaseService;//very expensive to create, share across threads as much as possible
    private UniqueFactory.UniqueNodeFactory uniqueNodeFactory;
    private Schema schema;
    private HashMap<String, Node> nodes;
    private HashMap<String, Stop> stopMap;
    private ArrayList<Route> routes;

    private enum RelTypes implements RelationshipType {
        ARE_SISTERS,
        ARE_NEIGHBOURS
    }

    public Graph(ArrayList<Route> routes, ArrayList<Stop> stops) {
        GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();
        graphDatabaseService =  graphDbFactory.newEmbeddedDatabase(GRAPH_PATH);
        setIndexProperty();
        initUniqueFactory();
        nodes = new HashMap<String, Node>();
        this.routes = routes;
        stopMap = new HashMap<String, Stop>();
        for(int index = 0; index < stops.size(); index++) {
            stopMap.put(stops.get(index).getId(), stops.get(index));
        }
    }
    
    public boolean deleteGraph(){
    	try {
    		File file = new File(GRAPH_PATH);
    		if(file.exists()) {
    			FileUtils.deleteDirectory(file);
    		}
    		return true;
        }
        catch(IOException e) {
            Log.e(TAG, "Could not delete the graph");
            e.printStackTrace();
        }
        return false;
    }

    public void printGraphStats() {
        Transaction tx = graphDatabaseService.beginTx();
        try {
            Log.d(TAG, "Number of nodes in the graph = "+String.valueOf(IteratorUtil.count(GlobalGraphOperations.at(graphDatabaseService).getAllNodes())));
            Iterable<Node> nodes = GlobalGraphOperations.at(graphDatabaseService).getAllNodes();
            //Iterator<Node> iterator = nodes.();
            int count = 0;
            for(Node currNode: nodes) {
                count++;
                if(count%1000 == 0){
                    Log.d(TAG, "Node at "+String.valueOf(count)+" has id = "+currNode.getProperty(NODE_PROPERTY_ID));
                }


            }
            Log.d(TAG, "Number of relationships = "+String.valueOf(IteratorUtil.count(GlobalGraphOperations.at(graphDatabaseService).getAllRelationships())));
            tx.success();
        }
        finally {
            tx.finish();
        }

    }

    private void initUniqueFactory() {
        Transaction tx = graphDatabaseService.beginTx();
        try {
            if(uniqueNodeFactory == null) {
                uniqueNodeFactory = new UniqueFactory.UniqueNodeFactory(graphDatabaseService, UNIQUE_INDEX_STOPS) {
                    @Override
                    protected void initialize(Node created, Map<String, Object> properties) {
                        created.addLabel(DynamicLabel.label(NODE_LABEL_STOP));
                        created.setProperty(NODE_PROPERTY_ID, properties.get(NODE_PROPERTY_ID));
                    }
                };
            }
            tx.success();
        }
        finally {
            tx.finish();
        }
    }

    private void setIndexProperty() {
        Transaction tx = graphDatabaseService.beginTx();
        try {
            if(schema == null) {
                schema = graphDatabaseService.schema();
                schema.indexFor(DynamicLabel.label(NODE_LABEL_STOP))
                        .on(NODE_PROPERTY_ID)
                        .create();
            }
            tx.success();
        }
        finally {
            tx.finish();
        }
    }

    public Node createNode(String id) {
        Node result = null;
        Transaction tx = graphDatabaseService.beginTx();
        try {
            if(nodes.containsKey(id)){
                result = nodes.get(id);
            }
            else {
                result = uniqueNodeFactory.getOrCreate(NODE_PROPERTY_ID, id);
                nodes.put(id, result);
            }

            tx.success();
        }
        finally {
            tx.finish();
        }
        return result;
    }
    
    public ArrayList<org.ma3map.api.carriers.Path> getPaths(Node node1, Node node2) {
        ArrayList<org.ma3map.api.carriers.Path> stopPaths = new ArrayList<org.ma3map.api.carriers.Path>();
        if(node1 != null && node2 != null) {
            Transaction tx = graphDatabaseService.beginTx();
            try {
                Log.d(TAG, "Getting paths between "+((String)node1.getProperty(NODE_PROPERTY_ID))+" and "+((String)node2.getProperty(NODE_PROPERTY_ID)));
                /*use Dijkstra's (instead of A*) Algorithm to get best path because we currently don't have a way of accurately estimating the
                the cost of the unsolved path given the path we already have*/
                Ma3mapCostEvaluator costEvaluator = new Ma3mapCostEvaluator();
                PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(PathExpanders.allTypesAndDirections(), costEvaluator);
                Iterable<WeightedPath> paths = finder.findAllPaths(node1, node2);
                if(paths != null){
                    Iterator<WeightedPath> pathIterator = paths.iterator();
                    while(pathIterator.hasNext()){
                        WeightedPath weightedPath = pathIterator.next();
                        double weight = weightedPath.weight();
                        ArrayList<Stop> stops = new ArrayList<Stop>();
                        Iterable<Node> nodes = weightedPath.nodes();
                        Iterator<Node> nodeIterator = nodes.iterator();
                        while(nodeIterator.hasNext()) {
                            Node currNode = nodeIterator.next();
                            stops.add(stopMap.get(currNode.getProperty(NODE_PROPERTY_ID)));
                        }
                        org.ma3map.api.carriers.Path currPath = new Path(stops, weight);
                        stopPaths.add(currPath);
                    }
                    Log.d(TAG, "Gotten "+String.valueOf(stopPaths.size())+" paths");
                    Collections.sort(stopPaths, new Path.WeightComparator());
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "An error occurred while trying to find paths");
            }
            finally {
                tx.finish();
            }
        }
        else {
            Log.e(TAG, "One of the provided nodes is null. Cannot get path");
        }
    	return stopPaths;
    }
    
    public Node getNode(String id) {

    	if(nodes.containsKey(id)){
            return nodes.get(id);
        }
        else {
            Log.w(TAG, "Could not find node with id = "+id);
        }
    	return null;
    }

    public boolean createRelationship(Node node1, Node node2, double distance, boolean areSisters) {
        boolean result = false;
        Transaction tx = graphDatabaseService.beginTx();
        try {
            if(areSisters) {
                distance = distance/10;
            }
            Relationship relationship = null;
            if(areSisters) {
                relationship = node1.createRelationshipTo(node2, RelTypes.ARE_SISTERS);
            }
            else {
                relationship = node1.createRelationshipTo(node2, RelTypes.ARE_NEIGHBOURS);
            }
            relationship.setProperty(PROPERTY_DISTANCE, new Double(distance));
            tx.success();
            result = true;
        }
        finally {
            tx.finish();
        }
        return result;
    }

    public void close() {
        graphDatabaseService.shutdown();
        Log.i(TAG, "Closing connection to graph");
    }

    private class Ma3mapCostEvaluator implements CostEvaluator<Double> {

        public Double getCost(Relationship relationship, Direction direction) {
            //TODO: use a combination of the distance and the speed to determine the cost
            //TODO: use exponental decay function to adjust the effect of the speed on the cost based on when the speed was recorded
            //be sure not to use the == comparitor with Double objects ;)
            Double distance = (Double)relationship.getProperty(PROPERTY_DISTANCE);
            return distance;
        }
    }
}