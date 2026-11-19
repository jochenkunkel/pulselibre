#!/usr/bin/env python3
"""Temporarily switch AGP 9 Java/Kotlin JVM targets for the CI experiment only."""
from pathlib import Path
import sys

JAVA_17 = "JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17"
JAVA_21 = "JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21"
KOTLIN_17 = "jvmTarget.set(JvmTarget.JVM_17)"
KOTLIN_21 = "jvmTarget.set(JvmTarget.JVM_21)"


def transform(source: str) -> str:
    """Fail closed on changed build-script syntax instead of silently not testing Java 21."""
    if source.count(JAVA_17) != 1 or source.count(KOTLIN_17) != 1:
        raise ValueError("Expected exactly one Java 17 and one Kotlin JVM_17 target")
    if JAVA_21 in source or KOTLIN_21 in source:
        raise ValueError("Build script already contains Java 21 target declarations")
    result = source.replace(JAVA_17, JAVA_21).replace(KOTLIN_17, KOTLIN_21)
    if JAVA_17 in result or KOTLIN_17 in result:
        raise ValueError("Target transformation incomplete")
    return result


def main() -> int:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("app/build.gradle.kts")
    original = path.read_text(encoding="utf-8")
    path.write_text(transform(original), encoding="utf-8")
    print("Experiment only: Java source/target 21; Kotlin JVM target 21")
    return 0


if __name__ == "__main__":
    sys.exit(main())
