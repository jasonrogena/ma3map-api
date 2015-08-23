package org.ma3map.api.handlers;

import org.ma3map.api.carriers.*;
import org.ma3map.api.carriers.Path;
import org.ma3map.api.handlers.Log;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.helpers.collection.IteratorUtil;

import java.io.Serializable;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.tooling.GlobalGraphOperations;

public class Graph implements Serializable {

    public static final String KEY = "Graph";
    private final String GRAPH_PATH = "/tmp/graph.neo4j";
    private final String PROPERTY_DISTANCE = "distance";
    private final String UNIQUE_INDEX_STOPS = "stops";
    private final String NODE_LABEL_STOP = "stop";
    private final String NODE_PROPERTY_ID = "id";
    private static final String TAG = "ma3map.Graph";
    private GraphDatabaseService graphDatabaseService;//very expensive to create, share across threads as much as possible
    private UniqueFactory.UniqueNodeFactory uniqueNodeFactory;
    private Schema schema;
    private HashMap<String, Node> nodes;
    private HashMap<String, Stop> stopMap;
    private ArrayList<Route> routes;

    private enum RelTypes implements RelationshipType {
        ARE_SISTERS,
        ARE_NEIGHBOURS
    }

    public Graph(ArrayList<Route> routes, ArrayList<Stop> stops, boolean readOnly) {
        if(readOnly == false) deleteGraph();
        GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();
        GraphDatabaseBuilder databaseBuilder = graphDbFactory.newEmbeddedDatabaseBuilder(GRAPH_PATH)
                .setConfig(GraphDatabaseSettings.allow_store_upgrade, "true")
                .setConfig( GraphDatabaseSettings.pagecache_memory, "256M")
                .setConfig(GraphDatabaseSettings.string_block_size, "60")
                .setConfig(GraphDatabaseSettings.array_block_size, "300");
        if(readOnly == true){
            databaseBuilder.setConfig(GraphDatabaseSettings.read_only, "true");
        }
        else {
            databaseBuilder.setConfig(GraphDatabaseSettings.read_only, "false");
            databaseBuilder.setConfig(GraphDatabaseSettings.batched_writes, "true");
        }
        graphDatabaseService = databaseBuilder.newGraphDatabase();

        nodes = new HashMap<String, Node>();

        if(readOnly == false) {
            setIndexProperty();
            initUniqueFactory();
        }
        else {
            printGraphStats();
            loadNodes();
        }
        this.routes = routes;
        stopMap = new HashMap<String, Stop>();
        for(int index = 0; index < stops.size(); index++) {
            stopMap.put(stops.get(index).getId(), stops.get(index));
        }
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDatabaseService.shutdown();
            }
        } );
    }

    private void loadNodes() {
        nodes = new HashMap<String, Node>();
        Transaction tx = graphDatabaseService.beginTx();
        try {
            for(Node currNode : GlobalGraphOperations.at(graphDatabaseService).getAllNodes()) {
                nodes.put((String)currNode.getProperty(NODE_PROPERTY_ID), currNode);
            }
            Log.d(TAG, "Loaded " + String.valueOf(nodes.size()) + " nodes");
            tx.success();
        }
        catch(Exception e){
            e.printStackTrace();
            tx.failure();
        }
        finally {
            tx.close();
            //tx.finish();
       }

    }
    
    public boolean deleteGraph(){
        File graph = new File(GRAPH_PATH);
        if(graph.exists()){
            for(File file: graph.listFiles()) {
                file.delete();
            }
        }
        return true;
    }

    public void printGraphStats() {
        Transaction tx = graphDatabaseService.beginTx();
        try {
            Log.d(TAG, "Number of nodes in the graph = "+String.valueOf(IteratorUtil.count(GlobalGraphOperations.at(graphDatabaseService).getAllNodes())));
            Log.d(TAG, "Number of relationships = " + String.valueOf(IteratorUtil.count(GlobalGraphOperations.at(graphDatabaseService).getAllRelationships())));
            tx.success();
        }
        catch(Exception e){
            e.printStackTrace();
            tx.failure();
        }
        finally {
            tx.close();
            //tx.finish();
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
        catch(Exception e){
            e.printStackTrace();
            tx.failure();
        }
        finally {
            tx.close();
            //tx.finish();
        }
    }

    private void setIndexProperty() {
        Transaction tx = graphDatabaseService.beginTx();
        try {
            if(schema == null) {
                schema = graphDatabaseService.schema();
                IndexDefinition indexDefinition = schema.indexFor(DynamicLabel.label(NODE_LABEL_STOP))
                        .on(NODE_PROPERTY_ID)
                        .create();
            }
            tx.success();
        }
        catch(Exception e){
            e.printStackTrace();
            tx.failure();
        }
        finally {
            tx.close();
            //tx.finish();
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
                /*result = graphDatabaseService.createNode();
                result.setProperty(NODE_PROPERTY_ID, id);*/
                nodes.put(id, result);
            }
            tx.success();
        }
        catch(Exception e){
            e.printStackTrace();
            tx.failure();
        }
        finally {
            tx.close();
            //tx.finish();
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
                tx.success();
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "An error occurred while trying to find paths");
                tx.failure();
            }
            finally {
                tx.close();
                //tx.finish();
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
            result = true;
            tx.success();
        }
        catch(Exception e){
            e.printStackTrace();
            tx.failure();
        }
        finally {
            tx.close();
            //tx.finish();
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