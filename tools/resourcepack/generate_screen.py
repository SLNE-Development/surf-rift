import math
import random

from PIL import Image, ImageDraw, ImageFilter, ImageFont

from common import asset, write_json, clamp

VIGNETTE_SIZE = (256, 128)
VIGNETTE_HEIGHT = 150
VIGNETTE_ASCENT = 72
BANNER_SIZE = (512, 96)
BANNER_HEIGHT = 20
BANNER_ASCENT = 17


def smoothstep(edge0, edge1, x):
    t = max(0.0, min(1.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3 - 2 * t)


def hex_edge(x, y, scale):
    q = (x * math.sqrt(3) / 3 - y / 3) / scale
    r = (2 * y / 3) / scale
    cx, cz = q, r
    cy = -cx - cz
    rx, ry, rz = round(cx), round(cy), round(cz)
    dx, dy, dz = abs(rx - cx), abs(ry - cy), abs(rz - cz)
    if dx > dy and dx > dz:
        rx = -ry - rz
    elif dy > dz:
        ry = -rx - rz
    else:
        rz = -rx - ry
    distance = max(abs(cx - rx), abs(cy - ry), abs(cz - rz))
    return smoothstep(0.38, 0.5, distance)


def vignette(inner, outer, start, end, max_alpha, base_alpha=0.0, hexes=False, grain=0.0, seed=0):
    width, height = VIGNETTE_SIZE
    image = Image.new("RGBA", VIGNETTE_SIZE)
    rng = random.Random(seed)
    for y in range(height):
        for x in range(width):
            nx = (x + 0.5) / width - 0.5
            ny = (y + 0.5) / height - 0.5
            distance = math.sqrt((nx * 2) ** 2 + (ny * 2) ** 2)
            strength = smoothstep(start, end, distance)
            if hexes:
                strength = min(1.0, strength * (0.75 + 0.6 * hex_edge(x, y, 7.0)))
            if grain:
                strength = max(0.0, min(1.0, strength + (rng.random() - 0.5) * grain * strength))
            alpha = base_alpha + (max_alpha - base_alpha) * strength
            t = smoothstep(start, end + 0.3, distance)
            color = tuple(clamp(inner[i] + (outer[i] - inner[i]) * t) for i in range(3))
            a = clamp(alpha * 255)
            image.putpixel((x, y), (*color, a if a >= 26 else 0))
    return image


def victory_banner():
    width, height = BANNER_SIZE
    image = Image.new("RGBA", BANNER_SIZE, (0, 0, 0, 0))

    glow = Image.new("RGBA", BANNER_SIZE, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse((40, 14, width - 40, height - 14), fill=(150, 90, 255, 150))
    glow = glow.filter(ImageFilter.GaussianBlur(14))
    image = Image.alpha_composite(image, glow)

    rays = Image.new("RGBA", BANNER_SIZE, (0, 0, 0, 0))
    rays_draw = ImageDraw.Draw(rays)
    cx, cy = width / 2, height / 2
    for index in range(18):
        angle = index / 18 * math.pi * 2
        x1 = cx + math.cos(angle) * 300
        y1 = cy + math.sin(angle) * 300
        x2 = cx + math.cos(angle + 0.06) * 300
        y2 = cy + math.sin(angle + 0.06) * 300
        rays_draw.polygon([(cx, cy), (x1, y1), (x2, y2)], fill=(255, 214, 120, 38))
    rays = rays.filter(ImageFilter.GaussianBlur(2))
    image = Image.alpha_composite(image, rays)

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/impact.ttf", 66)
    except OSError:
        font = ImageFont.load_default(size=60)

    text = "RIFT CHAMPION"
    mask = Image.new("L", BANNER_SIZE, 0)
    mask_draw = ImageDraw.Draw(mask)
    box = mask_draw.textbbox((0, 0), text, font=font)
    tx = (width - (box[2] - box[0])) / 2 - box[0]
    ty = (height - (box[3] - box[1])) / 2 - box[1]

    outline = Image.new("L", BANNER_SIZE, 0)
    ImageDraw.Draw(outline).text((tx, ty), text, font=font, fill=255, stroke_width=5, stroke_fill=255)
    outline_layer = Image.new("RGBA", BANNER_SIZE, (36, 12, 64, 255))
    outline_layer.putalpha(outline)
    image = Image.alpha_composite(image, outline_layer)

    mask_draw.text((tx, ty), text, font=font, fill=255)
    gradient = Image.new("RGBA", BANNER_SIZE)
    for y in range(height):
        t = y / (height - 1)
        top = (255, 246, 196)
        middle = (255, 205, 74)
        bottom = (214, 120, 26)
        if t < 0.5:
            color = tuple(clamp(top[i] + (middle[i] - top[i]) * (t / 0.5)) for i in range(3))
        else:
            color = tuple(clamp(middle[i] + (bottom[i] - middle[i]) * ((t - 0.5) / 0.5)) for i in range(3))
        for x in range(width):
            gradient.putpixel((x, y), (*color, 255))
    gradient.putalpha(mask)
    image = Image.alpha_composite(image, gradient)

    shine = Image.new("RGBA", BANNER_SIZE, (0, 0, 0, 0))
    shine_mask = Image.new("L", BANNER_SIZE, 0)
    ImageDraw.Draw(shine_mask).rectangle((0, 0, width, int(ty + (box[3] - box[1]) * 0.42 + box[1])), fill=70)
    shine_mask = Image.composite(shine_mask, Image.new("L", BANNER_SIZE, 0), mask)
    shine_layer = Image.new("RGBA", BANNER_SIZE, (255, 255, 255, 255))
    shine_layer.putalpha(shine_mask)
    image = Image.alpha_composite(image, shine_layer)

    sparkle = ImageDraw.Draw(image)
    rng = random.Random(7)
    for _ in range(26):
        x = rng.randint(10, width - 10)
        y = rng.randint(4, height - 4)
        size = rng.choice((1, 1, 2, 3))
        color = (255, 244, 200, 230)
        sparkle.line((x - size, y, x + size, y), fill=color)
        sparkle.line((x, y - size, x, y + size), fill=color)

    pixels = image.load()
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            fade = min(1.0, min(x, width - 1 - x) / 70.0, min(y, height - 1 - y) / 14.0)
            text_alpha = mask.getpixel((x, y)) + outline.getpixel((x, y))
            a = a if text_alpha > 0 else int(a * fade)
            pixels[x, y] = (r, g, b, a) if a >= 26 else (0, 0, 0, 0)
    return image


def pack_icon():
    size = 128
    image = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - size / 2, y - size / 2) / (size / 2)
            base = (int(46 - 30 * d), int(20 - 12 * d), int(78 - 40 * d))
            image.putpixel((x, y), (max(base[0], 8), max(base[1], 4), max(base[2], 18), 255))

    crack = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(crack)
    points = [(64, 6), (58, 26), (70, 40), (54, 58), (72, 74), (60, 92), (68, 108), (62, 122)]
    draw.line(points, fill=(170, 120, 255, 255), width=14, joint="curve")
    glow = crack.filter(ImageFilter.GaussianBlur(8))
    image = Image.alpha_composite(image, glow)
    image = Image.alpha_composite(image, glow)
    core = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    core_draw = ImageDraw.Draw(core)
    core_draw.line(points, fill=(94, 231, 245, 255), width=6, joint="curve")
    core_draw.line(points, fill=(235, 252, 255, 255), width=2, joint="curve")
    image = Image.alpha_composite(image, core)

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/impact.ttf", 30)
    except OSError:
        font = ImageFont.load_default(size=28)
    text_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    text_draw = ImageDraw.Draw(text_layer)
    for text, y, color in (("RIFT", 16, (255, 255, 255, 255)), ("ROYALE", 82, (255, 209, 102, 255))):
        box = text_draw.textbbox((0, 0), text, font=font)
        x = (size - (box[2] - box[0])) / 2 - box[0]
        text_draw.text((x, y - box[1]), text, font=font, fill=color, stroke_width=3, stroke_fill=(24, 8, 44, 255))
    image = Image.alpha_composite(image, text_layer)
    return image


def main():
    fill = Image.new("RGBA", (32, 16), (255, 255, 255, 255))
    fill.save(asset("rift", "textures", "font", "screen", "fill.png"))

    vignette((200, 30, 40), (120, 0, 10), 0.42, 1.05, 0.78, grain=0.25, seed=1).save(asset("rift", "textures", "font", "screen", "damage.png"))
    vignette((255, 214, 110), (176, 110, 255), 0.5, 1.1, 0.62, grain=0.15, seed=2).save(asset("rift", "textures", "font", "screen", "kill.png"))
    vignette((110, 0, 12), (30, 0, 4), 0.1, 0.95, 0.94, base_alpha=0.22, grain=0.2, seed=3).save(asset("rift", "textures", "font", "screen", "death.png"))
    vignette((130, 150, 255), (176, 90, 255), 0.48, 1.08, 0.68, hexes=True, seed=4).save(asset("rift", "textures", "font", "screen", "zone.png"))
    victory_banner().save(asset("rift", "textures", "font", "screen", "victory.png"))

    providers = [
        {"type": "bitmap", "file": "rift:font/screen/fill.png", "ascent": 253, "height": 512, "chars": ["\ue000"]},
        {"type": "bitmap", "file": "rift:font/screen/damage.png", "ascent": VIGNETTE_ASCENT, "height": VIGNETTE_HEIGHT, "chars": ["\ue001"]},
        {"type": "bitmap", "file": "rift:font/screen/kill.png", "ascent": VIGNETTE_ASCENT, "height": VIGNETTE_HEIGHT, "chars": ["\ue002"]},
        {"type": "bitmap", "file": "rift:font/screen/death.png", "ascent": VIGNETTE_ASCENT, "height": VIGNETTE_HEIGHT, "chars": ["\ue003"]},
        {"type": "bitmap", "file": "rift:font/screen/zone.png", "ascent": VIGNETTE_ASCENT, "height": VIGNETTE_HEIGHT, "chars": ["\ue004"]},
        {"type": "bitmap", "file": "rift:font/screen/victory.png", "ascent": BANNER_ASCENT, "height": BANNER_HEIGHT, "chars": ["\ue005"]},
    ]
    write_json(asset("rift", "font", "screen.json"), {"providers": providers})

    pack_icon().save(asset("..", "pack.png"))


if __name__ == "__main__":
    main()
