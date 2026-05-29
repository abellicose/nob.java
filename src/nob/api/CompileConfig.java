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

public class CompileConfig {
    public Path src = Path.of("src/");
    public Path out = Path.of("build/classes/");
    public String packageName = "";

    public List<String> modules = new ArrayList<>();
    public List<String> classpath = new ArrayList<>();
    public List<String> flags = new ArrayList<>();
    public List<String> dirsToInclude = new ArrayList<>();

    public void addModules(String... mods) {
        modules.addAll(List.of(mods));
    }

    public void addToClasspath(String... cp) {
        classpath.addAll(List.of(cp));
    }

    public void addCompilerFlag(String... f) {
        flags.addAll(List.of(f));
    }

    /** Paths relative to srcDir */
    public void addDirsToInclude(String... dirs) {
        dirsToInclude.addAll(List.of(dirs));
    }
}
