#!/usr/bin/env python3
"""One-pass, read-only acceptance run for the Galaxy S25 (collective test of all phone-bound open tickets).

What it does: drives the PulsLibre UI over adb (taps, swipes, screenshots, UI dumps), checks visible texts and
accessibility descriptions, observes the app passively in the background, and writes a Markdown report plus local
screenshots. What it never does: install/uninstall/clear data, run instrumentation tests, start a sync or history
download, write to the band, or change anything permanently (night mode, font scale and display density are restored,
also on errors). Every adb call is checked against an allow-list; taps on risky labels are refused.

Screenshots and the report contain real health values: the output directory must be OUTSIDE the repository.

Usage: python3 tools/s25_acceptance.py --serial RFGL229VWAF [--out-dir DIR] [--phases a,b] [--dry-run]
Plan and ticket mapping: docs/S25_ACCEPTANCE_PLAN_2026-11-20.md
"""
from __future__ import annotations

import argparse
import datetime as dt
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

PACKAGE = "de.pulselibre"
ACTIVITY = f"{PACKAGE}/.MainActivity"

# Only these shell commands may ever reach the phone.
ALLOWED_SHELL = [re.compile(p) for p in (
    r"^input (tap \d+ \d+|swipe \d+ \d+ \d+ \d+ \d+|keyevent \d+)$",
    r"^cmd uimode night( yes| no)?$",
    r"^settings get system font_scale$",
    r"^settings put system font_scale [0-9.]+$",
    r"^wm (size|density)$",
    r"^wm density (reset|\d+)$",
    rf"^am start -n {re.escape(ACTIVITY)}$",
    rf"^dumpsys package {re.escape(PACKAGE)}$",
    rf"^dumpsys activity services {re.escape(PACKAGE)}$",
    r"^dumpsys (window|battery|power)$",
    r"^logcat -d -b crash$",
    r"^uiautomator dump /dev/tty$",
    r"^uiautomator dump /sdcard/s25_ui\.xml$",
    r"^cat /sdcard/s25_ui\.xml$",
    r"^rm /sdcard/s25_ui\.xml$",
    r"^screencap -p$",
)]

# Labels that start device or data actions. The runner never taps a node whose text matches.
FORBIDDEN_TAP = re.compile(
    r"herunterladen|synchron|sync\b|löschen|entkoppeln|zurücksetzen der|reset|teilen|exportieren|"
    r"diagnosepaket|inventur|scan|verbinden|trennen|aufzeichnung (starten|beenden)|v2\d|5007|5050",
    re.IGNORECASE)

TABS = ("Übersicht", "Verlauf", "Analysen", "Gerät", "Daten")
PASS, FAIL, INFO, MANUAL, SKIP = "PASS", "FAIL", "INFO", "MANUAL", "SKIP"


@dataclass
class Check:
    ticket: str
    name: str
    status: str
    detail: str = ""


@dataclass
class Node:
    text: str
    desc: str
    bounds: tuple[int, int, int, int]
    clickable: bool = False

    @property
    def center(self) -> tuple[int, int]:
        left, top, right, bottom = self.bounds
        return (left + right) // 2, (top + bottom) // 2

    @property
    def label(self) -> str:
        return self.text or self.desc


class AdbError(RuntimeError):
    pass


class Adb:
    """Thin adb wrapper bound to one serial; `runner` is injectable so the logic can be tested without a phone."""

    def __init__(self, serial: str, runner=None, sleep=time.sleep):
        if not serial:
            raise AdbError("A serial is required (several devices may be attached).")
        self.serial = serial
        self._runner = runner or self._subprocess
        self.sleep = sleep

    def _subprocess(self, args: list[str], timeout: int) -> bytes:
        done = subprocess.run(["adb", "-s", self.serial, *args], capture_output=True, timeout=timeout)
        if done.returncode != 0:
            raise AdbError(f"adb {' '.join(args)} failed: {done.stderr.decode(errors='replace')[:200]}")
        return done.stdout

    def shell(self, command: str, timeout: int = 60) -> str:
        if not any(p.match(command) for p in ALLOWED_SHELL):
            raise AdbError(f"Refused (not on the allow-list): {command}")
        return self._runner(["shell", command], timeout).decode(errors="replace")

    def exec_out(self, command: str, timeout: int = 60) -> bytes:
        if not any(p.match(command) for p in ALLOWED_SHELL):
            raise AdbError(f"Refused (not on the allow-list): {command}")
        return self._runner(["exec-out", command], timeout)


