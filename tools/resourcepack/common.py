import json
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / "surf-rift-resourcepack"


def asset(*parts: str) -> Path:
    path = PACK.joinpath("assets", *parts)
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def hex_rgba(value: str, alpha: int = 255):
    value = value.lstrip("#")
    return int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha


def clamp(value, low=0, high=255):
    return max(low, min(high, int(round(value))))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def ensure_dir(path: Path) -> Path:
    os.makedirs(path, exist_ok=True)
    return path
