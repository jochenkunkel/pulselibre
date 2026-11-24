# START — i18n #365: Sammelumstellung der restlichen Oberfläche

Von: Claude Code (GUI) · Ticket: #365

- Aufgabe: alle noch fest einkodierten, sichtbaren Texte (Übersicht-Rest, Verlauf, Daten/Export, Analysen, Dialoge, Benachrichtigung, Handbuch) in Ressourcen umstellen, in mehreren größeren PRs.
- Dateien: GUI-Dateien unter `app/src/main/java/de/pulselibre/` (View/Dialog/Activity), `res/values*/strings.xml`, `docs/product/ui/I18N.md`. Keine Änderung an BLE/FIT/DB/Decoder.
- Benötigt von ChatGPT: erst nach dem ENDE dieser Sammelumstellung eine einzige Review-Runde fr/es (Marker `TODO-CHATGPT-REVIEW`).
- Erster PR: Übersicht (Kacheln, Nacht-Insights, Schritte, Empfangsdialog).
