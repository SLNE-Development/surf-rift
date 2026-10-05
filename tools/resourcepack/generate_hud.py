import json
from pathlib import Path

from PIL import Image

from common import asset, write_json

PILL_HEIGHT = 14
PILL_COLOR = (0, 0, 0, 144)
PILL_SEGMENTS = (1, 2, 4, 8, 16, 32, 64, 128)

ROW_ONE_PILL_ASCENT = 11
ROW_TWO_PILL_ASCENT = -6
ROW_ONE_TEXT_SHIFT = -1
ROW_TWO_TEXT_SHIFT = 16


def cap_image():
    image = Image.new("RGBA", (1, PILL_HEIGHT), (0, 0, 0, 0))
    for y in range(1, PILL_HEIGHT - 1):
        image.putpixel((0, y), PILL_COLOR)
    return image


def segment_image(width: int):
    return Image.new("RGBA", (width, PILL_HEIGHT), PILL_COLOR)


def vanilla_text_providers(shift: int):
    source = Path(__file__).with_name("vanilla_default_font.json")
    data = json.loads(source.read_text(encoding="utf-8"))
    providers = []
    for provider in data["providers"]:
        if provider.get("type") != "bitmap":
            continue
        shifted = dict(provider)
        shifted["ascent"] = provider.get("ascent", 7) - shift
        providers.append(shifted)
    return providers


def pill_providers(ascent: int):
    result = [{"type": "bitmap", "file": "rift:font/hud/pill_cap.png", "ascent": ascent, "height": PILL_HEIGHT, "chars": [""]}]
    for index, width in enumerate(PILL_SEGMENTS):
        result.append({"type": "bitmap", "file": f"rift:font/hud/pill_{width}.png", "ascent": ascent, "height": PILL_HEIGHT, "chars": [chr(0xE002 + index)]})
    return result


def row_font(pill_ascent: int, text_shift: int):
    providers = [{"type": "space", "advances": {" ": 4, "‌": 0}}]
    providers += pill_providers(pill_ascent)
    providers += vanilla_text_providers(text_shift)
    return {"providers": providers}


def main():
    cap_image().save(asset("rift", "textures", "font", "hud", "pill_cap.png"))
    for width in PILL_SEGMENTS:
        segment_image(width).save(asset("rift", "textures", "font", "hud", f"pill_{width}.png"))

    write_json(asset("rift", "font", "hud.json"), row_font(ROW_ONE_PILL_ASCENT, ROW_ONE_TEXT_SHIFT))
    write_json(asset("rift", "font", "hud2.json"), row_font(ROW_TWO_PILL_ASCENT, ROW_TWO_TEXT_SHIFT))

    advances = {}
    for index, value in enumerate((1, 2, 4, 8, 16, 32, 64, 128, 256)):
        advances[chr(0xF801 + index)] = -value
        advances[chr(0xF821 + index)] = value
    write_json(asset("rift", "font", "space.json"), {"providers": [{"type": "space", "advances": advances}]})

    transparent = Image.new("RGBA", (182, 5), (0, 0, 0, 0))
    transparent.save(asset("minecraft", "textures", "gui", "sprites", "boss_bar", "white_background.png"))
    transparent.save(asset("minecraft", "textures", "gui", "sprites", "boss_bar", "white_progress.png"))


if __name__ == "__main__":
    main()
