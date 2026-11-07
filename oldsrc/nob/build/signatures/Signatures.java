/* ======================================
 * File: Signatures.java
 * Date: 2026-08-16
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

public final class Signatures {
    private Signatures() {}

    public static boolean isForeign(String rawId) {
        return rawId.contains(":");
    }
}

