/* ======================================
 * File: CompilePlugin.java
 * Date: 2026-07-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.util.Set;

class CompilePlugin implements Plugin {
    public String id() {
        return "comp";
    }

    public Set<String> dependsOn() {
        return Set.of("base");
    }

    public void apply(Registrar reg) {
        Task tsk = new CompileTask();
        String sig = reg.registerTask(tsk);

        reg.addTo(sig, "compile");

        reg.setCache("incstate", new CompileCache());
    }
}

class CompileCache {
    FileTree cachedTree = null;
    FileTree newTree    = null;
    Map<String, String> binaryToSource = new HashMap<>();
    Map<String, Set<String>> sourceToBinaries = new HashMap<>();
    Map<String, Set<String>> sourceToDeclaredMethods = new HashMap<>();            // source (class and subclasses) and methods it owns
    Map<String, Set<String>> sourceToCalledMethods = new HashMap<>();           // source (class and subclasses) and methods it calls
    Map<String, Set<String>> methodToCallerBinaries = new HashMap<>();        // methods and specific classes that depend on those methods
}
