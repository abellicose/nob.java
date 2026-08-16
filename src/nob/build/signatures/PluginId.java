/* ======================================
 * File: PluginId.java
 * Date: 2026-08-14
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

public class PluginId {
    public final String rawId;

    public PluginId(String rawId) {
        this.rawId = rawId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PluginId)) return false;
        PluginId other = (PluginId) obj;
        return this.rawId.equals(other.rawId);
    }

    @Override
    public int hashCode() {
        return this.rawId.hashCode();
    }

    @Override
    public String toString() {
        return "PluginId[rawId=" + this.rawId + "]";
    }
}