def parse_nodes(xml_text: str) -> list[Node]:
    start = xml_text.find("<?xml")
    end = xml_text.rfind("</hierarchy>")
    if start < 0 or end < 0:
        return []
    root = ET.fromstring(xml_text[start:end + len("</hierarchy>")])
    nodes: list[Node] = []
    for item in root.iter("node"):
        m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", item.get("bounds", ""))
        if not m:
            continue
        nodes.append(Node(item.get("text", ""), item.get("content-desc", ""), tuple(int(g) for g in m.groups()),
                          item.get("clickable") == "true"))
    return nodes


def find(nodes: list[Node], needle: str, exact: bool = False) -> Node | None:
    for n in nodes:
        label = n.label
        if (label == needle) if exact else (needle in label or needle in n.desc):
            return n
    return None


def overlapping_text_nodes(nodes: list[Node], min_share: float = 0.25) -> list[tuple[str, str]]:
    """Pairs of text nodes whose boxes overlap by more than `min_share` of the smaller one (clipping heuristic)."""
    texts = [n for n in nodes if n.text.strip()]
    pairs: list[tuple[str, str]] = []
    for i, a in enumerate(texts):
        for b in texts[i + 1:]:
            ix = min(a.bounds[2], b.bounds[2]) - max(a.bounds[0], b.bounds[0])
            iy = min(a.bounds[3], b.bounds[3]) - max(a.bounds[1], b.bounds[1])
            if ix <= 0 or iy <= 0:
                continue
            small = min((a.bounds[2] - a.bounds[0]) * (a.bounds[3] - a.bounds[1]),
                        (b.bounds[2] - b.bounds[0]) * (b.bounds[3] - b.bounds[1]))
            if small > 0 and ix * iy / small > min_share:
                pairs.append((a.text, b.text))
    return pairs


AXIS_WITH_DATES = re.compile(r"Horizontale Achse \d{2}\.\d{2}\. \d{2}:\d{2} bis \d{2}\.\d{2}\. \d{2}:\d{2}")


def coverage_texts_agree(nodes: list[Node]) -> tuple[bool, str]:
    """#312: the night tile and the reason line must show the same coverage percent."""
    tile = re.search(r"Abdeckung: (\d+(?:,\d+)?) %", " ".join(n.desc for n in nodes))
    reason = re.search(r"Abdeckung (\d+(?:,\d+)?) % unter", " ".join(n.text for n in nodes))
    if not tile or not reason:
        return True, "Kachel oder Begründung nicht sichtbar (kein Widerspruch prüfbar)"
    return tile.group(1) == reason.group(1), f"Kachel {tile.group(1)} % / Begründung {reason.group(1)} %"


