package com.ieltspath.access.application.port;

import com.ieltspath.access.application.result.WalletMismatch;

import java.util.List;

public interface WalletReconciliationStore {
    List<WalletMismatch> findMismatches(int limit);
}
