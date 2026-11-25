#!/usr/bin/env python3
"""Writes evidence into the GitHub Actions job summary.

  summarize.py tests <surefire-reports-dir>
  summarize.py gate  <sonar-url> <user> <password> <project-key>
"""
import base64
import glob
import json
import os
import sys
import urllib.request
import xml.etree.ElementTree as ET


def out(text: str) -> None:
    print(text)
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if path:
        with open(path, "a", encoding="utf-8") as handle:
            handle.write(text + "\n")


def tests(directory: str) -> int:
    total = failed = skipped = 0
    rows = []
    for report in sorted(glob.glob(os.path.join(directory, "TEST-*.xml"))):
        suite = ET.parse(report).getroot()
        n = int(suite.get("tests", 0))
        bad = int(suite.get("failures", 0)) + int(suite.get("errors", 0))
        skip = int(suite.get("skipped", 0))
        total, failed, skipped = total + n, failed + bad, skipped + skip
        rows.append(f"| {suite.get('name')} | {n} | {bad} | {skip} |")
    out("### Unit tests")
    out("| Class | Tests | Failed | Skipped |\n|---|---|---|---|")
    out("\n".join(rows))
    out(f"\n**Total: {total} tests, {failed} failed, {skipped} skipped**")
    return 1 if failed or total == 0 else 0


def gate(url: str, user: str, password: str, project: str) -> int:
    request = urllib.request.Request(f"{url}/api/qualitygates/project_status?projectKey={project}")
    token = base64.b64encode(f"{user}:{password}".encode()).decode()
    request.add_header("Authorization", f"Basic {token}")
    with urllib.request.urlopen(request, timeout=30) as response:
        status = json.load(response)["projectStatus"]
    out("### SonarQube quality gate")
    out(f"**Result: {status['status']}**\n")
    out("| Condition | Fails when | Actual | Result |\n|---|---|---|---|")
    for cond in status.get("conditions", []):
        comparator = {"LT": "< ", "GT": "> "}.get(cond["comparator"], cond["comparator"])
        out(f"| {cond['metricKey']} | {comparator}{cond.get('errorThreshold', '')} | {cond.get('actualValue', '')} | {cond['status']} |")
    return 0


if __name__ == "__main__":
    mode = sys.argv[1]
    sys.exit(tests(sys.argv[2]) if mode == "tests" else gate(*sys.argv[2:6]))
