/* ======================================
 * File: ConfigId.java
 * Date: 2026-08-14
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

public class ConfigId extends NamespaceId {
    public ConfigId(PluginId pluginId, String name) {
        super(pluginId, name); 
    }
    public ConfigId(String rawId) {
        super(rawId); 
    }
}

