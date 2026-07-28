/* ======================================
 * File: Registrar.java
 * Date: 2026-07-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

public class Registrar {
    private final String id;
    private final Graph graph;

    public Registrar(String id, Graph graph) {
        this.id = id;
        this.graph = graph;
    }

    public void createLifecycle(String lifecycle) {
        this.graph.createLifecycle(lifecycle);
    }

    public void registerTask(Task task) {
        this.graph.registerTask(task);
    }

    public void addTo(String task, String prerequisite) {
        this.graph.addTo(task, prerequisite);
    }

    public void addAfter(String task, String prerequisite) {
        this.graph.addAfter(task, prerequisite);
    }
}

