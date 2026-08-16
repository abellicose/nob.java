/* ======================================
 * File: NamespaceId.java
 * Date: 2026-08-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;

public class NamespaceId {
    public final String id;
    public final PluginId pluginId;
    public final String name;

    public NamespaceId(String id, PluginId pluginId, String name) {
        this.id = id;
        this.pluginId = pluginId;
        this.name = name;
    }

    public NamespaceId(PluginId pluginId, String name) {
        this(rawIdFrom(pluginId.rawId, name), pluginId, name);
    }

    public NamespaceId(String id) {
        this(id, parsePluginId(id), parseName(id));
    }

    private static PluginId parsePluginId(String id) {
        int index = id.indexOf(':');
        if (index == -1) throw new NobException("Could not parse signature: " + id + ". Required shape -> pluginId:name");
        return new PluginId(id.substring(0, index));
    }

    private static String parseName(String id) {
        int index = id.indexOf(':');
        if (index == -1) throw new NobException("Could not parse signature: " + id + ". Required shape -> pluginId:name");
        return id.substring(index + 1);
    }

    private static String rawIdFrom(String pluginId, String name) {
        return pluginId + ":" + name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NamespaceId other = (NamespaceId) obj;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "NamespaceId[id=" + this.id + "]";
    }
}

