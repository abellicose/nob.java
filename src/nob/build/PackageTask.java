/* ======================================
 * File: PackageTask.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import nob.Task;

public class PackageTask implements Task{
    public String id() {
        return "package";
    }

    public List<String> dependsOn() {
        return List.of("compile");
    }

    public void execute(Context ctx) {
        Path manifestFile = createManifest(ctx);
        List<String> cmd = new ArrayList<>(List.of("jar", "cfm", ctx.jarOut.resolve(ctx.jarName).toString(), manifestFile.toString(), "-C", ctx.out.toString(), "."));
        Logger.debug("JarCmd: " + cmd);

        try {
            int exit = new ProcessBuilder(cmd).inheritIO().start().waitFor();
            if (exit != 0) throw new NobException("javac failed with exit code " + exit);
        } catch (NobException e) {
            throw e;
        } catch (Exception e) {
            throw new NobException("failed to start javac", e);
        } finally {
            try {
                Files.deleteIfExists(manifestFile);
            } catch (Exception ignored) {
                Logger.info("Failed to delete temp manifest file.");
            }
        }
    }

    public Path createManifest(Context ctx) {
        try {
            Path tempFile = Files.createTempFile("MANIFEST", ".MF");
            ctx.jarConfig.mfAttribs.put("Created-By", "Nob");
            Files.writeString(tempFile, ctx.manifest());
            return tempFile;
        } catch (Exception e) { 
            throw new NobException("Failed to create a temporary manifest file", e);
        }
    }
}
