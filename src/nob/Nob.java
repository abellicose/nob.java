/* ====================================== 
 * File: Nob.java
 * Date: 2026-05-14
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import java.util.function.Consumer;
import java.nio.file.Path;
import java.nio.file.Files;

import nob.core.BuildJar;
import nob.core.Compile;
import nob.api.JarConfig;
import nob.api.CompileConfig;
import nob.api.ProjectConfig;
import nob.cache.BuildContext;
import static nob.util.Util.*;

public class Nob {
    private static final String nob = "build/.nob";

    public String packageName   = null;
    public String mainClass     = null;
    public String src           = "src/";
    public String dest          = "build/";
    public String libs          = "build/libs";
    public String classes       = "classes";
    public String jarName       = "out.jar";

    private BuildContext ctx = null;

    private void verifyEverything() {
        // TODO: Probably shove goRebuildUrself in here. Make it a feature.
        if (ctx != null) return; // temp, do dep resolution and allat here asw

        ProjectConfig cfg = new ProjectConfig();
        cfg.packageName = packageName;
        cfg.mainClass = mainClass;
        cfg.src = src;
        cfg.dest = dest;
        cfg.libs = libs;
        cfg.classes = classes;
        cfg.jarName = jarName;

        try {
            ctx = BuildContext.load(cfg);
        } catch (Exception e) {
            System.out.println("[nob] Something went wrong while building context");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void compile(Consumer<CompileConfig> consumer) {
        verifyEverything();

        Compile.compile(this.ctx, consumer);
    }

    public void compile(CompileConfig cfg) {
        verifyEverything();

        Compile.compile(this.ctx, cfg);
    }

    public void buildJar(Consumer<JarConfig> consumer) {
        verifyEverything();

        BuildJar.buildJar(this.ctx, consumer);
    }

    public void buildJar(JarConfig cfg) {
        verifyEverything();

        BuildJar.buildJar(this.ctx, cfg);
    }

    // Builds the build file into a self-contained jar that can be run with java -jar.
    // Nob.jar must be in the same directory as the output jar at runtime.
    // TODO: Rework
/*
    public static void goRebuildUrself(String buildFileName) {
        final String name = buildFileName.replace(".java", "");
        Path buildFile = Path.of(name + ".java");
        if (!Files.exists(buildFile)) {
            System.out.println("Build file " + buildFileName + " does not exist at the specified path!!");
            System.exit(1);
        }

        Path jarFile = Path.of(name + ".jar");

        // Check if Needs jar Needs Building
        long mTimeSrc = Long.MAX_VALUE;
        long mTimeJar = 0;
        boolean jarExists = Files.exists(jarFile);

        try {
            mTimeSrc = Files.getLastModifiedTime(buildFile).toMillis();
        } catch (Exception e) {
            System.out.println("Failed to get modification time of build file.");
            e.printStackTrace();
            System.exit(1);
        }

        if (jarExists) {
            try {
                mTimeJar = Files.getLastModifiedTime(jarFile).toMillis();
            } catch (Exception e) {
                System.out.println("Failed to get modification time of .");
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        if (mTimeJar > mTimeSrc) {
            System.out.println("Jar is newest!");
            return;
        }

        // Delete Jar
        try {
            Files.deleteIfExists(jarFile);
        } catch (Exception e) {
            System.out.println("Could not delete old jar file!");
            e.printStackTrace();
        }

        // Recompile and Rebuild
        compile(cfg -> {
            cfg.src = buildFile.toString();
            cfg.dest = nob; // build/.nob
            cfg.addToClasspath("Nob.jar");
        });
    
        buildJar(cfg -> {
            cfg.name = name + ".jar";
            cfg.classes = nob;
            cfg.out = "./";
            cfg.mainClass = name;
            cfg.addToClasspath("Nob.jar");
        });

        // bootstrap
    }
*/
}


