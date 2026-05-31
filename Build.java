/* ======================================
 * File: Build.java
 * Date: 2026-05-14
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

import nob.api.CompileConfig;
import static nob.Nob.*;
import java.nio.file.Path;

public class Build {
    public static void main(String[] args) {
        // goRebuildUrself("Build");

        compile(cfg -> {
            cfg.packageName = "nob";
            cfg.libs = "build/libs";
        });

        buildJar(cfg -> {
            cfg.name = "Nob.jar";
            cfg.mainClass = "nob.Nob";
        });
    }
}

