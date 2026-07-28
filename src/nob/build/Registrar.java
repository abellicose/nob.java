/* ======================================
 * File: Registrar.java
 * Date: 2026-07-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.Task;

public class Registrar {
    private final String pluginId;
    private final Graph graph;

    public Registrar(String pluginId, Graph graph) {
        this.pluginId = pluginId;
        this.graph = graph;
    }

    public void createLifecycle(String lifecycle) {
        this.graph.createLifecycle(lifecycle);
    }

    public String registerTask(Task task) {
        String signature = createSignature(this.pluginId, task.id());
        this.graph.registerTask(signature, task);
        return signature;
    }

    public void addTo(String signature, String prerequisite) {
        this.graph.addTo(signature, prerequisite);
    }

    public void addAfter(String signature, String prerequisite) {
        this.graph.addAfter(signature, prerequisite);
    }

    public String createSignature(String pluginId, String taskId) {
        return pluginId + ":" + taskId;
    }
}

