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

import static nob.Util.*;

public class Nob {
    private static final String nob = "build/.nob/classes/";

    public static void compile(Consumer<CompileConfig> consumer) {
        Compile.compile(consumer);
    }

    public static void compile(CompileConfig cfg) {
        Compile.compile(cfg);
    }

    public static void buildJar(Consumer<JarConfig> consumer) {
        BuildJar.buildJar(consumer);
    }

    public static void buildJar(JarConfig cfg) {
        BuildJar.buildJar(cfg);
    }

    // Builds the build file into a self-contained jar that can be run with java -jar.
    // Nob.jar must be in the same directory as the output jar at runtime.
    public static void goRebuildUrself(String buildFileName) {
        final String name = buildFileName.replace(".java", "");
        Path buildFile = Path.of(name + ".java");
        if (!Files.exists(buildFile)) {
            System.out.println("Build file " + buildFileName + " does not exist at the specified path!");
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
            cfg.src = buildFile;
            cfg.out = Path.of(nob);
            cfg.addToClasspath("Nob.jar");
        });
    
        buildJar(cfg -> {
            cfg.name = name + ".jar";
            cfg.classes = Path.of(nob);
            cfg.out = Path.of("./");
            cfg.mainClass = name;
            cfg.addToClasspath("Nob.jar");
        });

        // bootstrap
    }
}


