from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import subprocess
import argparse
parser = argparse.ArgumentParser(description="Package KernelSU or Magisk experimental module")
parser.add_argument("--target", choices=["kernelsu", "magisk", "apatch"], default="kernelsu")
target = parser.parse_args().target
base = Path(__file__).resolve().parent
for name in ("service.sh", "customize.sh"):
    subprocess.run(["sh", "-n", str(base / "module" / name)], check=True)
out = base / ("dist/magic-finger-" + target + "-0.2-beta.zip")
with ZipFile(out, "w", ZIP_DEFLATED) as z:
    for p in sorted((base / "module").iterdir()):
        if p.is_file(): z.write(p, p.name)
    z.write(base / "dist/classes.dex", "classes.dex")
    if target == "magisk":
        installer = base / "magisk/META-INF/com/google/android/update-binary"
        subprocess.run(["sh", "-n", str(installer)], check=True)
        z.write(installer, "META-INF/com/google/android/update-binary")
        z.writestr("META-INF/com/google/android/updater-script", "#MAGISK\n")
with ZipFile(out) as z:
    assert z.testzip() is None
    assert {"module.prop", "service.sh", "customize.sh", "classes.dex"} <= set(z.namelist())
    if target == "magisk":
        assert z.read("META-INF/com/google/android/updater-script") == b"#MAGISK\n"
print(out)
