#!/usr/bin/env python3
"""Export only the reviewed project; never traverse coursework snapshots."""
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile


def main():
    root = Path(__file__).resolve().parent.parent
    target = root / "dist" / "eclassroom-github-ready.zip"
    target.parent.mkdir(exist_ok=True)
    files = [root / name for name in ("README.md", "CHANGELOG.md", ".gitignore", ".gitattributes")]
    for name in ("src", "scripts", "docs", ".github"):
        files.extend(p for p in (root / name).rglob("*")
                     if p.is_file() and not p.is_symlink()
                     and p.name != ".DS_Store" and "__pycache__" not in p.parts)
    with ZipFile(target, "w", compression=ZIP_DEFLATED) as archive:
        for source in sorted(files):
            archive.write(source, Path("eclassroom") / source.relative_to(root))
    print(f"Packaged {len(files)} files: {target}")


if __name__ == "__main__":
    main()
