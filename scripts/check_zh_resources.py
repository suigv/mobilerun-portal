#!/usr/bin/env python3
"""Check that Chinese strings cover the default resources and keep format arguments."""

from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/res"
FORMAT = re.compile(r"%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?(?:[tT][a-zA-Z]|[a-zA-Z%])")


def strings(path):
    result = {}
    for element in ET.parse(path).getroot():
        if element.tag != "string" or element.get("translatable") == "false":
            continue
        name = element.attrib["name"]
        if name in result:
            raise ValueError(f"{path}: duplicate string {name}")
        result[name] = "".join(element.itertext())
    return result


def main():
    default = strings(ROOT / "values/strings.xml")
    chinese = strings(ROOT / "values-zh-rCN/strings.xml")
    errors = [f"missing Chinese translation: {name}" for name in sorted(default.keys() - chinese.keys())]
    for name in sorted(default.keys() & chinese.keys()):
        if Counter(FORMAT.findall(default[name])) != Counter(FORMAT.findall(chinese[name])):
            errors.append(f"format arguments differ: {name}")
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Chinese resource coverage: {len(default)} strings; format arguments match")
    return 0


if __name__ == "__main__":
    sys.exit(main())
