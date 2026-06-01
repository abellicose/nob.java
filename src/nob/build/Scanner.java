/* ======================================
 * File: Scanner.java
 * Date: 2026-06-01
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

public class Scanner {
    static List<String> scan(DiffResult diff, Context ctx) {
        // we open all of the uh changed files
        // scan their methods, and external methods they call
        // compare to their methods list. okay first step is to get the methods they own and methods they call

        // diff contains files with / and .java, with replace those with . and remove .java for ease of calculation
        // we convert it back at the end
        
        // Start Preprocessing Files
        int changedSize = diff.changed().size();
        int deletedSize = diff.deleted().size();
        int totalSize = changedSize + deletedSize;

        Set<String> rebuildList = new HashSet<>(changedSize * 2);
        Map<String, String> classMap = new HashMap<>(totalSize * 2);

        Set<String> changedSourceNames = new HashSet<>(changedSize * 2);
        Set<String> changedClassDirs = new HashSet<>(changedSize * 2);
        List<String> changedClassFiles = new ArrayList<>(changedSize * 2);

        Set<String> deletedSourceNames = new HashSet<>(deletedSize * 2);
        Set<String> deletedClassDirs = new HashSet<>(deletedSize * 2);

        diff.deleted().forEach(file -> {
            SourcePath result = stripExtension(file);
            deletedSourceNames.add(result.extLess);
            deletedClassDirs.add(result.classDir);
        });

        // im deleting classes, but not removing them from methodsOwned, methodsCalled, methodDependents and adding those to rebuild
        for (String dir: deletedClassDirs) {
            Files.list(ctx.out.resolve(dir)).forEach(path -> {
                String sourceName = split(path.toString());
                if (deletedSourceNames.contains(sourceName)) {
                    Files.deleteIfExists(path);
                }
            });
        }

        diff.changed().forEach(file -> {
            SourcePath result = stripExtension(file);
            changedSourceNames.add(result.extLess);
            changedClassDirs.add(result.classDir);
        });

        for (String dir: changedClassDirs) {
            Files.list(ctx.out.resolve(dir)).forEach(path -> {
                String pathDir = path.toString();
                String sourceName = split(pathDir);
                if (changedSourceNames.contains(sourceName)) {
                    classMap.put(pathDir, sourceName);
                    changedClassFiles.add(pathDir);
                }
            });
        }
        // End Preprocessing Files

        Map<String, Set<String>> updatedOwnedMethods = new HashMap<>();
        Map<String, Set<String>> updatedCalledMethods = new HashMap<>();

        // now to loop over the changed files, read them and update shit
        for (String fileDir: changedClassFiles) {
            // Read Contents, Pass it to My Nigga ASM, Get Shit Back, Update Shit

            Set<String> ownedMethods = new HashSet<>();
            Set<String> calledMethods = new HashSet<>();
            ScanState state = new ScanState(ctx.packageName, ownedMethods, calledMethods);
            parseClass(fileDir, state);

            String mainClass = classMap.get(fileDir);
            if (mainClass == null) {
                throw new NobException("Couldn't find source for " + fileDir);
            }

            updatedOwnedMethods.computeIfAbsent(mainClass, k -> new HashSet<>()).addAll(ownedMethods);
            updatedCalledMethods.computeIfAbsent(mainClass, k -> new HashSet<>()).addAll(calledMethods);
        }
        
        for (Map.Entry<String, Set<String>> entry: updatedOwnedMethods.entrySet()) {
            String name = entry.getKey();
            Set<String> prev = ctx.ownedMethods.get(name);
            if (prev == null)
                continue;

            Set<String> curr = entry.getValue();

/*
            for (String method: curr) {
                !prev.remove(method); // !prev.contains(method), new method
            }
*/
            prev.removeAll(curr);

            for (String method: prev) { // deleted methods
                rebuildList.addAll(ctx.methodDependents.remove(method));
            }
        }

        for (Map.Entry<String, Set<String>> entry: updatedCalledMethods.entrySet()) {
            String name = entry.getKey();
            Set<String> prev = ctx.calledMethods.get(name);
            if (prev == null)
                continue;

            Set<String> curr = entry.getValue();
            for (String method: curr) {
                if (!prev.remove(method)) { // new call
                    ctx.methodDependents.computeIfAbsent(method, k -> new HashSet<>()).add(name);
                }
            }

            for (String method: prev) {  // deleted calls
                ctx.methodDependents.computeIfPresent(method, (k, v) -> {
                    v.remove(name);
                    return v;
                });
            }
        }

        ctx.ownedMethods.putAll(updatedOwnedMethods);
        ctx.calledMethods.putAll(updatedCalledMethods);

        return new ArrayList<>(rebuildList.removeAll(changedSourceNames));
    }

    private static void parseClass(String file, ScanState state) {
        Path path = Path.of(file);
        byte[] bytes = Files.readAllBytes(path);
        ClassReader cr = new ClassReader(bytes);
        ClassVisitor cv = new ClassScanner(ASM9, state);
        cr.accept(cv, 0);
    }

    private static String split(String str) {
        int extStart = str.length();
        for(int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '.') extStart = i;
            if (c == '$') extStart = i;
            if (c == '/') break;
        }
        return str.substring(0, extStart);
    }

    // paths are like nob/build/Scanner.java
    // I walk from back, return everything from beginning until that .
    private static SourcePath stripExtension(String str) {
        int extStart = str.length();
        int dirEnd = str.length();
        for (int i = extStart - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '.') extStart = i; 
            if (c == '/') { dirEnd = i; break; }
        }
        return new SourcePath(str.substring(0, dirEnd), str.substring(0, extStart));
    }

    record SourcePath(String classDir, String extLess) {}
}

class ScanState {
    String className;
    String packageName;
    Set<String> ownedMethods;
    Set<String> calledMethods;

    ScanState(String packageName, Set<String> ownedMethods, Set<String> calledMethods) {
        this.packageName = packageName;
        this.ownedMethods = ownedMethods;
        this.calledMethods = calledMethods;
    }
}

class ClassScanner extends ClassVisitor {
    ScanState state;
    public ClassScanner(int api, ScanState state) {
        super(api);
        this.state = state;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // track class name
        state.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // return custom method visitor if method aint part of this class
        state.ownedMethods.add(state.className + "/" + name + descriptor);
        MethodScanner scanner = new MethodScanner(api, state);
        return scanner;
    }
}

class MethodScanner extends MethodVisitor {
    ScanState state;
    public MethodScanner(int api, ScanState state) {
        super(api);
        this.state = state;
    }

    @Override
    void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        // track method calls in this class
        if (owner.startsWith(state.packageName) && !owner.equals(state.className)) {
            state.calledMethods.add(owner + "/" + name + descriptor);
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