class Run:
    def __init__(self, adb: Adb, out_dir: Path, dry: bool = False):
        self.adb, self.out, self.dry = adb, out_dir, dry
        self.checks: list[Check] = []
        self.shots: list[str] = []
        self.size = (1080, 2340)
        self.original: dict[str, str] = {}

    # ---- primitives -------------------------------------------------------------------------------------------
    def add(self, ticket: str, name: str, status: str, detail: str = "") -> None:
        self.checks.append(Check(ticket, name, status, detail))
        print(f"[{status}] {ticket} {name}" + (f" – {detail}" if detail else ""))

    def dump(self) -> list[Node]:
        nodes = parse_nodes(self.adb.exec_out("uiautomator dump /dev/tty").decode(errors="replace"))
        if nodes:
            return nodes
        # Fallback for devices where the tty dump stays empty: dump to a file, read it, delete it again.
        self.adb.shell("uiautomator dump /sdcard/s25_ui.xml")
        raw = self.adb.shell("cat /sdcard/s25_ui.xml")
        self.adb.shell("rm /sdcard/s25_ui.xml")
        return parse_nodes(raw)

    def shot(self, name: str) -> None:
        path = self.out / f"{name}.png"
        path.write_bytes(self.adb.exec_out("screencap -p"))
        self.shots.append(path.name)

    def tap_node(self, node: Node) -> None:
        if FORBIDDEN_TAP.search(node.label):
            raise AdbError(f"Refused to tap risky label: {node.label!r}")
        x, y = node.center
        self.adb.shell(f"input tap {x} {y}")
        self.adb.sleep(1.5)

    def tap_text(self, needle: str, exact: bool = False) -> bool:
        node = find(self.dump(), needle, exact)
        if not node:
            return False
        self.tap_node(node)
        return True

    def swipe(self, up: bool = True) -> None:
        # Slow, short drag: a fast swipe starts a fling that carries the page far past the target (seen on the S25).
        w, h = self.size
        y1, y2 = (int(h * .70), int(h * .42)) if up else (int(h * .42), int(h * .70))
        self.adb.shell(f"input swipe {w // 2} {y1} {w // 2} {y2} 900")
        self.adb.sleep(.8)

    def scan_page(self, steps: int = 16) -> list[Node]:
        """One pass from the top to the bottom; returns the union of every node seen (nothing can be skipped)."""
        self.scroll_top()
        seen: dict[tuple, Node] = {}
        previous = None
        for _ in range(steps):
            nodes = self.dump()
            for n in nodes:
                seen[(n.label, n.bounds[0], n.bounds[2])] = n
            signature = tuple(n.label for n in nodes)
            if signature == previous:   # the page did not move: bottom reached
                break
            previous = signature
            self.swipe(True)
        return list(seen.values())

    def scroll_to(self, needle: str, limit: int = 14, up: bool = True) -> Node | None:
        for _ in range(limit):
            node = find(self.dump(), needle)
            if node and node.center[1] < self.size[1] * .86:
                return node
            self.swipe(up)
        return None

    def scroll_top(self) -> None:
        for _ in range(10):
            self.swipe(up=False)

    def tab(self, name: str) -> bool:
        nodes = [n for n in self.dump() if n.label == name]
        if not nodes:
            return False
        self.tap_node(max(nodes, key=lambda n: n.center[1]))  # the bottom navigation, not a heading
        return True

    def back(self) -> None:
        self.adb.shell("input keyevent 4")
        self.adb.sleep(1)

    def wake(self) -> None:
        self.adb.shell("input keyevent 224")
        self.adb.sleep(.5)

    def crash_free(self, ticket: str, where: str) -> None:
        log = self.adb.shell("logcat -d -b crash")
        crashed = PACKAGE in log
        self.add(ticket, f"Kein App-Absturz ({where})", FAIL if crashed else PASS, "logcat -b crash enthält die App" if crashed else "")

    # ---- environment ---------------------------------------------------------------------------------------------
    def remember_and_restore_setup(self) -> None:
        self.original = {
            "night": "yes" if "yes" in self.adb.shell("cmd uimode night") else "no",
            "font": self.adb.shell("settings get system font_scale").strip(),
            "density": self.adb.shell("wm density"),
        }

    def restore(self) -> None:
        errors = []
        for label, command in (("Dichte", "wm density reset"),
                               ("Schrift", f"settings put system font_scale {self.original.get('font') or '1.0'}"),
                               ("Nachtmodus", f"cmd uimode night {self.original.get('night', 'yes')}")):
            try:
                self.adb.shell(command)
            except Exception as exc:  # restore must try everything
                errors.append(f"{label}: {exc}")
        self.add("Umgebung", "Systemeinstellungen wiederhergestellt (Dichte, Schrift, Nachtmodus)",
                 FAIL if errors else PASS, "; ".join(errors))


