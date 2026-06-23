/* ======================================
 * File: ResolveDeps.java
 * Date: 2026-06-23
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.Plugin;
import nob.build.Graph;

public class ResolveDeps implements Plugin {
    public void apply(Graph graph) {
        graph.register(new ResolveTask());
    }
}

