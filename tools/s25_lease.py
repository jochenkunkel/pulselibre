#!/usr/bin/env python3
"""Exclusive per-device leases for physical PulsLibre test devices.

Legacy usage remains valid and defaults to S25:
  python3 tools/s25_lease.py status
  python3 tools/s25_lease.py run --agent Codex --minutes 20 -- adb ...

S10 usage:
  python3 tools/s25_lease.py status --device s10
  python3 tools/s25_lease.py run --device s10 --agent Codex --minutes 40 -- bash ...

The wrapper holds a POSIX flock until the command and its process group have
finished. ETA is informational and never expires or preempts a lease.
"""
import argparse
import datetime as dt
import fcntl
import json
import os
from pathlib import Path
import signal
import socket
import subprocess
import sys
import time


def _state_for(device):
    # Keep the original S25 path so old clients and worktrees share its lock.
    if device == "s25":
        default = Path.home() / ".cache" / "pulselibre" / "s25-lease"
        value = os.environ.get("PULSELIBRE_S25_LOCK_DIR", str(default))
    else:
        default = Path.home() / ".cache" / "pulselibre" / "s10-lease"
        value = os.environ.get("PULSELIBRE_S10_LOCK_DIR", str(default))
    return Path(value).expanduser()


def read_status(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (FileNotFoundError, ValueError, OSError):
        return None


def _atomic_status(path, record):
    temp = path.with_suffix(".tmp")
    fd = os.open(temp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as stream:
        json.dump(record, stream, ensure_ascii=False, indent=2)
        stream.write("\n")
        stream.flush()
        os.fsync(stream.fileno())
    temp.replace(path)


def _run(command, status_path):
    child = subprocess.Popen(command, start_new_session=True)
    handled_signal = []

    def stop_child(signum, _frame):
        if handled_signal:
            return
        handled_signal.append(signum)
        try:
            os.killpg(child.pid, signum)
        except ProcessLookupError:
            pass

    previous = {}
    for sig in (signal.SIGINT, signal.SIGTERM, signal.SIGHUP):
        previous[sig] = signal.signal(sig, stop_child)
    try:
        while True:
            try:
                result = child.wait()
                break
            except InterruptedError:
                continue
        # A shell may exit before a background descendant. Do not release the
        # device while anything in the command's process group is still alive.
        try:
            os.killpg(child.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
        deadline = time.monotonic() + 2
        while time.monotonic() < deadline:
            try:
                os.killpg(child.pid, 0)
            except ProcessLookupError:
                break
            time.sleep(0.05)
        else:
            try:
                os.killpg(child.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
        if handled_signal:
            return 128 + handled_signal[0]
        return result
    finally:
        for sig, handler in previous.items():
            signal.signal(sig, handler)
        status_path.unlink(missing_ok=True)


def main(argv=None):
    parser = argparse.ArgumentParser(description="Exclusive per-device access lease")
    sub = parser.add_subparsers(dest="action", required=True)
    status_parser = sub.add_parser("status")
    status_parser.add_argument("--device", default="s25")
    run = sub.add_parser("run")
    run.add_argument("--device", default="s25")
    run.add_argument("--agent", required=True)
    run.add_argument("--minutes", required=True, type=int)
    run.add_argument("--task")
    run.add_argument("--session-id")
    run.add_argument("--test-class")
    run.add_argument("--phase", default="running")
    run.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args(argv)

    device = args.device.lower()
    # Serial aliases must be configured explicitly so a serial cannot silently
    # select a different device's lock. Never persist or print the serial.
    if device in ("s10", "s25"):
        canonical_device = device
    elif device and device == os.environ.get("PULSELIBRE_S10_SERIAL", "").lower():
        canonical_device = "s10"
    elif device and device == os.environ.get("PULSELIBRE_S25_SERIAL", "").lower():
        canonical_device = "s25"
    else:
        parser.error("--device must be s10 or s25; map an ADB serial with PULSELIBRE_S10_SERIAL or PULSELIBRE_S25_SERIAL")
    state = _state_for(canonical_device)
    lock_path = state / "lock"
    status_path = state / "status.json"
    state.mkdir(mode=0o700, parents=True, exist_ok=True)
    os.chmod(state, 0o700)

    with lock_path.open("a+") as lock:
        if args.action == "status":
            try:
                fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError:
                data = read_status(status_path)
                print(f"BELEGT ({canonical_device.upper()}):", json.dumps(data, ensure_ascii=False))
                return 1
            stale = read_status(status_path)
            if stale:
                print(f"FREI ({canonical_device.upper()}; veraltete Statusdatei ignoriert)")
            else:
                print(f"FREI ({canonical_device.upper()}; keine aktive Sperre)")
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
            print(f"{canonical_device.upper()} BELEGT:", json.dumps(read_status(status_path), ensure_ascii=False), file=sys.stderr)
            return 2

        now = dt.datetime.now(dt.timezone.utc)
        record = {
            "device": canonical_device,
            "agent": args.agent,
            "host": socket.gethostname(),
            "pid": os.getpid(),
            "started_utc": now.isoformat(),
            "expected_free_utc": (now + dt.timedelta(minutes=args.minutes)).isoformat(),
            "phase": args.phase,
            "command": Path(command[0]).name,
        }
        for key in ("task", "session_id", "test_class"):
            value = getattr(args, key)
            if value:
                record[key] = value
        _atomic_status(status_path, record)
        print(f"{canonical_device.upper()} reserviert:", json.dumps(record, ensure_ascii=False), flush=True)
        try:
            return _run(command, status_path)
        finally:
            print(f"{canonical_device.upper()} wieder FREI", flush=True)


if __name__ == "__main__":
    sys.exit(main())
