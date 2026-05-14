/* ====================================== 
 * File: Nob.java
 * Date: 2026-05-14
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.lang.ProcessBuilder;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.nio.file.attribute.FileTime;
import java.lang.StringBuilder;
import java.util.Map;
import java.util.HashMap;

class CompileConfig {
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

class ManifestConfig {
    String mainClass = null;
    List<String> classpath = new ArrayList<>();
    Map<String, String> extraAttributes = new HashMap<>();

    ManifestConfig addToClasspath(String... args) {
        classpath.addAll(List.of(args));
        return this;
    }

    ManifestConfig addExtraAttributes(String... args) {
        for (int i = 0; i < args.length; i+=2) {
            extraAttributes.put(args[i], args[i+1]);
        }
        return this;
    }
    
    protected String buildManifest() {
        StringBuilder builder = new StringBuilder("Manifest-Version: 1.0\n");

        if (mainClass != null) builder.append("Main-Class: " + mainClass + "\n");
        if (!classpath.isEmpty()) builder.append("Class-Path: " + String.join(" ", classpath) + "\n");
        extraAttributes.forEach((k, v) -> builder.append(k + ": " + v + "\n"));
        return builder.toString();
    }
}

public class Nob {
    public static Path buildDir = Path.of("build/");
    public static Path srcDir = Path.of("src/");
    public static String jarsDir = null;
    public static String classesDir = "classes/";
    private static final Path NOBjarPath = Path.of("nob.jar");
    private static final String nobClassesDir = ".nob/";

    private static Path srcPath() {
        return srcDir;
    }

    private static Path classesPath() {
        return buildDir.resolve(classesDir);
    }

    private static Path libsPath() {
        return buildDir.resolve("libs/");
    }

    private static Path jarsPath() {
        return jarsDir == null ? buildDir.resolve("jars/"): Path.of(jarsDir);
    }

    public static boolean buildJar(String jarName, Consumer<ManifestConfig> consumer) {
        ManifestConfig config = new ManifestConfig();
        consumer.accept(config);
        return buildJar(jarName, config);

    }

    // return if succeeded
    // TODO: temporarily returning a boolean, if there's no case where we can do something else if jar hasnt been built properly then we can system.exit(1)
    public static boolean buildJar(String jarName, ManifestConfig manifest) {
        if (!NOBmkdirIfNotExists(jarsPath())) {
            System.out.println("Failed to make jarsDirPath");
            System.exit(1);
        }

        try {
            int classFileCount = Files.walk(classesPath()).filter(path -> path.toString().endsWith(".class")).toList().size();
            if (classFileCount == 0) {
                System.out.println("No class files found.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("Failed to find class files.");
            e.printStackTrace();
            return false;
        }

        manifest.extraAttributes.put("Created-By", "Nob");

        Path manifestFile;
        try {
            manifestFile = Files.createTempFile("MANIFEST", ".MF");
            Files.writeString(manifestFile, manifest.buildManifest());
        } catch(Exception e) {
            System.out.println("Couldn't create/write to temporary manifest file!");
            e.printStackTrace();
            return false;
        }

        List<String> cmd = new ArrayList<>(List.of("jar", "cfm", jarsPath().resolve(jarName).toString(), manifestFile.toString(), "-C", classesPath().toString(), "."));

        int processStatus = 1;
        try {
            processStatus = new ProcessBuilder(cmd)
                                .inheritIO()
                                .start()
                                .waitFor();
        } catch(Exception e) {
            e.printStackTrace();
        }

        try {
            Files.delete(manifestFile);
        } catch(Exception e) {
            System.out.println("Couldn't delete temp manifest flie");
            e.printStackTrace();
        }

        if (processStatus != 0) {
            System.out.println("Jar creation failed");
            return false;
        }
        System.out.println("Jar creation succeeded");
        return true;
    }
    
    // returns if succeeded
    public static void compile(Consumer<CompileConfig> consumer) {
        CompileConfig config = new CompileConfig();
        consumer.accept(config);
        compile(config);
    }

    public static void compile(CompileConfig config) {
        if (!NOBmkdirIfNotExists(classesPath())) {
            System.out.println("Failed to make nobPath");
            System.exit(1);
        }

        List<String> files = null;
        try {
            files = Files.walk(srcPath()).filter(path -> path.toString().endsWith(".java")).map(Path::toString).toList();
        } catch (Exception e) {
            System.out.println("Failed to find source files.");
            e.printStackTrace();
            System.exit(1);
        }

        if (files != null && files.isEmpty()) {
            System.out.println("No source files found!");
            System.exit(1);
        }

        List<String> cmd = new ArrayList<>(List.of("javac"));

        // --add-modules
        if (!config.modules.isEmpty()) {
            cmd.add("--add-modules");
            cmd.addAll(config.modules);
        }

        // classpath
        if (!config.classpath.isEmpty()) {
            cmd.add("-cp");
            cmd.add(".:" + String.join(":", config.classpath));
        }

        // build dir
        cmd.add("-d");
        cmd.add(classesPath().toString());

        cmd.addAll(config.flags);
        cmd.add("-Xlint");
        cmd.addAll(files);

        int processStatus = 1;
        try {
            processStatus = new ProcessBuilder(cmd)
                                .inheritIO()
                                .start()
                                .waitFor();
        } catch(Exception e) {
            e.printStackTrace();
        }

        if (processStatus != 0) {
            System.out.println("Compilation command failed");
            System.exit(1);
        }
        System.out.println("Compilation succeeded");

        if (!config.dirsToInclude.isEmpty()) {
            System.out.println("Moving Dirs to Include");
            Path srcPathFinal = srcPath();

            config.dirsToInclude.forEach(dir -> {
                Path src = srcPathFinal.resolve(dir);
                try {
                    Files.walk(src).forEach(file -> {
                        try {
                            Path relative = src.getParent().relativize(file);
                            Files.copy(file, classesPath().resolve(relative), StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                } catch (Exception e) { e.printStackTrace(); };
            });
        }
    }

    // returns if succeeded
    public static boolean NOBmkdirIfNotExists(Path path) {
        if (Files.notExists(path)) {
            try {
                Files.createDirectories(path);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }


    // buildClassName -> self explanatory, dont add .java
    public static void goRebuildUrself(String buildClassName) {
        // Check Build Files Existance
        Path buildFile = Path.of(buildClassName + ".java");
        if (!Files.exists(buildFile)) {
            System.out.println("Build file " + buildClassName + " does not exist at the specified path!");
            System.exit(1);
        }

        // Check if Needs jar Needs Building
        long mTimeSrc = Long.MAX_VALUE;
        long mTimeJar = 0;
        boolean jarExists = Files.exists(NOBjarPath);

        try {
            mTimeSrc = Files.getLastModifiedTime(buildFile).toMillis();
        } catch (Exception e) {
            System.out.println("Failed to get modification time of build file.");
            e.printStackTrace();
            System.exit(1);
        }

        if (jarExists) {
            try {
                mTimeJar = Files.getLastModifiedTime(NOBjarPath).toMillis();
            } catch (Exception e) {
                System.out.println("Failed to get modification time of .");
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.out.println("SrcModTime: "  + mTimeSrc + ", JarModTime: " + mTimeJar);
        
        if (mTimeJar > mTimeSrc) {
            System.out.println("Jar is newest!");
            return;
        }

        // Delete Jar
        try {
            Files.deleteIfExists(NOBjarPath);
        } catch (Exception e) {
            System.out.println("Could not delete old jar file!");
            e.printStackTrace();
        }

        // Compile
        srcDir = buildFile;
        classesDir = nobClassesDir;
        compile(config -> {
            config.addToClasspath("Nob.jar");
        });
    
        // package
        jarsDir = "./";
        boolean status = buildJar(buildClassName + ".jar", manifest -> {
            manifest.mainClass = buildClassName;
            manifest.addToClasspath("Nob.jar");
        });

        // TODO: Maybe bove this into buildJar
        if (!status) {
            System.out.println("Couldn't build jar");
            System.exit(1);
        }

        // bootstrap here
    }
}

