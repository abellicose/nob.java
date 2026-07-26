/* ======================================
 * File: Nob.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import nob.build.*;
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
    public Graph graph          = new Graph();

    public static boolean debug = false;

    public void init() {
        try {
            if (ctx != null) return;
            ctx = Context.load(this);

            ctx.deps.add(new Dependency("org.ow2.asm", "asm", "9.10.1"));

            graph.createLifecycle("resolve");

            graph.createLifecycle("compile");
            graph.addAfter("compile", "resolve");

            graph.createLifecycle("package");
            graph.addAfter("package", "compile");
            graph.addAfter("resolve", "package");

            graph.registerTask(new ResolveTask());
            graph.addTo("resolvetask", "resolve");
            graph.registerTask(new Test());
            graph.addTo("testtask", "resolve$start");

            graph.registerTask(new CompileTask());
            graph.addTo("compiletask", "compile");

            graph.registerTask(new PackageTask());
            graph.addTo("packagetask", "package");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
                try {
                    graph.run("package", ctx);
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

class Test implements Task {
    public String id() {
        return "testtask";
    }

    public void execute(Context ctx) {
        Logger.info("Executing testtask");
    }
}
