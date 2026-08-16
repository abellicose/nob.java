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
    Map<PluginId, Plugin> registry = new HashMap<>();
    final TaskPipeline tp;

    public PluginPipeline(TaskPipeline tp) {
        this.tp = tp;
    }

    public void register(Plugin plugin) {
        PluginId pluginId = new PluginId(plugin.id());

        if (registry.containsKey(pluginId)) return;
        registry.put(pluginId, plugin);

        // Registrar registrar = new Registrar(pluginId, tp);
        // plugin.apply(registrar);
        // Set<String> dependencies = registrar.getDependencies();
    }
}

