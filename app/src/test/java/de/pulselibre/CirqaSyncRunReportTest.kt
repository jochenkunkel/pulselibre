package de.pulselibre

import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaSyncRunReportTest {
    @Test fun fileStageTotalsPrecedeLongItemListDetails() {
        val report = CirqaSyncRunReport.format(
            CirqaSyncRunReport.Overview(
                runIdentity = "run=synthetic",
                message = "bounded sync",
                runDrained = false,
                coverage = "coverage=device-history-unverified",
                incompleteReason = "next-transaction-followup-empty",
                accepted = true,
                itemListSent = true,
                pullSent = true,
                fileSummary = "successful=14,notSuccessful=0,stages=[transferred=14,verified=14," +
                    "privateStored=14,decoded=13,sqliteCommitted=13,imported=8,noImportableData=5]," +
                    "queued=0,transactionFollowups=3,transactionFollowupResponseItems=0," +
                    "selected=14,selectedBytes=53425,skipped=20,pages=4,currentBytes=0",
                terminalEvidence = CirqaSyncTerminalEvidence.Snapshot(
                    terminalCause = "next-transaction-followup-empty",
                    listResponses = 4,
                    listedItems = 34,
                    sessionPageRequestsQueued = 0,
                    transactionFollowupsQueued = 3,
                    lastTransactionResponseItems = 0,
                    selectedItems = 14,
                    pullRequestsQueued = 14,
                    completedItems = 14,
                    failedItems = 0,
                    pendingQueueItems = 0,
                    currentItemPresent = false,
                    currentExpectedBytes = 0,
                    currentReceivedBytes = 0,
                    sessionCursorPresent = false,
                    transactionTokenPresent = true,
                    continuationRequestPending = "none"
                ).format(),
                itemSummary = "x".repeat(2400),
                diagnostics = "frames=synthetic; proto=synthetic"
            )
        )

        val fileTotals = report.indexOf("files=[")
        val itemDetails = report.indexOf("items=[")
        assertTrue(fileTotals >= 0)
        assertTrue(itemDetails > fileTotals)
        assertTrue(report.substring(0, itemDetails).contains("sqliteCommitted=13"))
        assertTrue(report.substring(0, itemDetails).contains("selectedBytes=53425"))
        assertTrue(report.substring(0, itemDetails).contains("skipped=20"))
        assertTrue(report.substring(0, itemDetails).contains("listed=34"))
        assertTrue(report.substring(0, itemDetails).contains("transactionTokenPresent=true"))
        assertTrue(report.contains("next-transaction-followup-empty"))
    }

    @Test fun timeoutReportDistinguishesQueuedFileFromInFlightFileAndContinuation() {
        val queued = CirqaSyncTerminalEvidence.Snapshot(
            terminalCause = CirqaSyncTerminalEvidence.cause(
                "120-s-Zeitlimit; transport acknowledgement only", "transfer-not-drained", false
            ),
            listResponses = 1,
            listedItems = 37,
            sessionPageRequestsQueued = 0,
            transactionFollowupsQueued = 0,
            lastTransactionResponseItems = null,
            selectedItems = 37,
            pullRequestsQueued = 36,
            completedItems = 36,
            failedItems = 0,
            pendingQueueItems = 1,
            currentItemPresent = false,
            currentExpectedBytes = 0,
            currentReceivedBytes = 0,
            sessionCursorPresent = false,
            transactionTokenPresent = true,
            continuationRequestPending = "none"
        ).format()

        assertTrue(queued.contains("cause=session-time-limit"))
        assertTrue(queued.contains("listed=37"))
        assertTrue(queued.contains("selected=37,pullRequestsQueued=36,completed=36,failed=0"))
        assertTrue(queued.contains("pendingQueue=1,currentItem=false"))
        assertTrue(queued.contains("sessionCursorPresent=false,transactionTokenPresent=true"))
    }

    @Test fun terminalEvidenceIdentifiesAnOutstandingSessionRequestSeparately() {
        val evidence = CirqaSyncTerminalEvidence.Snapshot(
            terminalCause = "session-time-limit",
            listResponses = 2,
            listedItems = 37,
            sessionPageRequestsQueued = 1,
            transactionFollowupsQueued = 0,
            lastTransactionResponseItems = null,
            selectedItems = 0,
            pullRequestsQueued = 0,
            completedItems = 0,
            failedItems = 0,
            pendingQueueItems = 0,
            currentItemPresent = false,
            currentExpectedBytes = 0,
            currentReceivedBytes = 0,
            sessionCursorPresent = false,
            transactionTokenPresent = false,
            continuationRequestPending = "session"
        ).format()

        assertTrue(evidence.contains("continuationRequestPending=session"))
        assertTrue(evidence.contains("sessionPageRequestsQueued=1,transactionFollowupsQueued=0"))
        assertTrue(evidence.contains("transactionTokenPresent=false"))
    }
}
