/* ======================================
 * File: CompileConfig.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import nob.util.NobException;

public class CompileConfig {
    public List<String> flags = new ArrayList<>();
    public List<String> modules = new ArrayList<>();
    public List<String> classpath = new ArrayList<>();

    public void addModules(String... mods) {
        modules.addAll(List.of(mods));
    }

    public void addToClasspath(String... cp) {
        classpath.addAll(List.of(cp));
    }

    public void addCompilerFlag(String... f) {
        flags.addAll(List.of(f));
    }
}
