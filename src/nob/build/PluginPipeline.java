/* ======================================
 * File: PluginPipeline.java
 * Date: 2026-07-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import nob.Plugin;
import nob.NobException;

public class PluginPipeline {
    Map<String, Plugin> registry = new HashMap<>();
    Map<String, List<String>> downstream = new HashMap<>();
    final TaskPipeline tp;

    public PluginPipeline(TaskPipeline tp) {
        this.tp = tp;
    }

    public void register(Plugin plugin) {
        String pluginId = plugin.id();
        if (registry.containsKey(pluginId)) {
            throw new NobException("Plugin with id " + plugin.id() + " already registered. Cannot reregister it");
            // TODO: Show more details about them, why and where they were loaded from etc
        }
        registry.put(pluginId, plugin);
        for (String ds: plugin.dependsOn()) {
            downstream.computeIfAbsent(ds, k -> new ArrayList<>()).add(pluginId);
        }

    }

    // Runs and instantiates the entire loaded plugin graph
    public void run() {
        for (String dep : downstream.keySet()) {
            if (!registry.containsKey(dep)) {
                throw new NobException("Plugin " + dep + " is depended on but was never registered");
            }
        }
        Deque<String> stack = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<String> out = new ArrayList<>();

        // this should detect cycles. reporting cycles later
        for (String outlet: registry.keySet()) {
            if (visited.contains(outlet)) continue;
            stack.push(outlet);
            topoSort(stack, visiting, visited, out);
        }

        for (String pluginId: out) {
            Plugin plugin = registry.get(pluginId);
            if (plugin == null) {
                throw new NobException("No plugin for id " + pluginId + " found");
            }
            execute(plugin);
        }
    }

    private void execute(Plugin plugin) {
        Logger.debug("Executing plugin " + plugin.id());
/*
        Registrar reg = new Registrar(plugin.id(), this.tp);
        plugin.apply(reg);
*/
    }

    private void topoSort(Deque<String> stack, Set<String> visiting, Set<String> visited,  List<String> out) {
        while (!stack.isEmpty()) {
            String outlet = stack.peek();
            int inletCount = 0;

            visiting.add(outlet);
            for (String inlet: downstream.getOrDefault(outlet, List.of())) {
                if (visited.contains(inlet)) continue;

                if (visiting.contains(inlet)) {
                    throw new NobException("Circular dependency detected between plugins.");
                }
                stack.push(inlet);
                inletCount++;
            }

            if (inletCount == 0) {
                stack.pop();
                visiting.remove(outlet);
                visited.add(outlet);
                out.add(0, outlet);
            }
        }
    }
}

