# #149: begrenzte Produkt-Fortsetzung nach `nextTransactionId`

- Von: OpenCode · An: alle · 2026-11-21 17:32 UTC
- Bezug: No-Pull-Ergebnis in PR #384 und Produkt-Download-Follow-up `CIRQA_PRODUCT_DOWNLOAD_FOLLOWUP_2026-11-21.md`

Die beiden separat bestätigten S25-No-Pull-Proben (Initiallisten 28 bzw. 35 Einträge) erhielten nach je einer `transaction_id`-Folgeanfrage `status=0`, 0 Einträge. Das belegt nicht, dass weitere Historien-Dateien vorhanden sind; es belegt aber die Drahtform und eine erfolgreiche, leere Antwort.

Ich habe den bestehenden Produktpfad nun offline so erweitert, dass er:
1. vorhandene `session_id`-Pagination unverändert und bevorzugt fortsetzt,
2. ohne Session, aber mit `nextTransactionId`, genau eine separate Folgeanfrage im selben nutzerinitiierten Lauf sendet,
3. zurückgelieferte Einträge durch dieselbe UID-/Budget-/MLR-/FIT-/SQLite-Pipeline verarbeitet,
4. danach bei erneutem `nextTransactionId` stoppt und `coverage=device-history-unverified` beibehält; keine automatische Transaktionskette,
5. pending/empty/limit-Zustände unterscheidet und eine kurze Run-ID im Endstatus ausgibt, damit alte identische Ergebnisse nicht als neuer Lauf gelesen werden.

Änderungen betreffen `CirqaMlHandshakeProbe.kt`, den isolierten `CirqaItemListContinuationPolicy`, dessen Unit-Tests sowie Run-ID-Helper/-Tests. Keine DB-Schema-, Decoder-, Testlabor- oder UI-Datei geändert. Full local CI ist erfolgreich; PR-CI/Review stehen noch aus.

Keine neue aktive S25-Ausführung ist erfolgt. Für einen End-to-End-Test des geänderten Produktlaufs wird nach PR-/Main-Full-CI eine eigene, exakt begrenzte Freigabe benötigt; die bereits ausgeführte Produktaktion war mit dem alten Code.
