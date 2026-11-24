# GitHub Actions: one active workflow run (cost control)

Configured on 2026-11-16 for PulseLibre.

## Goal

Prevent overlapping GitHub Actions runs from consuming unnecessary billed minutes. This applies to **the complete workflow run**, not only the Android APK build: unit tests, lint, emulator/instrumentation tests, and artifact jobs are all part of the same run.

## Implemented configuration

In `.github/workflows/android.yml`, at workflow top level (not inside a job):

```yaml
concurrency:
  # One active run across pushes, PRs, tags and manually launched test suites.
  # Cancels the entire superseded run, including its build, lint and emulator jobs.
  group: pulselibre-single-active-workflow
  cancel-in-progress: true
```

Previously the group depended on workflow and ref, and cancellation excluded release tags and manual dispatch. Those exceptions have been removed. The fixed group now makes runs on different branches, pull requests, tags and manual triggers mutually exclusive **within this repository**.

**Important trade-off:** A new ordinary push can cancel an important manually started full test suite or release build. For projects that require uninterrupted releases, use a separate protected concurrency group for releases instead of copying this strict policy unchanged.

## Policy for development sessions

1. Before making changes, inspect Actions for queued/in-progress runs, including build and test workflows.
2. Batch related source/documentation changes into as few commits as practical.
3. Push changes; the GitHub Actions concurrency policy automatically cancels older runs in this group and retains the latest.
4. Check the latest run and its jobs for success, failure, or cancellation; don't claim an APK is ready before a successful run.
5. If another workflow YAML is added, it must use **the same group string** to participate in repository-wide cancellation. This configuration does not cancel unrelated workflows using different groups.
6. The current GitHub connector can inspect Actions runs but does **not** expose a direct cancel-run operation. Do not claim manual cancellation; rely on GitHub's configured automatic cancellation when a newer run starts.
7. Cancellation is best-effort, not instantaneous; an already completed run cannot be undone. GitHub Actions minutes already consumed are not refunded.

## Reuse in other repositories

At top level of **every** GitHub Actions workflow YAML under `.github/workflows/`, use:

```yaml
concurrency:
  group: single-active-ci
  cancel-in-progress: true
```

Use the same fixed group name in every workflow file in that repository. This limits the repository to one active run in the shared group, across pushes, PRs, manual test workflows and release workflows. Choose a different group per repository only if desired; GitHub concurrency groups are repository-scoped.

**Validation:** Start a long-running workflow, then trigger a new run. Confirm the old run becomes `cancelled` and the latest proceeds. Do not intentionally trigger costly runs solely to validate this on a production project.

## Scope

This configuration cancels GitHub Actions **workflow runs**, including all their jobs. It does not disable workflows, delete artifacts, cancel third-party CI providers, or stop jobs already completed.