def phase_preflight(run: Run) -> bool:
    a = run.adb
    pkg = a.shell(f"dumpsys package {PACKAGE}")
    version = re.search(r"versionName=(\S+)", pkg)
    run.add("Umgebung", "App installiert", PASS if version else FAIL, version.group(1) if version else "nicht gefunden")
    flags = re.search(r"\n\s*flags=\[([^\]]*)\]", pkg)
    run.add("#6", "Release-Build nicht debuggbar, kein Backup-Flag", PASS if flags and "DEBUGGABLE" not in flags.group(1)
            and "ALLOW_BACKUP" not in flags.group(1) else FAIL, flags.group(1).strip() if flags else "flags nicht lesbar")
    perms = set(re.findall(r"android\.permission\.[A-Z_]+", pkg.split("requested permissions:")[-1].split("install permissions:")[0]))
    forbidden = sorted(p for p in perms if p in {"android.permission.INTERNET", "android.permission.ACCESS_FINE_LOCATION",
                                                 "android.permission.READ_EXTERNAL_STORAGE"})
    run.add("#6", "Keine INTERNET-/Standort-/Speicher-Berechtigung angefordert", FAIL if forbidden else PASS, ", ".join(forbidden))
    power = a.shell("dumpsys power")
    awake = "mWakefulness=Awake" in power
    window = a.shell("dumpsys window")
    locked = "isKeyguardShowing=true" in window or "mShowingLockscreen=true" in window
    run.add("Umgebung", "Bildschirm an und entsperrt", PASS if awake and not locked else FAIL,
            "Bitte Handy entsperren und Display-Timeout verlängern" if not (awake and not locked) else "")
    battery = a.shell("dumpsys battery")
    level = re.search(r"level: (\d+)", battery)
    plugged = re.search(r"(AC|USB|Wireless) powered: true", battery)
    run.add("Umgebung", "Akkustand/Laden (Info für spätere Akku-Aussagen)", INFO,
            f"{level.group(1) if level else '?'} %, {'lädt' if plugged else 'nicht am Ladegerät'}")
    size = re.search(r"Physical size: (\d+)x(\d+)", a.shell("wm size"))
    if size:
        run.size = (int(size.group(1)), int(size.group(2)))
    run.remember_and_restore_setup()
    return bool(version) and awake and not locked


def phase_navigation(run: Run) -> None:
    run.wake()
    run.adb.shell(f"am start -n {ACTIVITY}")
    run.adb.sleep(2.5)
    for tab in TABS:
        ok = run.tab(tab)
        run.add("Navigation", f"Tab „{tab}“ erreichbar", PASS if ok else FAIL)
    run.crash_free("Navigation", "nach Tab-Durchlauf")


def phase_overview(run: Run) -> None:
    run.tab("Übersicht"); run.scroll_top()
    run.shot("uebersicht_oben")
    page = run.scan_page()
    titles = {n.text for n in page}
    for needle in ("Herzfrequenz", "SpO₂", "Schlaf", "Nacht-Insights", "Schritte heute"):
        run.add("Übersicht", f"Kachel „{needle}“ sichtbar", PASS if needle in titles else FAIL)
    ok, detail = coverage_texts_agree(page)
    run.add("#312", "Nacht-Insights: Abdeckung in Kachel und Begründung gleich", PASS if ok else FAIL, detail)
    run.shot("uebersicht_mitte")


def phase_history(run: Run) -> None:
    run.tab("Verlauf"); run.scroll_top()
    for window in ("24 Stunden", "7 Tage", "30 Tage"):
        ok = run.tap_text(window, exact=True)
        run.add("Verlauf", f"Zeitfenster „{window}“ wählbar", PASS if ok else FAIL)
    run.tap_text("24 Stunden", exact=True)
    nodes = run.dump()
    axis = next((n.desc for n in nodes if "Horizontale Achse" in n.desc), "")
    run.add("#309", "Screenreader-Achse nennt Datum und Uhrzeit (24 h)", PASS if AXIS_WITH_DATES.search(axis) else FAIL, axis[-80:])
    if run.tap_text("Verteilung", exact=True):
        desc = next((n.desc for n in run.dump() if n.desc.startswith("Verteilungsdiagramm")), "")
        run.add("#6", "Verteilungsdiagramm hat Screenreader-Zusammenfassung",
                PASS if "Zeitfenstern" in desc or "keine Messwerte" in desc else FAIL, desc[:90])
        run.tap_text("Punkte", exact=True)
    run.shot("verlauf")


