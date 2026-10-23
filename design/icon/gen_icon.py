"""生成 GithubSrcReader 的应用图标（渐变底 + 白色 </> 符号）。

纯程序化绘制，无第三方素材、无水印；高倍超采样后再降采样，保证小尺寸边缘干净。
用法：
    python gen_icon.py
输出：design/icon/*.png（预览）与 app/src/main/res/mipmap-*/ic_launcher*.png（各密度）
"""
import os

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.normpath(os.path.join(HERE, "..", "..", "app", "src", "main", "res"))

# 图标底色：深海军蓝 -> 亮蓝，对角渐变
C0 = (14, 23, 42)      # #0E172A
C1 = (31, 111, 235)    # #1F6FEB

# 设计稿基准尺寸，所有坐标都按它定义
BASE = 1024
SUPERSAMPLE = 4

# 符号坐标（1024 空间），字形整体居中、宽约 53%
STROKE = 84
SCALE = 1.10   # 相对设计稿放大，小尺寸下更醒目，同时仍落在自适应图标安全区内
LESS = [(392, 352), (280, 512), (392, 672)]
SLASH = [(545, 672), (479, 352)]
GREATER = [(632, 352), (744, 512), (632, 672)]


def scaled(points):
    """以画布中心为基准整体放大。"""
    c = BASE / 2
    return [(c + (x - c) * SCALE, c + (y - c) * SCALE) for x, y in points]

DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def make_gradient(size: int) -> Image.Image:
    """对角线性渐变 + 左上角柔和高光。"""
    img = Image.new("RGB", (size, size))
    px = img.load()
    denom = 2 * (size - 1)
    for y in range(size):
        for x in range(size):
            t = (x + y) / denom
            px[x, y] = (
                round(C0[0] + (C1[0] - C0[0]) * t),
                round(C0[1] + (C1[1] - C0[1]) * t),
                round(C0[2] + (C1[2] - C0[2]) * t),
            )

    glow = Image.new("L", (size, size), 0)
    radius = size * 0.55
    ImageDraw.Draw(glow).ellipse(
        [-radius * 0.35, -radius * 0.55, radius * 0.75, radius * 0.55], fill=34
    )
    glow = glow.filter(ImageFilter.GaussianBlur(size * 0.14))
    return Image.composite(Image.new("RGB", (size, size), (255, 255, 255)), img, glow)


def make_glyph(size: int) -> Image.Image:
    """白色 </> 符号，透明底；超采样后缩小做抗锯齿。"""
    big = size * SUPERSAMPLE
    layer = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    k = big / BASE

    def stroke(points, width):
        w = max(2, round(width * k))
        pts = [(x * k, y * k) for x, y in points]
        draw.line(pts, fill=(255, 255, 255, 255), width=w, joint="curve")
        r = w / 2
        for x, y in (pts[0], pts[-1]):
            draw.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 255))

    stroke(scaled(LESS), STROKE)
    stroke(scaled(SLASH), STROKE * 0.74)
    stroke(scaled(GREATER), STROKE)
    return layer.resize((size, size), Image.LANCZOS)


def compose(size: int) -> Image.Image:
    img = make_gradient(size).convert("RGBA")
    img.alpha_composite(make_glyph(size))
    return img


def with_mask(img: Image.Image, radius_ratio: float | None, circle: bool = False) -> Image.Image:
    size = img.size[0]
    mask = Image.new("L", (size * SUPERSAMPLE, size * SUPERSAMPLE), 0)
    draw = ImageDraw.Draw(mask)
    box = [0, 0, size * SUPERSAMPLE - 1, size * SUPERSAMPLE - 1]
    if circle:
        draw.ellipse(box, fill=255)
    else:
        draw.rounded_rectangle(box, radius=size * SUPERSAMPLE * radius_ratio, fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out = img.copy()
    out.putalpha(mask)
    return out


def main() -> None:
    # 预览大图
    full = compose(1024)
    full.save(os.path.join(HERE, "app_icon_1024.png"))
    with_mask(full, 0.225).save(os.path.join(HERE, "app_icon_rounded_512.png"))
    with_mask(full.resize((512, 512), Image.LANCZOS), None, circle=True).save(
        os.path.join(HERE, "app_icon_circle_512.png")
    )

    # 各密度的传统图标
    for density, px in DENSITIES.items():
        folder = os.path.join(RES, f"mipmap-{density}")
        os.makedirs(folder, exist_ok=True)
        for stale in ("ic_launcher.webp", "ic_launcher_round.webp"):
            path = os.path.join(folder, stale)
            if os.path.exists(path):
                os.remove(path)
        icon = compose(px)
        with_mask(icon, 0.225).save(os.path.join(folder, "ic_launcher.png"))
        with_mask(icon, None, circle=True).save(os.path.join(folder, "ic_launcher_round.png"))

    print("icon generated:", RES)


if __name__ == "__main__":
    main()
