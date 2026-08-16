/* ======================================
 * File: BucketId.java
 * Date: 2026-08-12
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

public class BucketId extends NamespaceId {
    public BucketId(PluginId pluginId, String name) {
        super(pluginId, name); 
    }
    public BucketId(String rawId) {
        super(rawId); 
    }
    
    public BucketId(String id, PluginId pluginId, String name) {
        super(id, pluginId, name);
    }
}

