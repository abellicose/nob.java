/* ======================================
 * File: DependencyBuilder.java
 * Date: 2026-05-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.analysis;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class DependencyBuilder extends ClassVisitor {
    public DependencyBuilder(int api) {
        super(api);
    }
    
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        System.out.println("Name: " + name + ", Descriptor: " + descriptor + ", Signature: " + signature);
        SignatureTracker st = new SignatureTracker(org.objectweb.asm.Opcodes.ASM9);
        return st;
    }

}

