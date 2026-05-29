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
    
    public static List<String> scan(DiffResult result, BuildContext ctx) throws Exception {
        if (changed.isEmpty()) {
            System.out.println("[nob] No files have been changed.");
            return List.emptyList();
        }

        List<String> rebuildList = new ArrayList<>();

        for (String file: changed) {
            byte[] bytecode = Files.readAllBytes(ctx.out.resolve(file.replace(".java", ".class")));

            Set<String> methods = new HashSet<>();
            Set<String> methodsCalling = new HashSet<>();

            Scan sctx = new Scan(ctx.packageName, methods, methodsCalling);
            scanFile(bytecode, sctx);

            List<String> oldMethodsCalled = ctx.methodsCalled.get(sctx.className);
            ctx.methodsCalled.put(sctx.className, new ArrayList<>(methodsCalling));

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
                        ctx.methodCalls.get(method).remove(sctx.className);
                    }  
                }
                // methods we are calling but weren't
                for (String method: methodsCalling) {
                    ctx.methodCalls.computeIfAbsent(method, k -> new ArrayList<>()).add(sctx.className);
                }
            }

            List<String> oldCachedMethods = ctx.methods.get(sctx.className);
            ctx.methods.put(sctx.className, new ArrayList<>(methods));  

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

        rebuildList.removeAll(new HashSet(changed));
        return rebuildList;
    }

    static void scanFile(byte[] bytecode, ScanContext ctx) {
        ClassReader cr = new ClassReader(bytecode);
        ClassVisitor cv = new DependencyBuilder(org.objectweb.asm.Opcodes.ASM9, ctx);
        cr.accept(cv, 0);
    }
}

