/* ======================================
 * File: Nob.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import nob.build.dependencyresolver.Dependency;
import nob.build.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public class Nob {
    public String sourceDir     = "src/";
    public String buildDir      = "build/";
    public String classesDir    = "classes/";   // Relative to buildDir
    public String libsDir       = "build/libs"; // Relative to project root
    public String mainClass     = null;
    public String packageName   = null;
    public String jarName       = "out.jar";

    public Context ctx          = null;
    public TaskPipeline graph          = new TaskPipeline();

    public static boolean debug = false;

    public void init() {
        try {
            if (ctx != null) return;
            ctx = Context.load(this);

            PluginPipeline pp = new PluginPipeline(graph);
            Test first = new Test("first", "second");
            Test second = new Test("second", "third");
            Test third = new Test("third");
            pp.register(first);
            pp.register(second);
            pp.register(third);

            ctx.deps.add(new Dependency("org.ow2.asm", "asm", "9.10.1"));

            Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
                try {
                    // graph.run("package", ctx);
                    pp.run();
                } catch (NobException e) {
                    handle(e);
                }
                Logger.stop();
            }));
        } catch (NobException e) {
            handle(e);
            System.exit(1);
        }
    }

    private void handle(NobException e) {
        Logger.error(e.getMessage());
        if (e.getCause() != null) e.getCause().printStackTrace();
    }
}

class Test implements Plugin {
    private final String id;
    private final List<String> deps;

    public Test(String id) {
        this.id = id;
        this.deps = List.of();
    }

    public Test(String id, String... deps) {
        this.id = id;
        this.deps = new ArrayList<>(Arrays.asList(deps));
    }

    public String id() {
        return this.id;
    }

    public List<String> dependsOn() {
        return this.deps;
    }

    public void apply(Registrar reg) {
        Logger.info("Executing testtask");
    }
}
