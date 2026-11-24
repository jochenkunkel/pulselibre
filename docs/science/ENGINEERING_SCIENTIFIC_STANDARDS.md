# PulseLibre engineering and scientific provenance policy

**Status:** mandatory for new work; existing code requires an audit before claiming full compliance.

## Architecture
- Keep BLE transport/registration, protocol decoding, durable storage, domain models, numerical algorithms, and UI as separate responsibilities.
- Experimental device services are opt-in, allowlisted, bounded, and must not alter overnight collection or pairing.
- Preserve offline-first privacy; never add cloud access, CLOSE_ALL, or destructive changes implicitly.

## Provenance required next to implementations
For every published scientific method, equation, threshold or coefficient used in executable code:
- Cite **authors, year, title, venue, DOI or stable URL** in a nearby KDoc comment.
- State exactly which equation/definition/section is implemented and what was adapted.
- State input signal, units, sampling cadence, artifact rules, missing-data policy, output range, and limitations.
- Do not cite a paper as endorsing weights or thresholds it does not actually specify.
- Add a matching entry to `docs/science/SCIENTIFIC_REFERENCES.md` and unit tests with traceable expected results.

For independently designed PulseLibre formulas:
- Mark `PULSELIBRE EXPERIMENTAL HEURISTIC — NOT CLINICALLY VALIDATED`.
- Record version, rationale, parameters, constraints, and validation plan.
- Never attribute internally chosen weights to a publication.

## Evidence levels
1. **Observed on CIRQA**: actual device logs/validated packet captures.
2. **Implemented and tested locally**: code and automated test results, no device claim.
3. **Reported by third-party Garmin project**: plausible candidate, not CIRQA support.
4. **Hypothesis**: unverified; never silently turn into a measured health metric.

## Sleep and actigraphy
- Stillness alone is not sleep; wakeful immobility is a known limitation of actigraphy.
- Raw accelerometer access on CIRQA remains unverified; step counts are not actigraphy.
- Sleep score and stages remain unavailable until validated estimators exist.
- Distinguish measurement, estimate, and interpretation in data models and UI.

## Review checklist
Before merging a new scientific calculation, confirm source accuracy, dimensional correctness, handling of gaps and invalid data, tests, and plain-language disclosure in the UI.
