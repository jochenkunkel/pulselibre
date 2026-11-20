package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaSyncFileCompletionTest {
    @Test fun fitRequiresSuccessfulSqliteCommitAfterVerifiedPrivateStorage() {
        assertTrue(CirqaSyncFileCompletion.isSuccessful(
            "fitHeader=true;privateFit=CREATED;observations=1;sqlite=ok:1"
        ))
        assertTrue(CirqaSyncFileCompletion.isSuccessful(
            "fitHeader=true;privateFit=ALREADY_PRESENT;observations=0;sqlite=ok:0"
        ))
        assertFalse(CirqaSyncFileCompletion.isSuccessful(
            "fitHeader=true;privateFit=CREATED;observations=1;sqlite=failed:SQLiteException"
        ))
        assertFalse(CirqaSyncFileCompletion.isSuccessful(
            "fitHeader=true;privateFit=CREATED;decode=unsupported"
        ))
        assertFalse(CirqaSyncFileCompletion.isSuccessful(
            "fitHeader=true;privateSaveFailed=IOException"
        ))
    }

    @Test fun rawFileCompletesOnlyAfterExactSizePrivateStorage() {
        assertTrue(CirqaSyncFileCompletion.isSuccessful(
            "len=100;expected=100;exactSize=true;privateRaw=ALREADY_PRESENT"
        ))
        assertFalse(CirqaSyncFileCompletion.isSuccessful(
            "len=99;expected=100;exactSize=false;privateRaw=CREATED"
        ))
        assertFalse(CirqaSyncFileCompletion.isSuccessful(
            "len=100;expected=100;exactSize=true;privateRawSaveFailed=IOException"
        ))
    }
}
