/* ======================================
 * File: NamespaceId.java
 * Date: 2026-08-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;

public record NamespaceId(String id, String pluginId, String name) {
    public static NamespaceId parse(String id) {
        String[] arr = id.split(":");
        if (arr.length != 2 || arr[0].isEmpty() || arr[1].isEmpty()) {
            throw new NobException("Couldn't split [" + id + "] cleanly.");
        }

        return new NamespaceId(id, arr[0], arr[1]);
    }

    public String get() {
        return this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NamespaceId other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