def phase_analysis(run: Run) -> None:
    run.tab("Analysen"); run.scroll_top()
    nodes = run.dump()
    for needle in ("Analysen", "Noch nicht verfügbar"):
        run.add("#41", f"Analysen zeigt „{needle}“", PASS if find(nodes, needle) else FAIL)
    run.shot("analysen_oben")
    node = run.scroll_to("Aktivitäts-Atlas", limit=10)
    run.add("#41", "Aktivitäts-Atlas auffindbar (Screenshot lokal zur Sichtprüfung)", PASS if node else INFO,
            "" if node else "nicht sichtbar – evtl. mangels Daten")
    if node:
        run.shot("analysen_atlas")


def phase_home_customize(run: Run) -> None:
    run.tab("Übersicht")
    node = run.scroll_to("Startseite anpassen", limit=16)
    if not node:
        run.add("#267", "Startseite anpassen erreichbar", FAIL)
        return
    run.tap_node(node)
    nodes = run.dump()
    run.add("#267", "Dialog zeigt Kacheln mit Favorit/Hoch/Runter", PASS if find(nodes, "Favorit") and find(nodes, "Hoch") else FAIL)
    run.shot("anpassen_dialog")
    spo2 = next((n for n in nodes if n.text.startswith("SpO")), None)
    if spo2:
        run.adb.shell(f"input tap {spo2.center[0] - 400} {spo2.center[1]}")  # its checkbox, left of the label
        save = find(run.dump(), "Speichern")
        if save:
            run.tap_node(save)
        run.tab("Übersicht"); run.scroll_top()
        gone = not any(n.text.startswith("SpO") for n in run.dump())
        run.add("#267", "SpO₂ ausblenden wirkt", PASS if gone else FAIL)
        node = run.scroll_to("Startseite anpassen", limit=16)
        if node:
            run.tap_node(node)
            reset = find(run.dump(), "Zurücksetzen")   # the dialog's own reset (restores the default layout)
            if reset:
                run.adb.shell(f"input tap {reset.center[0]} {reset.center[1]}")
                run.adb.sleep(1.5)
        run.scroll_top()
        back = any(n.text.startswith("SpO") for n in run.dump())
        run.add("#267", "Zurücksetzen stellt den Standard wieder her", PASS if back else FAIL)
    else:
        run.back()
        run.add("#267", "SpO₂-Zeile im Dialog gefunden", FAIL)


def phase_device(run: Run) -> None:
    run.tab("Gerät"); run.scroll_top()
    nodes = run.dump()
    run.add("#159", "Geräteseite zeigt Verbindungsstatus", PASS if find(nodes, "Verbunden") or find(nodes, "Nicht verbunden") else FAIL,
            next((n.text for n in nodes if "GATT" in n.text), ""))
    run.add("#159", "Batteriestand der CIRQA sichtbar (Info; hängt an OpenCode-Quelle)", INFO,
            next((n.text for n in nodes if re.search(r"Akku|Batterie", n.text)), "nicht angezeigt"))
    run.shot("geraet")
    # #172: hidden entry, 3 s long press on the device heading opens the read-only Testlabor sheet.
    heading = find(run.dump(), "Gerät · CIRQA", exact=True)
    if heading:
        x, y = heading.center
        run.adb.shell(f"input swipe {x} {y} {x} {y} 3400")
        run.adb.sleep(2)
        opened = find(run.dump(), "Testlabor") is not None
        run.add("#172", "Versteckter Einstieg (3 s Druck) öffnet das Testlabor", PASS if opened else FAIL)
        if opened:
            run.shot("testlabor_sheet")
            run.back()
    else:
        run.add("#172", "Überschrift „Gerät · CIRQA“ gefunden", FAIL)
    # #204: unknown-field hint and the warned byte preview. Raw bytes are never stored (no dump/shot of the result).
    hint = run.scroll_to("Unbekannte Datenfelder", limit=12)
    if not hint:
        run.add("#204", "Hinweis auf unbekannte Datenfelder", INFO, "nicht sichtbar (keine Funde oder Inventur nicht gelaufen)")
        return
    run.tap_node(hint)
    run.shot("unbekannte_felder_liste")
    group = next((n for n in run.dump() if n.text.startswith("▸")), None)
    if group:
        run.tap_node(group)
    if run.tap_text("Beispielbytes ansehen"):
        nodes = run.dump()
        warned = find(nodes, "sensible Gesundheits")
        run.add("#204", "Beispielbytes: Warnung erscheint vor jeder Anzeige", PASS if warned else FAIL)
        cancel = find(nodes, "Abbrechen", exact=True)
        if cancel:
            run.tap_node(cancel)
        run.add("#204", "Abbrechen zeigt keine Rohbytes",
                PASS if not any(re.search(r"\b[0-9A-F]{2}( [0-9A-F]{2}){3,}\b", n.text) for n in run.dump()) else FAIL)
    else:
        run.add("#204", "„Beispielbytes ansehen“ in der Feldliste", INFO, "kein Eintrag aufklappbar")
    run.back()


