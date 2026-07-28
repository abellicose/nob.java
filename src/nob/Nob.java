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

            Registrar reg = new Registrar("maven", graph);

            ctx.deps.add(new Dependency("org.ow2.asm", "asm", "9.10.1"));

            reg.createLifecycle("resolve");

            reg.createLifecycle("compile");
            reg.addAfter("compile", "resolve");

            reg.createLifecycle("package");
            reg.addAfter("package", "compile");
            reg.addAfter("resolve", "package");

            String resSig = reg.registerTask(new ResolveTask());
            reg.addTo(resSig, "resolve");
            String testSig = reg.registerTask(new Test());
            reg.addTo(testSig, "resolve$start");

            String compSig = reg.registerTask(new CompileTask());
            reg.addTo(compSig, "compile");

            String pkgSig = reg.registerTask(new PackageTask());
            reg.addTo(pkgSig, "package");

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
