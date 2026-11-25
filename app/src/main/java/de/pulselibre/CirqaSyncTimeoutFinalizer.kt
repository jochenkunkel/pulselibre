package de.pulselibre

/** Marks an in-flight file as failed when the bounded session deadline tears down GATT. */
internal object CirqaSyncTimeoutFinalizer {
    fun failCurrentItem(localId: Long?, ledger: CirqaSyncPipelineLedger): Boolean =
        localId?.let { ledger.record(it, CirqaSyncPipelineLedger.Stage.FAILED) } ?: false
}
