/* ======================================
 * File: CompileTask.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.function.Consumer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import java.io.IOException;
import java.io.Serializable;
import nob.NobException;
import nob.Task;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.JavaFileManager;
import javax.tools.FileObject;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public class CompileTask implements Task {
    public String id() {
        return "compile";
    }

    public List<String> dependsOn() { 
        return List.of("resolve"); 
    }

    // TODO: Test if a stale dependency in ctx.out crashes this
    // A.class (already built) depends on B.class
    // if B.class changed its method signature, they wont work properly unless A.class is
    // recompiled. See if Changing B but not A does something to this.
    // Should be handled by toCompile() but just in case
    public void execute(Context ctx) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        FileManager fm = new FileManager(ctx, compiler.getStandardFileManager(diagnostics, null, null));
        List<String> options = ctx.compileConfig.compilerFlags;

        List<Path> paths = new ArrayList<>();
        paths.add(ctx.out);
        paths.addAll(ctx.libJars);
        for (String path: ctx.compileConfig.classpath) {
            paths.add(Path.of(path));
        }

        try {
            fm.getDelegate().setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of(ctx.source));
            fm.getDelegate().setLocationFromPaths(StandardLocation.CLASS_PATH, paths);
            fm.getDelegate().setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(ctx.out));

            DiffResult diff = diff(ctx);

            List<String> toRecompile = diff.changed();
            List<String> toDelete = diff.deleted();
            Logger.info("Files to delete: " + toDelete);
            int n = 0;

            while (!toRecompile.isEmpty()) {
                Logger.info("Files to recompilee: " + toRecompile);
                List<Path> sourcePaths = toRecompile.stream()
                .map(m -> ctx.source.resolve(m + ".java"))
                .collect(Collectors.toList());
                Iterable<? extends JavaFileObject> files = fm.getDelegate().getJavaFileObjectsFromPaths(sourcePaths);

                JavaCompiler.CompilationTask task = compiler.getTask(null, fm, diagnostics, options, null, files);

                boolean done = task.call();


                if (!done) {
                    for (Diagnostic<? extends JavaFileObject> d: diagnostics.getDiagnostics()) {
                        Logger.info(d.toString());
                    }
                    throw new NobException("javac failed");
                }

                List<String> out = new ArrayList<>(toRecompile.size());
                Scanner.scan(toRecompile, toDelete, out, ctx);
                toRecompile = out;
                toDelete = List.of();

                if (++n > 4) 
                    throw new NobException("Recompile loop executed too many times.");
            }

            if (n == 0) {
                Logger.info("All files are UP-TO-DATE");
            } 

            ctx.save(); 

            // TODO: Don't close here, keep it alive during the lifetime of the process
            fm.close();
        } catch (Exception e) {
            Logger.warn("Scan failed");
            e.printStackTrace();
        }
    }

    DiffResult diff(Context ctx) {
        List<String> changed = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        DiffResult diff = new DiffResult(changed, removed);

        FileTree curr = FileTree.build(ctx.source.toString());

        if (ctx.cachedTree == null) {
            curr.collectLeaves(ctx.source.toString(), changed);
        } else {
            FileTree.diff(ctx.cachedTree, curr, diff);
        }

        ctx.newTree = curr;
        return diff;
    }

}

class FileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    Context ctx;
    int offset;

    public FileManager(Context ctx, StandardJavaFileManager fm) {
        super(fm);
        this.ctx = ctx;
        this.offset = ctx.source.toString().length() + 1;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        if (sibling != null) {
            String source = sibling.getName();
            source = source.substring(offset, source.length() - 5);
            String binary = className.replace(".", "/");
            ctx.sourceToBinaries.computeIfAbsent(source, k -> new HashSet<>()).add(binary);
            ctx.binaryToSource.put(binary, source);
        }
        return super.getJavaFileForOutput(location, className, kind, sibling);
    }

    public StandardJavaFileManager getDelegate() {
        return fileManager; // ForwardingJavaFileManager exposes this as protected field
    }
}
