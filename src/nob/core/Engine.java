/* ======================================
 * File: Engine.java
 * Date: 2026-11-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.core;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import com.sun.jdi.request.DuplicateRequestException;

import nob.api.plugin.Plugin;
import nob.api.task.Task;
import nob.api.plugin.Registry;

// Phase 1: Discovery
public class Engine {

    private final Map<PluginId, PluginMeta> plugins = new HashMap<>();

    void scanPlugin(Plugin plugin) {
        PluginId pluginId = new PluginId(plugin.id());
        if (plugins.containsKey(pluginId)) {
            throw new DuplicateRequestException("Plugin " + plugin.id() + " already registered.");
        }

        DefaultRegistry r = new DefaultRegistry(plugin.id());
        plugin.apply(r);
        r.close();
        PluginMeta meta = new PluginMeta(r.getTasks(), r.getConfig(), r.getBuckets());
        plugins.put(pluginId, meta);
    }
}

record PluginId(String pluginId) {}
record PluginMeta(Set<Class<? extends Task>> tasks, Set<ConfigMeta> config, Set<BucketMeta> buckets) {}
