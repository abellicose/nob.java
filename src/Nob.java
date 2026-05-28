/* ====================================== 
 * File: Nob.java
 * Date: 2026-05-14
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

public class Nob {
    private static final String nob = Path.of(".nob/classes/");

    public static void compile(Comsumer<CompileConfig> consumer) {
        Compile.compile(consumer);
    }

    public static void compile(CompileConfig cfg) {
        Compile.compile(cfg);
    }

    public static void buildJar(Comsumer<JarConfig> consumer) {
        BuildJar.compile(consumer);
    }

    public static void buildJar(CompileConfig cfg) {
        BuildJar.compile(cfg);
    }

    public static void goRebuildUrself(String buildFileName) {
        buildFileName = buildFileName.replace(".java", "");
        Path buildFile = Path.of(buildFileName + ".java");
        if (!Files.exists(buildFile)) {
            System.out.println("Build file " + buildClassName + " does not exist at the specified path!");
            System.exit(1);
        }

        Path jarFile = Path.of(buildFileName + ".jar");

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
            cfg.src = buildfile;
            cfg.out = path.of(nob);
            cfg.addtoclasspath("nob.jar");
        });
    
        buildjar(cfg -> {
            cfg.name = buildfilename + ".jar";
            cfg.jars = path.of("./");
            cfg.mainclass = buildfilename;
            cfg.addtoclasspath("nob.jar");
        });

        // bootstrap
    }
}