def phase_display_variants(run: Run) -> None:
    variants = (
        ("hell", "cmd uimode night no", None, None),
        ("schrift13", "cmd uimode night yes", "1.3", None),
        ("schrift20", None, "2.0", None),
        ("breite320", None, "1.0", "wm density 540"),
    )
    for name, night, font, density in variants:
        if night:
            run.adb.shell(night)
        if font:
            run.adb.shell(f"settings put system font_scale {font}")
        if density:
            run.adb.shell(density)
        run.adb.sleep(2.5)
        for tab in ("Übersicht", "Verlauf", "Analysen", "Gerät"):
            if not run.tab(tab):
                run.add("#276", f"Variante {name}: Tab {tab} erreichbar", FAIL)
                continue
            run.scroll_top()
            run.shot(f"{name}_{tab.lower().replace('ü', 'ue')}")
            overlaps = overlapping_text_nodes(run.dump())
            # A heuristic can false-alarm (e.g. layered decoration), so a hit is an INFO that asks for the screenshot.
            run.add("#276", f"Variante {name}, {tab}: keine überlappenden Texte (Heuristik)", PASS if not overlaps else INFO,
                    "Screenshot prüfen: " + "; ".join(f"{a[:25]}|{b[:25]}" for a, b in overlaps[:3]) if overlaps else "")
        run.crash_free("#276", f"Variante {name}")
    # restore happens in `finally` of main(); also here so later phases run in the default look
    run.adb.shell("wm density reset")
    run.adb.shell(f"settings put system font_scale {run.original.get('font') or '1.0'}")
    run.adb.shell(f"cmd uimode night {run.original.get('night', 'yes')}")
    run.adb.sleep(2)


def phase_background(run: Run, seconds: int = 90) -> None:
    """#171/#25/#3: passive observation of the app in the background (no actions, no BLE command from here)."""
    run.adb.shell("input keyevent 3")  # HOME
    run.adb.sleep(seconds)
    services = run.adb.shell(f"dumpsys activity services {PACKAGE}")
    foreground = "isForeground=true" in services
    run.add("#25/#3", f"Dienststatus nach {seconds} s im Hintergrund (Info)", INFO,
            "Vordergrunddienst läuft" if foreground else "kein Vordergrunddienst aktiv")
    run.crash_free("#3", "nach Hintergrundphase")
    run.adb.shell(f"am start -n {ACTIVITY}")
    run.adb.sleep(2)


