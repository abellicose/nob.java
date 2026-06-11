/* ======================================
 * File: PomData.java
 * Date: 2026-06-10
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.deps;
import java.util.Map;
import java.util.List;

public class PomData {
    public Dependency parent;
    public Map<String, String> properties;
    public List<Dependency> deps;
    public Map<Dependency, String> managements;

    public PomData(Dependency parent, Map<String, String> properties, List<Dependency> deps, Map<Dependency, String> managements) {
        this.parent = parent;
        this.properties = properties;
        this.deps = deps;
        this.managements = managements;
    }

    public String toString() {
        return "PomData{" +
            "parent=" + parent +
            ", properties=" + properties +
            ", deps=" + deps +
            ", managements=" + managements +
            '}';
    }
}
