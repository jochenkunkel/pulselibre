#!/usr/bin/env python3
"""#707: Nachbarschafts- und Strukturtests fuer die proprietaere FIT-Nachricht 233 (Feld 2, 4 Bytes).

Nur lesend, nur Zaehlungen: es werden keine Rohwerte, Dateinamen oder Hashes ausgegeben, damit die Ausgabe ins
Ticket darf. Tag = Byte 3 von Feld 2 (beobachtete Kennung, keine bestaetigte Semantik).

  python3 -I tools/fit_msg233_neighbourhood.py ordner_mit_fit_dateien [--seed 1] [--perm 2000]

Tests:
 1. C8-Nachbarschaft: Anteil der Tag-C8-Eintraege, deren naechste Nicht-233-Nachricht 21 (event) oder 227 ist, gegen
    alle anderen Tags derselben Datei; Permutationstest (Tags innerhalb jeder Datei gemischt).
 2. E0/D0: Anzahl Payload-Varianten und je Byteposition die Zahl verschiedener Werte (feste Struktur / Laengenfeld?).
 3. Tag-Varianten (20/23/24, 30/33) je Dateityp aus file_id (msg 0 Feld 0).
"""
import os, sys, random, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from fit_field_survey import parse  # noqa: E402

NEIGHBOURS = {21, 227}
FILE_TYPES = {4: 'activity', 32: 'monitoring_b', 15: 'monitoring_a', 49: 'sleep', 44: 'segment'}


def stream(path):
    """Ordered (global, tag, payload) per message; tag/payload only for 233 with a 4-byte field 2."""
    out, ftype = [], None
    for g, _e, rec, _ts in parse(path):
        if g == 0 and 0 in rec and ftype is None:
            ftype = rec[0][2][0]
        if g == 233 and 2 in rec and rec[2][1] == 4:
            raw = rec[2][2]
            out.append((g, raw[3], raw))
        else:
            out.append((g, None, None))
    return out, ftype


def next_other(msgs, i):
    for j in range(i + 1, len(msgs)):
        if msgs[j][0] != 233:
            return msgs[j][0]
    return None


def c8_hits(files):
    """Per file: list of (tag, next-is-neighbour) for every 233 entry."""
    per = []
    for msgs, _ in files:
        per.append([(t, next_other(msgs, i) in NEIGHBOURS) for i, (g, t, _) in enumerate(msgs) if g == 233 and t is not None])
    return per


def c8_stat(per):
    c8 = [h for f in per for t, h in f if t == 0xC8]
    other = [h for f in per for t, h in f if t != 0xC8]
    rate = lambda xs: (sum(xs) / len(xs)) if xs else 0.0
    return len(c8), rate(c8), len(other), rate(other)


def permutation_p(per, perm, seed):
    _, observed, _, _ = c8_stat(per)
    rnd = random.Random(seed)
    ge = 0
    for _ in range(perm):
        shuffled = []
        for f in per:
            tags = [t for t, _ in f]; rnd.shuffle(tags)
            shuffled.append([(t, h) for t, (_, h) in zip(tags, f)])
        if c8_stat(shuffled)[1] >= observed:
            ge += 1
    return (ge + 1) / (perm + 1)


def structure(files, tag):
    payloads = [p for msgs, _ in files for g, t, p in msgs if t == tag]
    per_pos = [len({p[k] for p in payloads}) for k in range(3)] if payloads else []
    return len(payloads), len(set(payloads)), per_pos


def variants(files):
    table = collections.defaultdict(collections.Counter)
    for msgs, ftype in files:
        name = FILE_TYPES.get(ftype, 'type_%s' % ftype)
        for g, t, _ in msgs:
            if t in (0x20, 0x23, 0x24, 0x30, 0x33):
                table[name]['%02X' % t] += 1
    return table


def main(argv):
    root = argv[1]
    seed = int(argv[argv.index('--seed') + 1]) if '--seed' in argv else 1
    perm = int(argv[argv.index('--perm') + 1]) if '--perm' in argv else 2000
    files = []
    for f in sorted(os.listdir(root)):
        if f.lower().endswith('.fit'):
            try:
                files.append(stream(os.path.join(root, f)))
            except Exception:
                pass
    files = [x for x in files if any(g == 233 for g, _, _ in x[0])]
    print('Dateien mit 233:', len(files))
    per = c8_hits(files)
    n8, r8, no, ro = c8_stat(per)
    print('1. C8 -> naechste Nachricht 21/227: C8 %d Eintraege, Anteil %.3f; andere Tags %d, Anteil %.3f; p=%.4f (%d Permutationen)'
          % (n8, r8, no, ro, permutation_p(per, perm, seed) if n8 else 1.0, perm))
    for tag in (0xE0, 0xD0):
        n, distinct, pos = structure(files, tag)
        print('2. %02X: %d Eintraege, %d Payload-Varianten, verschiedene Werte je Byte 0..2: %s' % (tag, n, distinct, pos))
    print('3. Varianten je Dateityp:')
    for name, c in sorted(variants(files).items()):
        print('   %s: %s' % (name, dict(sorted(c.items()))))


if __name__ == '__main__':
    main(sys.argv)
