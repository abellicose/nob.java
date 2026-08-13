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
    public final String pluginId;
    public final String name;

    private NamespaceId(String id, String pluginId, String name) {
        this.id = id;
        this.pluginId = pluginId;
        this.name = name;
    }

    public NamespaceId(String id) {
        String[] arr = id.split(":");
        if (arr.length != 2 || arr[0].isEmpty() || arr[1].isEmpty()) {
            throw new NobException("Couldn't split [" + id + "] cleanly.");
        }

        this(id, arr[0], arr[1]);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamespaceId other = (NamespaceId) o;
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

