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
    public String packageName       = null;       // Required
    public String src               = "src/";
    public String dest              = "build/";
    public String libs              = "libs/";    // relative to project root
    public String classes           = "classes/"; // relative to dest

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

    public void validate() throws NobException {
        if (packageName == null) throw new NobException("Must set CompileConfig.packageName");
    }
}
