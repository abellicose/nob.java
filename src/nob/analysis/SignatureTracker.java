/* ======================================
 * File: SignatureTracker.java
 * Date: 2026-05-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.analysis;

import org.objectweb.asm.MethodVisitor;

public class SignatureTracker extends MethodVisitor {
    ScanContext ctx;
    public SignatureTracker(int api, ScanContext ctx) {
        super(api);
        this.ctx = ctx;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        if (owner.startsWith(ctx.packageName) && !owner.equals(ctx.className)) {
            this.methodCalls.add(owner + "/" + name + descriptor);
            System.out.println("Owner: " + owner + ", Name: " + name + ", Descriptor: " + descriptor + ", Val: " + owner + name + descriptor);
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}

