/* ======================================
 * File: CompileConfig.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;

class CompileConfig {
    Path src = Path.of("src/");
    Path out = Path.of("build/classes/");

    List<String> modules = new ArrayList<>();
    List<String> classpath = new ArrayList<>();
    List<String> flags = new ArrayList<>();
    List<String> dirsToInclude = new ArrayList<>();

    CompileConfig addModules(String... mods) {
        modules.addAll(List.of(mods));
        return this;
    }

    CompileConfig addToClasspath(String... cp) {
        classpath.addAll(List.of(cp));
        return this;
    }

    CompileConfig addCompilerFlag(String... f) {
        flags.addAll(List.of(f));
        return this;
    }

    /** Paths relative to srcDir */
    CompileConfig addDirsToInclude(String... dirs) {
        dirsToInclude.addAll(List.of(dirs));
        return this;
    }
}
