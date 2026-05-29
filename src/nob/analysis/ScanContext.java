/* ======================================
 * File: ScanContext.java
 * Date: 2026-05-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.analysis;

import java.util.Set;

public class ScanContext {
    public String className;
    public String packageName;
    public Set<String> methods;
    public Set<String> methodCalls;
    
    public ScanContext(String packageName, Set<String> methods, Set<String> methodCalls) {
        this.packageName = packageName;
        this.methods = methods;
        this.methodCalls = methodCalls;
    }
}

