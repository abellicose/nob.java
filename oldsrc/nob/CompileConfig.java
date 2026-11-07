/* ======================================
 * File: CompileConfig.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import java.util.List;
import java.util.ArrayList;

public class CompileConfig {
    public List<String> compilerFlags = new ArrayList<>();
    public List<String> modules = new ArrayList<>();
    public List<String> classpath = new ArrayList<>();

    public void classpath(String... cp) {
        classpath.addAll(List.of(cp));
    }

    public void module(String... mods) { 
        modules.addAll(List.of(mods));
    }

    public void flag(String... flags) {
        compilerFlags.addAll(List.of(flags));
    }
}