def manual_items() -> list[Check]:
    return [
        Check("#204", "Neue Dialoge (Feldliste, Beispielbytes-Warnung) mit TalkBack durchgehen; KEINE Screenshots der Rohbytes", MANUAL),
        Check("#204", "Beispielbytes bewusst einmal anzeigen und prüfen, dass nur „Datei n“ (kein Hash/Name) erscheint", MANUAL),
        Check("#41", "Fitness-Ansicht (Ruhepuls-Trend, Atlas) inhaltlich plausibel – nur Nutzer kennt die Realität", MANUAL),
        Check("#159/#172", "Wunsch-Gestaltung der Geräteseite (Batterie, Status) – visuelle Entscheidung", MANUAL),
        Check("Optik", "Screenshots im Ausgabeordner ansehen (Abschneiden, Kontrast) – Heuristik ersetzt das nicht", MANUAL),
    ]


PHASES = {
    "preflight": phase_preflight, "navigation": phase_navigation, "overview": phase_overview,
    "history": phase_history, "analysis": phase_analysis, "customize": phase_home_customize,
    "device": phase_device, "variants": phase_display_variants, "background": phase_background,
}


def write_report(run: Run, started: dt.datetime, path: Path) -> None:
    counts = {s: sum(c.status == s for c in run.checks) for s in (PASS, FAIL, INFO, MANUAL, SKIP)}
    lines = [f"# S25-Abnahmelauf {started:%Y-%m-%d %H:%M}", "",
             f"Gerät: `{run.adb.serial}` · PASS {counts[PASS]} · FAIL {counts[FAIL]} · INFO {counts[INFO]} · MANUELL {counts[MANUAL]}",
             "", "**Enthält echte Gesundheitswerte (Screenshots im selben Ordner) – nicht in das Repository einchecken.**", "",
             "| Status | Ticket | Prüfung | Detail |", "| --- | --- | --- | --- |"]
    lines += [f"| {c.status} | {c.ticket} | {c.name} | {c.detail.replace('|', '/')} |" for c in run.checks]
    lines += ["", "## Screenshots", *[f"- {s}" for s in run.shots]]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main(argv: list[str] | None = None, adb: Adb | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--serial", required=True, help="adb serial of the S25 (never run against an unnamed device)")
    parser.add_argument("--out-dir", type=Path, default=Path.home() / "s25-acceptance" / dt.datetime.now().strftime("%Y%m%d-%H%M%S"))
    parser.add_argument("--phases", default=",".join(PHASES), help="comma list, default: all")
    parser.add_argument("--background-seconds", type=int, default=90)
    parser.add_argument("--dry-run", action="store_true", help="print the plan and exit without touching the phone")
    args = parser.parse_args(argv)
    names = [p.strip() for p in args.phases.split(",") if p.strip()]
    unknown = [p for p in names if p not in PHASES]
    if unknown:
        parser.error(f"unknown phases: {unknown}")
    if args.dry_run:
        print("Plan:", " → ".join(names))
        return 0
    for parent in [args.out_dir.resolve(), *args.out_dir.resolve().parents]:
        if (parent / ".git").exists():
            parser.error("--out-dir must be outside any Git repository (screenshots contain health data)")
    args.out_dir.mkdir(parents=True, exist_ok=True)
    started = dt.datetime.now()
    run = Run(adb or Adb(args.serial), args.out_dir)
    try:
        ready = phase_preflight(run) if "preflight" in names else True
        if not ready:
            run.add("Umgebung", "Abbruch: Voraussetzungen nicht erfüllt", FAIL, "Rest wird nicht ausgeführt")
        else:
            for name in names:
                if name == "preflight":
                    continue
                try:
                    if name == "background":
                        phase_background(run, args.background_seconds)
                    else:
                        PHASES[name](run)
                except Exception as exc:
                    run.add("Ablauf", f"Phase {name} abgebrochen", FAIL, str(exc)[:200])
    finally:
        try:
            run.restore()
        except Exception as exc:
            print(f"Wiederherstellung fehlgeschlagen: {exc}", file=sys.stderr)
        run.checks += manual_items()
        write_report(run, started, args.out_dir / "report.md")
    print(f"Bericht: {args.out_dir / 'report.md'}")
    return 1 if any(c.status == FAIL for c in run.checks) else 0


if __name__ == "__main__":
    sys.exit(main())
