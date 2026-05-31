/* ======================================
 * File: Nob.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import nob.build.Context;

public class Nob {
    public String sourceDir     = "src/";
    public String buildDir      = "build/";
    public String classesDir    = "classes/";   // Relative to buildDir
    public String libsDir       = "build/libs"; // Relative to project root
    public String mainClass     = null;
    public String packageName   = null;
    public String jarName       = "out.jar";

    public Context ctx          = null;

    public void init() {
        if (ctx != null) return;

        ctx = Context.load(this);
    }

    public void compile() {
        init();
        // Add compileTask to graph
    }

    public void jar() {
        init();
        // Add packageTask to graph
    }
}

