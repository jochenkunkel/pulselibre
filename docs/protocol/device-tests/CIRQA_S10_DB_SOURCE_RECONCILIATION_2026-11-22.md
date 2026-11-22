# S10-Lauf 2 – Offline-Abgleich der FIT-Quellen (redaktiert)

Stand: 2026-11-22. Ergänzung zum [Ausführungsbericht des begrenzten DB-Import-Tests](CIRQA_S10_BOUNDED_DB_IMPORT_EXECUTION_2026-11-22.md).

## Methode

Der Lauf-2-Export wurde lokal und read-only ausgewertet. Die im Backup enthaltenen
SHA-256-Quellenidentitäten der `canonical_fit_sources` wurden mit lokal berechneten
SHA-256-Werten der sechs verfügbaren Kandidatenpayloads aus der früheren HCI-Auswertung
verglichen. Hashwerte, Dateinamen, UIDs und Nutzdaten werden hier nicht veröffentlicht.

## Ergebnis

- Der Lauf-2-Export enthält **23** FIT-Quellen und **27.736** Observationen mit
  Provenienz. Im Export wurden Observationen **17** Quellenidentitäten zugeordnet;
  für sechs weitere Quellen gibt es in der Provenienztabelle keine Observationen.
  Diese Tabellenzahlen beschreiben Datenbankobjekte und keine Zahl empfangener
  Transfers.
- Der Hashvergleich ergab **0 exakte Treffer** zwischen den 23 Quellenidentitäten
  und den sechs verfügbaren früheren Kandidatenpayloads (fünf FIT, ein Nicht-FIT).
  Daher kann keine dieser sechs Kandidatendateien als byteidentische Quelle im
  Lauf-2-Backup identifiziert werden.
- Die früher dokumentierten **acht FIT-Funde** stammen aus mehreren HCI-Fenstern
  und umfassen App→Band-Daten. Die **sechs finalen Kandidaten** stammen dagegen aus
  dem letzten Fenster und sind Band→App-Transfers. Diese Zahlen haben verschiedene
  Richtungen und Fenster; sie sind weder widersprüchliche Zählungen desselben Satzes
  noch addierbare Mengen.

## Aussagegrenze

Der Abgleich verbindet die 23 Lauf-2-Quellen nicht mit den acht früheren HCI-Funden:
Von den acht liegen nicht alle unabhängigen Kandidatenbytes für einen vollständigen
Identitätsvergleich vor; zudem sind die HCI-Mitschnitte nicht der Lauf-2-Mitschnitt.
Die Nichtübereinstimmung mit den sechs verfügbaren Payloads sagt nur aus, dass diese
Payloads nicht byteidentisch im Lauf-2-Quellenbestand stehen.

Damit bestätigt der Export den **DB-Import während Lauf 2**, aber weder die konkrete
Dateizuordnung zu den früheren Mitschnitten noch die Vollständigkeit der
Gerätehistorie. `coverage=device-history-unverified` bleibt bestehen.

Die Auswertung erfolgte offline, ohne Gerätezugriff. Roh-Payloads, Hashwerte,
Gerätekennungen und Gesundheitswerte bleiben privat.
