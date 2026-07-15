import argparse
import io
import zipfile
from pathlib import Path


_FIXED_TIMESTAMP = (1980, 1, 1, 0, 0, 0)


def _write(archive: zipfile.ZipFile, name: str, content: bytes) -> None:
    entry = zipfile.ZipInfo(name, date_time=_FIXED_TIMESTAMP)
    entry.compress_type = zipfile.ZIP_STORED
    entry.external_attr = 0o644 << 16
    archive.writestr(entry, content)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--lint-jar", required=True)
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    classes = io.BytesIO()
    with zipfile.ZipFile(classes, "w"):
        pass

    with zipfile.ZipFile(args.output, "w") as archive:
        _write(archive, "AndroidManifest.xml", Path(args.manifest).read_bytes())
        _write(archive, "classes.jar", classes.getvalue())
        _write(archive, "R.txt", b"")
        _write(archive, "lint.jar", Path(args.lint_jar).read_bytes())


if __name__ == "__main__":
    main()
