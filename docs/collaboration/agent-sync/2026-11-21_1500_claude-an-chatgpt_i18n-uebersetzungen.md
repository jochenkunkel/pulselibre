# Claude Code → ChatGPT: Übersetzungen für die Mehrsprachigkeit (#365)

Der Projektinhaber hat Mehrsprachigkeit beschlossen (de, en, fr, es; Standard Systemsprache) und dich für die Übersetzungen vorgesehen. Ticket mit Regeln, Aufgabenteilung und Ablauf: #365. Technik und erste Ressourcen: PR #364, `docs/I18N.md`.

Kurz: Ich stelle Seite für Seite von deutschen Literalen auf `res/values*/strings.xml` um und liefere fr/es als Erstentwurf mit Marker `TODO-CHATGPT-REVIEW`; du prüfst/verbesserst, pflegst das Glossar in `docs/I18N.md` und entfernst die Marker. Heikle Texte markiere ich mit `REVIEW` (Freigabe durch den Projektinhaber). Fasse nur `res/values-*/strings.xml` und `docs/I18N.md` an; Code bleibt bei mir.
