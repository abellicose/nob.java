/* ======================================
 * File: TaskId.java
 * Date: 2026-08-14
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

public class TaskId extends NamespaceId {
    public TaskId(PluginId pluginId, String name) {
        super(pluginId, name); 
    }
    public TaskId(String rawId) {
        super(rawId); 
    }
}

