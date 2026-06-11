/* ======================================
 * File: Dependency.java
 * Date: 2026-06-10
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.deps;

import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

public class Dependency {
    public String groupId;
    public String artifactId;
    public String version;
    public Set<String> exclusions;

    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.exclusions = new HashSet<>();
    }

    public Dependency(String groupId, String artifactId, String version, Set<String> exclusions) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.exclusions = exclusions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Dependency d)) return false;
        return groupId.equals(d.groupId) && artifactId.equals(d.artifactId);
    }

    @Override
    public String toString() {
        return "Dependency[groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + "]";
    }
}
