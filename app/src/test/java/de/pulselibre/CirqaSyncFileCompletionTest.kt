package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaSyncFileCompletionTest {
    private fun fit(
        transfer: CirqaSyncFileCompletion.Stage = CirqaSyncFileCompletion.Stage.SUCCEEDED,
        integrity: CirqaSyncFileCompletion.Stage = CirqaSyncFileCompletion.Stage.SUCCEEDED,
        storage: CirqaSyncFileCompletion.Stage = CirqaSyncFileCompletion.Stage.SUCCEEDED,
        decode: CirqaSyncFileCompletion.Stage = CirqaSyncFileCompletion.Stage.SUCCEEDED,
        sqlite: CirqaSyncFileCompletion.Stage = CirqaSyncFileCompletion.Stage.SUCCEEDED,
        importable: Int? = 1
    ) = CirqaSyncFileCompletion.Result(
        format = CirqaSyncFileCompletion.Format.FIT,
        transfer = transfer,
        integrity = integrity,
        privateStorage = storage,
        decode = decode,
        sqliteCommit = sqlite,
        importableObservations = importable,
        detail = "redacted"
    )

    @Test fun importedFitReportsEveryReachedStage() {
        val result = fit()
        val counts = CirqaSyncFileCompletion.StageCounts()
        counts.record(result)

        assertEquals(CirqaSyncFileCompletion.Status.IMPORTED, result.status)
        assertTrue(result.successfulForSync)
        assertEquals(1, counts.sqliteCommitted)
        assertEquals(1, counts.imported)
        assertEquals(
            "redacted;pipeline=IMPORTED;stages=" +
                "transfer:SUCCEEDED,integrity:SUCCEEDED,privateStorage:SUCCEEDED," +
                "decode:SUCCEEDED,sqliteCommit:SUCCEEDED",
            result.redactedSummary()
        )
    }

    @Test fun noImportableObservationsRemainDistinctFromFailedImport() {
        val result = fit(importable = 0)
        val counts = CirqaSyncFileCompletion.StageCounts()
        counts.record(result)

        assertEquals(CirqaSyncFileCompletion.Status.NO_IMPORTABLE_DATA, result.status)
        assertTrue(result.successfulForSync)
        assertEquals(1, counts.sqliteCommitted)
        assertEquals(0, counts.imported)
        assertEquals(1, counts.noImportableData)
        assertEquals(0, counts.failed)
    }

    @Test fun verifiedRawFileIsNotMisreportedAsDecodedOrImported() {
        val result = CirqaSyncFileCompletion.Result(
            format = CirqaSyncFileCompletion.Format.NON_FIT,
            transfer = CirqaSyncFileCompletion.Stage.SUCCEEDED,
            integrity = CirqaSyncFileCompletion.Stage.SUCCEEDED,
            privateStorage = CirqaSyncFileCompletion.Stage.SUCCEEDED,
            decode = CirqaSyncFileCompletion.Stage.NOT_APPLICABLE,
            sqliteCommit = CirqaSyncFileCompletion.Stage.NOT_APPLICABLE,
            detail = "redacted"
        )
        val counts = CirqaSyncFileCompletion.StageCounts()
        counts.record(result)

        assertEquals(CirqaSyncFileCompletion.Status.VERIFIED, result.status)
        assertTrue(result.successfulForSync)
        assertEquals(1, counts.transferred)
        assertEquals(1, counts.verified)
        assertEquals(1, counts.privatelyStored)
        assertEquals(0, counts.decoded)
        assertEquals(0, counts.sqliteCommitted)
        assertEquals(0, counts.imported)
    }

    @Test fun verifiedUnsupportedFitIsRetainedButNotAnImportedSuccess() {
        val result = fit(
            decode = CirqaSyncFileCompletion.Stage.UNSUPPORTED,
            sqlite = CirqaSyncFileCompletion.Stage.NOT_REACHED,
            importable = null
        )
        val counts = CirqaSyncFileCompletion.StageCounts()
        counts.record(result)

        assertEquals(CirqaSyncFileCompletion.Status.UNSUPPORTED, result.status)
        assertFalse(result.successfulForSync)
        assertEquals(1, counts.verified)
        assertEquals(1, counts.privatelyStored)
        assertEquals(0, counts.decoded)
        assertEquals(1, counts.unsupported)
    }

    @Test fun invalidFitCanBeRetainedForDiagnosisWithoutBeingVerified() {
        val result = fit(
            integrity = CirqaSyncFileCompletion.Stage.FAILED,
            decode = CirqaSyncFileCompletion.Stage.NOT_REACHED,
            sqlite = CirqaSyncFileCompletion.Stage.NOT_REACHED,
            importable = null
        )
        val counts = CirqaSyncFileCompletion.StageCounts()
        counts.record(result)

        assertEquals(CirqaSyncFileCompletion.Status.FAILED, result.status)
        assertFalse(result.successfulForSync)
        assertEquals(1, counts.transferred)
        assertEquals(0, counts.verified)
        assertEquals(1, counts.privatelyStored)
        assertEquals(1, counts.failed)
    }

    @Test fun sqliteFailurePreservesEarlierSuccessfulStagesAndCountsFailure() {
        val result = fit(sqlite = CirqaSyncFileCompletion.Stage.FAILED)
        val counts = CirqaSyncFileCompletion.StageCounts()
        counts.record(result)

        assertEquals(CirqaSyncFileCompletion.Status.FAILED, result.status)
        assertFalse(result.successfulForSync)
        assertEquals(1, counts.transferred)
        assertEquals(1, counts.verified)
        assertEquals(1, counts.privatelyStored)
        assertEquals(1, counts.decoded)
        assertEquals(0, counts.sqliteCommitted)
        assertEquals(0, counts.imported)
        assertEquals(1, counts.failed)
    }

    @Test fun mixedStageTotalsDistinguishPrivateRetentionFromImportCommit() {
        val counts = CirqaSyncFileCompletion.StageCounts()
        var successful = 0
        var failed = 0
        val importFailure = CirqaSyncFileCompletion.FailureDiagnostic(
            CirqaSyncFileCompletion.FailurePhase.FIT_IMPORT_COMMIT,
            "SQLiteException"
        )

        // Synthetic characterization of the redacted S10 report shape:
        // 2 retained non-FIT files, 12 FIT files committed with no importable
        // observations, and 20 FIT import/commit failures. No device identifiers,
        // file references, sizes, or health values are represented here.
        repeat(2) {
            val nonFit = CirqaSyncFileCompletion.Result(
                format = CirqaSyncFileCompletion.Format.NON_FIT,
                transfer = CirqaSyncFileCompletion.Stage.SUCCEEDED,
                integrity = CirqaSyncFileCompletion.Stage.SUCCEEDED,
                privateStorage = CirqaSyncFileCompletion.Stage.SUCCEEDED,
                decode = CirqaSyncFileCompletion.Stage.NOT_APPLICABLE,
                sqliteCommit = CirqaSyncFileCompletion.Stage.NOT_APPLICABLE,
                detail = "synthetic"
            )
            counts.record(nonFit)
            if (nonFit.successfulForSync) successful++ else failed++
        }
        repeat(12) {
            val noImportableData = fit(importable = 0)
            counts.record(noImportableData)
            if (noImportableData.successfulForSync) successful++ else failed++
        }
        repeat(20) {
            val importCommitFailed = fit(sqlite = CirqaSyncFileCompletion.Stage.FAILED)
                .copy(failure = importFailure)
            counts.record(importCommitFailed)
            if (importCommitFailed.successfulForSync) successful++ else failed++
        }

        assertEquals(14, successful)
        assertEquals(20, failed)
        assertEquals(
            "transferred=34,verified=34,privateStored=34,decoded=32," +
                "sqliteCommitted=12,imported=0,noImportableData=12,unsupported=0,failed=20," +
                "failureCauses=FIT_IMPORT_COMMIT/SQLiteException:20",
            counts.redactedSummary()
        )
    }

    @Test fun failureSummaryKeepsOnlyPipelinePhaseAndExceptionClass() {
        val counts = CirqaSyncFileCompletion.StageCounts()
        counts.record(
            fit(sqlite = CirqaSyncFileCompletion.Stage.FAILED).copy(
                detail = "private path and exception message must not be reported",
                failure = CirqaSyncFileCompletion.FailureDiagnostic(
                    CirqaSyncFileCompletion.FailurePhase.FIT_IMPORT_COMMIT,
                    "IllegalStateException"
                )
            )
        )

        assertTrue(counts.redactedSummary().contains(
            "failureCauses=FIT_IMPORT_COMMIT/IllegalStateException:1"
        ))
        assertFalse(counts.redactedSummary().contains("private path"))
        assertFalse(counts.redactedSummary().contains("exception message"))
    }

    @Test fun transferFailureCannotBeHiddenByDownstreamFlags() {
        val result = fit(transfer = CirqaSyncFileCompletion.Stage.FAILED)
        assertEquals(CirqaSyncFileCompletion.Status.FAILED, result.status)
        assertFalse(result.successfulForSync)
    }

    @Test fun resetClearsStageCountersBetweenSyncRuns() {
        val counts = CirqaSyncFileCompletion.StageCounts()
        counts.record(fit())
        counts.reset()

        assertEquals(
            "transferred=0,verified=0,privateStored=0,decoded=0,sqliteCommitted=0," +
                "imported=0,noImportableData=0,unsupported=0,failed=0,failureCauses=none",
            counts.redactedSummary()
        )
    }
}
