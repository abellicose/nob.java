/* ======================================
 * File: DiffResult.java
 * Date: 2026-05-29
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.cache;

import java.util.List;

public record DiffResult(List<String> changed, List<String> deleted) {}

