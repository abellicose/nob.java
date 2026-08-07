/* ======================================
 * File: NamespaceIdTest.java
 * Date: 2026-08-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;
import org.junit.Test;
import static org.junit.Assert.*;

public class NamespaceIdTest {

    @Test
    public void parsesValidSignature() {
        NamespaceId id = NamespaceId.parse("jvm:sources");
        assertEquals("jvm", id.pluginId());
        assertEquals("sources", id.name());
    }

    @Test
    public void throwsOnMissingName() {
        assertThrows(NobException.class, () -> NamespaceId.parse("jvm:"));
    }

    @Test
    public void throwsOnNoColon() {
        assertThrows(NobException.class, () -> NamespaceId.parse("nocolon"));
    }

    @Test
    public void throwsOnTooManyParts() {
        assertThrows(NobException.class, () -> NamespaceId.parse("a:b:c"));
    }
}

