# BITTE / ÜBERGABE – GUI an ChatGPT: i18n #365, gesammeltes fr/es-Review und Handbuch

- Von: Claude Code (GUI)
- An: ChatGPT
- Ticket: #365
- Bezug: `2026-11-21_195900_chatgpt_i18n-365_ende-teilreview-pr389.md`, `2026-11-21_150846_claude-code_i18n-365_update-sammelpaket.md`
- Status: Sammelumstellung der GUI ist in main (#377, #379, #380–#382, #386). Offen nur noch PR #388 (englisches Handbuch, Entwurf). Danach bitte das **eine** gesammelte Review.

## Umfang
- Dateien: `app/src/main/res/values-fr/strings.xml`, `values-es/strings.xml` (je 768 Schlüssel, Parität mit en/de per `StringResourcesParityTest`), `docs/I18N.md` (Glossar, Umstellungsstand), Handbuch.
- Neu seit deinem Teilreview #389: **459 Schlüssel** je Sprache. Liste: `git diff db365b7 origin/main -- app/src/main/res/values/strings.xml` (nur `+`-Zeilen). Gruppen nach Präfix: `an_` Analysen (59), `tr_` Trend (54), `md_` Methoden/Module (60), `ex_` Daten/Export (70), `dev_`/`gatt_`/`link_`/`bat_`/`dur_` Gerät (66), `unk_` unbekannte FIT-Felder (48), `cust_`/`dd_`/`tl_`/`dq_`/`ma_`/`day_` Dialoge und Tagesansicht (64), `svc_` Dienst-Benachrichtigung (8), `bmi_`/`main_` Hauptansicht (27), `uv_` (2), `action_` (1).
- Marker: je Sprache 9 `TODO-CHATGPT-REVIEW` (Abschnitte, bitte entfernen) und 89 `REVIEW: heikler Text` (bleiben bis Freigabe Projektinhaber; nicht als freigegeben behaupten). Heikel sind u. a. BMI-Einordnung, Hinweis zu Benachrichtigungen, Gerät/Verbindungsstatus, Beispielbytes-Warnung, Backup/Wiederherstellung.
- Handbuch: Quelle `docs/user/BENUTZERHANDBUCH.md` (de), englischer Entwurf `docs/user/USER_MANUAL.en.md` (PR #388). Bitte en gegenlesen und fr/es als `MANUEL_UTILISATEUR.fr.md` / `MANUAL_DE_USUARIO.es.md` in `docs/user/` ergänzen; `FEHLERBEHEBUNG.md` ist ebenfalls nur deutsch. Abbildungen bleiben deutsch.

## Regeln beim Review (unverändert)
- Platzhalter exakt erhalten (`%1$s`, `%2$d`, `%%`), Einheiten `bpm`, `KiB`, `ms`, `kcal` unverändert; Glossar in `docs/I18N.md` (Kachel = tuile / tarjeta).
- Deutsche Modelltitel/-sätze bleiben stabile IDs (DomainText, MetricText) – nur die Ressourcen übersetzen, keine Code-Änderung nötig. Zahlen in übersetzten Modellsätzen bleiben deutsch formatiert (Absicht).
- Lokal vor dem PR: `gradle --offline :app:testDebugUnitTest --tests de.pulselibre.StringResourcesParityTest :app:lintDebug`.
- Bitte einen PR; Rückmeldung als ENDE im Agenten-Chat. Keine S25-Abnahme behaupten.

## Bewusst nicht übersetzt
Testlabor, Diagnoseblatt, Debug-Karte/Scan-Diagnose in `MainActivity`, Logtexte (siehe `docs/I18N.md`).
