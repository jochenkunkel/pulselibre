package de.pulselibre

/** Typed, per-file outcomes for the existing MLR → verified file → FIT → SQLite path. */
internal object CirqaSyncFileCompletion {
    enum class Stage {
        NOT_REACHED,
        SUCCEEDED,
        FAILED,
        UNSUPPORTED,
        NOT_APPLICABLE
    }

    enum class Format { FIT, NON_FIT, UNKNOWN }

    enum class Status {
        TRANSFERRED,
        VERIFIED,
        DECODED,
        IMPORTED,
        NO_IMPORTABLE_DATA,
        UNSUPPORTED,
        FAILED
    }

    data class Result(
        val format: Format,
        val transfer: Stage,
        val integrity: Stage,
        val privateStorage: Stage,
        val decode: Stage,
        val sqliteCommit: Stage,
        /** Null means decode did not produce a count, not that the count was zero. */
        val importableObservations: Int? = null,
        val detail: String
    ) {
        init {
            require(importableObservations == null || importableObservations >= 0)
        }

        val status: Status
            get() = when {
                transfer == Stage.FAILED || integrity == Stage.FAILED ||
                    privateStorage == Stage.FAILED || sqliteCommit == Stage.FAILED -> Status.FAILED
                decode == Stage.UNSUPPORTED -> Status.UNSUPPORTED
                format == Format.NON_FIT && privateStorage == Stage.SUCCEEDED -> Status.VERIFIED
                sqliteCommit == Stage.SUCCEEDED && importableObservations == 0 ->
                    Status.NO_IMPORTABLE_DATA
                sqliteCommit == Stage.SUCCEEDED -> Status.IMPORTED
                decode == Stage.SUCCEEDED -> Status.DECODED
                privateStorage == Stage.SUCCEEDED -> Status.VERIFIED
                transfer == Stage.SUCCEEDED -> Status.TRANSFERRED
                else -> Status.FAILED
            }

        /** Valid non-FIT files are retained successfully; unsupported FIT is not a completed import. */
        val successfulForSync: Boolean
            get() = when (status) {
                Status.VERIFIED, Status.IMPORTED, Status.NO_IMPORTABLE_DATA -> true
                else -> false
            }

        fun redactedSummary(): String =
            "$detail;pipeline=${status.name};stages=" +
                "transfer:${transfer.name},integrity:${integrity.name}," +
                "privateStorage:${privateStorage.name},decode:${decode.name}," +
                "sqliteCommit:${sqliteCommit.name}"
    }

    data class StageCounts(
        var transferred: Int = 0,
        var verified: Int = 0,
        var privatelyStored: Int = 0,
        var decoded: Int = 0,
        var sqliteCommitted: Int = 0,
        var imported: Int = 0,
        var noImportableData: Int = 0,
        var unsupported: Int = 0,
        var failed: Int = 0
    ) {
        fun record(result: Result) {
            if (result.transfer == Stage.SUCCEEDED) transferred++
            if (result.integrity == Stage.SUCCEEDED) verified++
            if (result.privateStorage == Stage.SUCCEEDED) privatelyStored++
            if (result.decode == Stage.SUCCEEDED) decoded++
            if (result.sqliteCommit == Stage.SUCCEEDED) sqliteCommitted++
            when (result.status) {
                Status.IMPORTED -> imported++
                Status.NO_IMPORTABLE_DATA -> noImportableData++
                Status.UNSUPPORTED -> unsupported++
                Status.FAILED -> failed++
                else -> Unit
            }
        }

        fun reset() {
            transferred = 0
            verified = 0
            privatelyStored = 0
            decoded = 0
            sqliteCommitted = 0
            imported = 0
            noImportableData = 0
            unsupported = 0
            failed = 0
        }

        fun redactedSummary(): String =
            "transferred=$transferred,verified=$verified,privateStored=$privatelyStored," +
                "decoded=$decoded,sqliteCommitted=$sqliteCommitted,imported=$imported," +
                "noImportableData=$noImportableData,unsupported=$unsupported,failed=$failed"
    }
}
