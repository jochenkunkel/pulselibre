#!/usr/bin/env python3
"""Exclusive S25 lease for agents sharing the same VSCodium host and checkout.

Usage: python3 tools/s25_lease.py status
       python3 tools/s25_lease.py run --agent Codex --minutes 20 -- adb shell getprop ro.product.model
       python3 tools/s25_lease.py run --agent Codex --minutes 20 -- bash -lc '...'
Keep the wrapper alive for the *entire* device session. Requires POSIX flock.
"""
import argparse
import datetime as dt
import fcntl
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / ".s25-lease"
LOCK = STATE / "lock"
STATUS = STATE / "status.json"


def read_status():
    try:
        return json.loads(STATUS.read_text(encoding="utf-8"))
    except (FileNotFoundError, ValueError):
        return None


def main():
    parser = argparse.ArgumentParser(description="Exclusive S25 device access on one shared host")
    sub = parser.add_subparsers(dest="action", required=True)
    sub.add_parser("status")
    run = sub.add_parser("run")
    run.add_argument("--agent", required=True)
    run.add_argument("--minutes", required=True, type=int)
    run.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    STATE.mkdir(mode=0o700, parents=True, exist_ok=True)
    with LOCK.open("a+") as lock:
        if args.action == "status":
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError:
                print("BELEGT:", json.dumps(read_status(), ensure_ascii=False))
                return 1
            else:
                print("FREI (keine aktive Sperre)")
                return 0
        if args.minutes < 1 or args.minutes > 480:
            parser.error("--minutes muss zwischen 1 und 480 liegen")
        command = args.command
        if command and command[0] == "--":
            command = command[1:]
        if not command:
            parser.error("Nach -- muss ein auszuführender Befehl stehen")
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            print("S25 BELEGT:", json.dumps(read_status(), ensure_ascii=False), file=sys.stderr)
            return 2
        now = dt.datetime.now(dt.timezone.utc)
        record = {
            "agent": args.agent,
            "host": socket.gethostname(),
            "pid": os.getpid(),
            "started_utc": now.isoformat(),
            "expected_free_utc": (now + dt.timedelta(minutes=args.minutes)).isoformat(),
            "command": command[0],
        }
        temp = STATUS.with_suffix(".tmp")
        temp.write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        temp.replace(STATUS)
        print("S25 reserviert:", json.dumps(record, ensure_ascii=False), flush=True)
        try:
            result = subprocess.run(command, check=False)
            return result.returncode
        finally:
            STATUS.unlink(missing_ok=True)
            print("S25 wieder FREI", flush=True)


if __name__ == "__main__":
    sys.exit(main())
