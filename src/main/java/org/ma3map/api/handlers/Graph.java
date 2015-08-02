package org.ma3map.api.handlers;

import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.Schema;
//import org.neo4j.graphdb.schema.IndexCreator;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.io.File;
import java.io.IOException;

import org.ma3map.api.handlers.Log;

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

    public Graph() {
        GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();
        graphDatabaseService =  graphDbFactory.newEmbeddedDatabase(GRAPH_PATH);
        setIndexProperty();
        initUniqueFactory();
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
            result = uniqueNodeFactory.getOrCreate(NODE_PROPERTY_ID, id);
            tx.success();
        }
        finally {
            tx.finish();
        }
        return result;
    }
    
    public Iterable<WeightedPath> getPaths(Node node1, Node node2) {
        Iterable<WeightedPath> paths = null;
        if(node1 != null && node2 != null) {
            Transaction tx = graphDatabaseService.beginTx();
            try {
                Log.d(TAG, "Getting paths between "+((String)node1.getProperty(NODE_PROPERTY_ID))+" and "+((String)node2.getProperty(NODE_PROPERTY_ID)));
        	/*use Dijkstra's instead of A* because we still don't know how to estimate the weight of the remaining path given the already constructed path
        	 * Brute force the damn thing!!
        	 */
                Ma3mapCostEvaluator costEvaluator = new Ma3mapCostEvaluator();
                PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra(PathExpanders.allTypesAndDirections(), costEvaluator);
                WeightedPath path = finder.findSinglePath(node1, node2);
                Log.i(TAG, "Current path has "+String.valueOf(path.length())+" nodes");
                //return finder.findAllPaths(node1, node2);
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
    	return paths;
    }
    
    public Node getNode(String id) {

    	Transaction tx = graphDatabaseService.beginTx();
        try {
            /*IndexManager indexManager = graphDatabaseService.index();
            Index<Node> allNodes = indexManager.forNodes(NODE_LABEL_STOP);
            node = allNodes.get(NODE_PROPERTY_ID, id).getSingle();
            tx.success();*/
            Result result = graphDatabaseService.execute("match (n {"+NODE_PROPERTY_ID+": '"+id+"'}) return n");
            Iterator<Node> nodes = result.columnAs("n");
            ArrayList<Node> nodeList = new ArrayList<Node>();
            for(Node node : IteratorUtil.asIterable(nodes)){
                nodeList.add(node);
            }
            if(nodeList.size() == 1) {
                return nodeList.get(0);
            }
            else if(nodeList.size() > 1){
                Log.e(TAG, "More than one node returned with the id "+id);
            }
            else {
                Log.e(TAG, "No node found with the id "+id);
            }
            tx.success();
        }
        finally {
            tx.finish();
        }
    	return null;
    }

    public boolean createRelationship(Node node1, Node node2, double distance, boolean areSisters) {
        boolean result = false;
        Transaction tx = graphDatabaseService.beginTx();
        try {
            if(areSisters) {
                Log.d(TAG, "Nodes are sisters");
                distance = 0;
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

    private static enum RelTypes implements RelationshipType {
        ARE_SISTERS,
        ARE_NEIGHBOURS
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