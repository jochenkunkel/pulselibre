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
        assertTrue(report.contains("next-transaction-followup-empty"))
    }
}
