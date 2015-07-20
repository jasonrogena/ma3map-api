package org.ma3map.api.handlers;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.index.UniqueFactory;

import java.util.Map;

import org.ma3map.api.handlers.Log;

public class Graph {
    private final String GRAPH_PATH = "cache/ma3map.neo4j";
    private final String PROPERTY_DISTANCE = "distance";
    private final String PROPERTY_SISTERS = "sisters";
    private final String UNIQUE_INDEX_STOPS = "stops";
    private final String NODE_LABEL_STOP = "stop";
    private final String NODE_PROPERTY_ID = "id";
    private static final String TAG = "ma3map.Graph";
    private final GraphDatabaseService graphDatabaseService;//very expensive to create, share across threads as much as possible
    private final UniqueFactory.UniqueNodeFactory uniqueNodeFactory;

    public Graph() {
        GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();
        graphDatabaseService =  graphDbFactory.newEmbeddedDatabase(GRAPH_PATH);
        //registerShutdownHook(graphDatabaseService);
        Transaction tx = graphDatabaseService.beginTx();
        try {
            uniqueNodeFactory = new UniqueFactory.UniqueNodeFactory(graphDatabaseService, UNIQUE_INDEX_STOPS) {
                @Override
                protected void initialize(Node created, Map<String, Object> properties) {
                    created.addLabel(DynamicLabel.label(NODE_LABEL_STOP));
                    created.setProperty(NODE_PROPERTY_ID, properties.get(NODE_PROPERTY_ID));
                }
            };
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
            relationship.setProperty(PROPERTY_DISTANCE, distance);
            relationship.setProperty(PROPERTY_SISTERS, areSisters);
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
}