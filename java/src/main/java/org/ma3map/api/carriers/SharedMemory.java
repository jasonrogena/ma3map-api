package org.ma3map.api.carriers;

import org.ma3map.api.handlers.Graph;

/**
 * Created by jrogena on 07/08/2015.
 */
public class SharedMemory {
    private final Graph graph;

    public SharedMemory(Graph graph) {
        this.graph = graph;
    }
}
