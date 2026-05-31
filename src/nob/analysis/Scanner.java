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
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import nob.cache.BuildContext;
import nob.cache.DiffResult;
import nob.util.NobException;

public class Scanner {
    
    // Convention: all internal storage uses class paths without extension
    // e.g. "nob/core/Compile" not "nob/core/Compile.java"
    // Only add .java when feeding to javac, only add .class when reading bytecode
    public static List<String> scan(DiffResult diff, BuildContext ctx) throws Exception {
        // should never happen
        if (diff.changed().isEmpty() && diff.deleted().isEmpty()) {
            System.out.println("[nob] No files have been changed.");
            return List.of();
        }

        Set<String> rebuildList = new HashSet<>();
        Map<String, String> changeMap = getCorrespondingClassFiles(diff.deleted(), ctx.out);
        Set<String> files = new HashSet<>(changeMap.values());

        // iterating through the class files of the files deleted (if exists)
        for (String file: files) {
            // last existing methods of the deleted class
            Set<String> deps = ctx.methods.remove(file);
            if (deps == null) continue;
            // for all methods, remove their dependency classes and add them to the rebuild list
            for (String method: deps) {
                rebuildList.addAll(ctx.methodCalls.remove(method));
            }

            // for all the external methods this class called
            deps = ctx.methodsCalled.remove(file);
            if (deps == null) continue;
            // remove this class as a dependent
            for (String method: deps) {
                // this be lowkey expensive,
                ctx.methodCalls.computeIfPresent(method, (k, v) -> {
                    v.remove(file);
                    return v;
                });
            }
        }

        // .class files for all source files that changed
        changeMap = getCorrespondingClassFiles(diff.changed(), ctx.out);

        // new methods and new methods this class deps on
        Map<String, Set<String>> updatedMethods = new HashMap<>();
        Map<String, Set<String>> updatedMethodsCalled = new HashMap<>();

        // Methods: Every method inside a source file (and its inner classes)
        // MethodsCalled: Every method called from a source file (and its inner classes)
        // MethodCalls: Every individual methods and their dependencies

        for (String file: changeMap.keySet()) {
            // read bytes of a class
            byte[] bytecode = Files.readAllBytes(ctx.out.resolve(file + ".class"));

            // scan the methods and the methods being called by this class
            Set<String> methods = new HashSet<>();
            Set<String> methodsCalled = new HashSet<>();

            ScanContext sctx = new ScanContext(ctx.packageName, methods, methodsCalled);
            scanFile(bytecode, sctx);

            if (!sctx.className.equals(file)) {
                throw new NobException("WHAT, ClassName: " + sctx.className + ", FileName: " + file);
            }

            String fileName = changeMap.get(sctx.className);

            // add the parent class (if one) as the entry and track its methods and methodcalls
            updatedMethods.computeIfAbsent(fileName, k -> new HashSet<>()).addAll(methods);
            updatedMethodsCalled.computeIfAbsent(fileName, k -> new HashSet<>()).addAll(methodsCalled);
        }

        // for each parent file, compare the methods it calls, if a method stopped being called, remove ourselves from their dependency list
        // if we call new external methods, add ourselves to their dep list
        for (String file: files) {
            Set<String> prev = ctx.methodsCalled.get(file);
            Set<String> curr = updatedMethodsCalled.get(file);
            if (prev != null) {
                // ts complicated on purpose
                // if we can't remove a method from the new list, that means that method existed
                // but doesnt exist anymore. which means it has been removed
                // so we get the deps list for this method, and remove this class from it
                // and after removing all, we might have some methods left that exist now but 
                // weren't before, meaning they were added. so we add ourselves to their
                // deps list
                for (String method: prev) {
                    if (!curr.remove(method)) {
                        // method we were calling, but now we arent
                        ctx.methodCalls.get(method).remove(file);
                    }  
                }
                // methods we are calling but weren't
                for (String method: curr) {
                    ctx.methodCalls.computeIfAbsent(method, k -> new HashSet<>()).add(file);
                }
            }

            // Compare the list of old and new methods this class has,
            // if any has been deleted, rebuild their dependency classes
            prev = ctx.methods.get(file);
            curr = updatedMethods.get(file);

            if (prev != null) {
                for (String method: prev) {
                    if (!curr.contains(method)) {
                        Set<String> deps = ctx.methodCalls.remove(method);
                        if (deps == null) continue;
                        rebuildList.addAll(deps);
                    }
                }
            }
        }

        // update cache with all the newly tracked data
        ctx.methods.putAll(updatedMethods);
        ctx.methodsCalled.putAll(updatedMethodsCalled);
        List<String> filteredList = new ArrayList<>();
        Set<String> filter = new HashSet<>(diff.changed());
        for (String file: rebuildList) {
            String f = file + ".java";
            if (!filter.contains(f)) {
                filteredList.add(f);
            }
        }
        
        return filteredList;
    }

    static void scanFile(byte[] bytecode, ScanContext ctx) {
        ClassReader cr = new ClassReader(bytecode);
        ClassVisitor cv = new DependencyBuilder(org.objectweb.asm.Opcodes.ASM9, ctx);
        cr.accept(cv, 0);
    }

    static Map<String, String> getCorrespondingClassFiles(List<String> sourceFiles, Path out) throws Exception {
        Set<String> sourceSet = new HashSet<>();
        for (String sourceFile: sourceFiles) {
            sourceSet.add(sourceFile.replace(".java", ""));
        }

        // This is essentially a list, map so that inner classes map to the outer class files
        Map<String, String> classMap = new HashMap<>();
        try (var stream = Files.walk(out)) { // out is build/classes/
            stream.forEach(path -> {
                String pathString = out.relativize(path).toString();
                String noExt = pathString.replace(".class", "");
                String outer = noExt.split("\\$")[0];
                if (sourceSet.contains(outer)) {
                    classMap.put(noExt, outer);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new NobException("Could not find .class files");
        }
        return classMap;
    }
}

