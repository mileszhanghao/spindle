"""Turn Gradle/Kotlin compiler errors and JUnit failures into GitHub annotations."""
import glob
import re
import sys
import xml.etree.ElementTree as ET

count = 0


def emit(kind, msg, file=None, line=None):
    global count
    if count >= 40:
        return
    count += 1
    msg = msg.replace("%", "%25").replace("\r", "").replace("\n", "%0A")[:900]
    loc = ""
    if file:
        loc = f" file={file}" + (f",line={line}" if line else "")
    print(f"::{kind}{loc}::{msg}")


log = open(sys.argv[1], errors="replace").read() if len(sys.argv) > 1 else ""
for m in re.finditer(r"^e: file://(?:/home/runner/work/[^/]+/[^/]+/)?(\S+?):(\d+):(\d+) (.+)$", log, re.M):
    emit("error", m.group(4), m.group(1), m.group(2))
for m in re.finditer(r"^(?:ERROR|error):? (.+)$", log, re.M):
    emit("error", m.group(1))
for m in re.finditer(r"^\* What went wrong:\n(.+(?:\n(?!\* Try).+){0,12})", log, re.M):
    emit("error", m.group(1))
for m in re.finditer(r"^(?:> )?(?:Could not resolve|Could not find) .+$", log, re.M):
    emit("error", m.group(0))

for path in glob.glob("**/build/test-results/**/*.xml", recursive=True):
    root = ET.parse(path).getroot()
    for case in root.iter("testcase"):
        for f in list(case.findall("failure")) + list(case.findall("error")):
            text = (f.get("message") or "") + "\n" + (f.text or "")[:600]
            emit("error", f"{case.get('classname')}.{case.get('name')}: {text}")
