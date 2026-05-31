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
    ScanContext ctx;

    public DependencyBuilder(int api, ScanContext ctx) {
        super(api);
        this.ctx = ctx;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.ctx.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }
    
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        this.ctx.methods.add(this.ctx.className + "/" + name + descriptor);
        System.out.println("ClassName: " + this.ctx.className + ", Method: " + this.ctx.className + name + descriptor);
        SignatureTracker st = new SignatureTracker(org.objectweb.asm.Opcodes.ASM9, this.ctx);
        return st;
    }

}

