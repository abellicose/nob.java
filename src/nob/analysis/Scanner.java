/* ======================================
 * File: Scanner.java
 * Date: 2026-05-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import nob.cache.BuildContext;

public class Scanner {
    
    // Convention: all internal storage uses class paths without extension
    // e.g. "nob/core/Compile" not "nob/core/Compile.java"
    // Only add .java when feeding to javac, only add .class when reading bytecode
    public static List<String> scan(DiffResult diff, BuildContext ctx) throws Exception {
        // should never happen
        if (diff.changed.isEmpty() && diff.deleted.isEmpty()) {
            System.out.println("[nob] No files have been changed.");
            return List.of();
        }

        Set<String> rebuildList = new HashSet<>();
        Map<String, String> changeMap = getCorrespondingClassFiles(diff.deleted, ctx.out);

        for (String file: changeMap.values()) {
            List<String> deps = ctx.methods.remove(file);
            if (deps == null) continue;
            for (String method: deps) {
                rebuildList.addAll(ctx.methodCalls.remove(method));
            }

            deps = ctx.methodsCalled.remove(file);
            if (deps == null) continue;
            for (String method: deps) {
                ctx.methodCalls.computeIfPresent(method, (k, v) -> {
                    v.remove(file);
                    return v;
                });
            }
        }

        changeMap = getCorrespondingClassFiles(diff.changed, ctx.out);

        Map<String, List<String>> updatedMethods = new HashMap<>();
        Map<String, List<String>> updatedMethodsCalled = new HashMap<>();

        // Methods: Every method inside a source file (and its inner classes)
        // MethodsCalled: Every method called from a source file (and its inner classes)
        // MethodCalls: Every individual methods and their dependencies

        // TODO: For Classes With Inner classes, The entire source (class + inner class) needs to be processed first before updating.
        for (String file: changeMap.keySet()) {
            byte[] bytecode = Files.readAllBytes(ctx.out.resolve(file + ".class"));

            Set<String> methods = new HashSet<>();
            Set<String> methodsCalling = new HashSet<>();

            Scan sctx = new Scan(ctx.packageName, methods, methodsCalling);
            if (!sctx.className.equals(file)) {
                throw new NobException("WHAT, ClassName: " + sctx.className + ", FileName: " + file);
            }

            scanFile(bytecode, sctx);

            String fileName = changeMap.get(sctx.className);

            List<String> oldMethodsCalled = ctx.methodsCalled.get(fileName);
            updatedMethodsCalled.computeIfAbsent(fileName, k -> new ArrayList<>()).addAll(methodsCalling);

            if (oldMethodsCalled != null) {
                // ts complicated on purpose
                // if we can't remove a method from the new list, that means that method existed
                // but doesnt exist anymore. which means it has been removed
                // so we get the deps list for this method, and remove this class from it
                // and after removing all, we might have some methods left that exist now but 
                // weren't before, meaning they were added. so we add ourselves to their
                // deps list
                for (String method: oldMethodsCalled) {
                    if (!methodsCalling.remove(method)) {
                        // method we were calling, but now we arent
                        ctx.methodCalls.get(method).remove(fileName);
                    }  
                }
                // methods we are calling but weren't
                for (String method: methodsCalling) {
                    ctx.methodCalls.computeIfAbsent(method, k -> new ArrayList<>()).add(fileName);
                }
            }

            List<String> oldCachedMethods = ctx.methods.get(fileName);
            updatedMethods.computeIfAbsent(fileName, k -> new ArrayList<>()).addAll(methods);

            if (oldCachedMethods != null) {
                for (String method: oldCachedMethods) {
                    if (!methods.contains(method)) {
                        List<String> deps = ctx.methodCalls.get(method);
                        if (deps == null) continue;
                        rebuildList.addAll(deps);
                    }
                }
            }
        }

        for (Map.Entry<String, List<String>> entry: updatedMethods.entrySet()) {
            ctx.methods.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, List<String>> entry: updatedMethodsCalled.entrySet()) {
            ctx.methodsCalled.put(entry.getKey(), entry.getValue());
        }

        for (String processed: diff.changed) {
            rebuildList.remove(processed);
        }

        return new ArrayList(rebuildList);
    }

    static void scanFile(byte[] bytecode, ScanContext ctx) {
        ClassReader cr = new ClassReader(bytecode);
        ClassVisitor cv = new DependencyBuilder(org.objectweb.asm.Opcodes.ASM9, ctx);
        cr.accept(cv, 0);
    }

    static Map<String, String> getCorrespondingClassFiles(List<String> sourceFiles, Path out) {
        Set<String> sourceSet = new HashSet<>();
        for (String sourceFile: sourceFiles) {
            sourceSet.add(sourceFile.replace(".java", ""));
        }

        Map<String, String> classMap = new HashMap<>();
        try (var stream = Files.walk(out)) { // out is build/classes/
            stream.forEach(path -> {
                String pathString = out.relativize(path).toString();
                String noExt = pathString.replace(".class", "");
                String outer = noExt.split("\\$")[0];
                if (sourceSet.contains(outer)) {
                    classMap.put(noExt, outer);
                }
            })
        }
        return classMap;
    }
}

