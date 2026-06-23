/* ======================================
 * File: Plugin.java
 * Date: 2026-06-23
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import nob.build.Graph;

public interface Plugin {
    void apply(Graph graph);
}

