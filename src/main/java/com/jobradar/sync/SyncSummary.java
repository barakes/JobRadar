package com.jobradar.sync;

import java.util.List;

public record SyncSummary(int succeeded, int failed, List<SyncResult> results) {
}