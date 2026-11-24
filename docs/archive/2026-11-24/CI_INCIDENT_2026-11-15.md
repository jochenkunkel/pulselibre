# CI incident: workflow jobs fail before executing steps

Date: 2026-11-15.

## Evidence

- Run #122 (commit `33c0382c`) succeeded.
- Runs #123–#129 report `failure` for the `build` job; the dependent emulator job is `skipped`.
- Run #129 (commit `3c30860e`, https://github.com/jochenkunkel/pulselibre/actions/runs/43745718328) was created at 18:15:28Z and marked failed at 18:15:30Z. The GitHub jobs API returned `steps: null` and `logs_url: null`.
- Fetching job logs for prior failures returned GitHub 404 `BlobNotFound`; no build diagnostics artifact was available.

## Assessment

**Likely pre-run infrastructure / account / GitHub Actions scheduling issue**, rather than demonstrated Kotlin compilation failure: a normal Gradle compile cannot be inferred from a two-second job with no recorded steps. This is a hypothesis, not a confirmed root cause. Check GitHub Actions UI for billing/usage limits, account restrictions, repository Actions permissions, runner availability and service incidents.

## Recovery

1. Inspect run #129 in GitHub Actions UI for any banner explaining why the job did not start.
2. Check repository/org Actions permissions and billing/usage if shown.
3. Re-run one failed workflow after the underlying cause is resolved; avoid repeated commits solely to trigger more failing runs.
4. Require successful `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` and API-35 emulator tests.
5. Only then approve installation on the S25. Existing green build #122 does **not** validate commits after #122.

No real CIRQA communication was attempted during this incident.
