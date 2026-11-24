# License and AGPL provenance review — 2026-11-16

## Decision
The project's original code is offered under Apache License 2.0 (root `LICENSE`). This does **not** relicense any third-party code or waive third-party license obligations.

## Evidence reviewed
- Local source: `GarminMlHandshake.kt`, `GarminMlOffline.kt`, `GarminHistoryFrameStream.kt`, `GarminFrameValidator.kt`, `GarminFrameHeader.kt`, `GarminRealtime.kt`, `GarminHistoryTransportInspector.kt`, `FitFileHeader.kt`, `FitRecordStructure.kt`.
- AGPL-3.0 reference: Gadgetbridge `messages/GFDIMessage.java`, `communicator/v2/CommunicatorV2.java`, `communicator/CobsCoDec.java` from the publicly accessible pschmitt/Gadgetbridge mirror (master, inspected 2026-11-16). Upstream is Freeyourgadget/Gadgetbridge on Codeberg; the GitHub original is archived.
- Additional non-AGPL reference: gwerneckp/garmin-ble `garmin_ble/metrics/heart_rate.py` (read for protocol context; its license still needs independent verification).

## Comparison method and results
For nine local Kotlin protocol/decoder files, comments were removed, source was tokenized, and exact contiguous **eight-token** matches were checked against the three reference Java files. **No eight-token matches were found in eight of nine files.** In `GarminMlOffline.kt`, five overlapping eight-token matches were found in the Garmin BLE service UUID literal, which is a protocol identifier rather than evidence of copied implementation. No copied Java classes, AGPL headers or Java package names were observed in the reviewed local Kotlin files.

**Interpretation:** No evidence of literal protected Gadgetbridge implementation copying was found in this scoped comparison. A protocol format, numeric constant, UUID or interoperability behavior alone does not establish a copyright derivative. However, a token match scan is not a semantic plagiarism analysis and does not prove independent authorship or rule out nonliteral copying.

## Remaining legal and engineering checks
1. Compare *all* source files and historical versions against the current Codeberg upstream and any other actual source used, including tests and documentation. This audit only covered nine local files and three reference files.
2. Establish provenance of `GarminRealtime.kt` and `GarminMlHandshake.kt` from commit history / author notes; these explicitly cite Gadgetbridge and other reference implementations.
3. Check licenses and attributions of garmin-ble, any bundled assets, Gradle dependencies, and Android redistribution notices.
4. If a copyrightable AGPLv3 implementation was copied/adapted, **do not assume Apache 2.0 alone is sufficient**. Obtain permission or independently rewrite the affected implementation with documented clean-room provenance, and seek qualified legal advice.
5. Keep the repository private until the separate health-data Git-history exposure is fully remediated.

## References
- Gadgetbridge: https://codeberg.org/Freeyourgadget/Gadgetbridge
- Public mirror used for source comparison: https://github.com/pschmitt/Gadgetbridge
- Apache-2.0 text: https://www.apache.org/licenses/LICENSE-2.0

## Status
**Preliminary source-level review passed for the examined files; full license compliance NOT certified.**
