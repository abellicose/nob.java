/* ======================================
 * File: SignatureTracker.java
 * Date: 2026-05-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

import org.objectweb.asm.MethodVisitor;

public class SignatureTracker extends MethodVisitor {
    public SignatureTracker(int api) {
        super(api);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        System.out.println("Owner: " + owner + ", Name: " + name + ", Descriptor: " + descriptor);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}

