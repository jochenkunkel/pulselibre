# FREIGABE – ChatGPT/Projektleitung an GUI-Agent – fr/es (24.11.2026)

**Status: FREIGEGEBEN ZUR WEITERARBEIT.** Die Projektleitung hat ausdrücklich angewiesen: „gib es bitte frei, damit der gui agent weitermachen kann“.

## Umfang der Freigabe

- Der dokumentierte **KI-Sprachreview der 7 A6-43-Schlüssel je Sprache** in `values-fr/strings.xml` und `values-es/strings.xml` ist als abgeschlossen akzeptiert. Siehe `docs/collaboration/agent-sync/2026-11-24_1230_chatgpt-an-gui_fr-es-sprachreview.md`.
- **GUI-Agent darf ohne weiteres Warten** die Arbeit an fr/es, i18n-Integration, GUI und zugehörigen Tests fortsetzen; A6-43-Reviewmarker können in einem getesteten PR auf „KI-sprachlich geprüft“ umgestellt werden.
- Die ausstehende **menschliche Muttersprachler-Prüfung ist kein Blocker für laufende Entwicklung, CI oder interne Testbuilds**. Sie gilt weiterhin als offen und darf nicht als erfolgt behauptet werden.
- **Keine pauschale fachliche Freigabe** für medizinische, datenschutzrechtliche oder gerätebezogene `REVIEW`-Texte; diese Markierungen und die entsprechenden Projektinhaber-Freigaben bleiben bestehen. Keine öffentliche Freigabebehauptung für ungeprüfte Übersetzungen.
- Vor Merge: Ressourcenschlüssel-/Placeholder-Parität, Android-Lint und projektübliche CI-Gates prüfen. Bestehende Rollen: FIT technischer Lead, GUI Owner der Oberflächen-/i18n-Arbeit, CI unterstützt Test und Build.

**Handlungsanweisung an GUI:** Jetzt weiterarbeiten, offene i18n-Aufgaben priorisieren, keine Warteposition wegen „Muttersprachler“; verbleibende echte Freigabegrenzen sichtbar dokumentieren.
