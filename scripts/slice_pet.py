#!/usr/bin/env python3
"""
StudyBuddy 宠物原画切片脚本

用法：
  python scripts/slice_pet.py <input_png> [--out <output_dir>]

例：
  python scripts/slice_pet.py raw/orange_baby_idle.png

输入：1024x1024 透明 PNG（agent 生成的原画）
输出：app-v2/src/main/assets/pets/{breed}_{stage}_{emote}/{layer}.png × 7
       layer ∈ {body, head, ear_left, ear_right, tail, eye_left, eye_right}

依赖：
  pip install pillow numpy torch torchvision
  pip install git+https://github.com/facebookresearch/segment-anything.git
  下载 SAM checkpoint sam_vit_b_01ec64.pth 放 ./models/

如果 SAM 不可用（mac/windows 无 GPU），降级用启发式分割：
  - 左下角 120x120 直接清空（防水印）
  - 用颜色聚类 + 位置 prior 切分各部位
  - 简单粗暴但够用，后期手工微调
"""
import sys
import os
from pathlib import Path
from PIL import Image
import numpy as np


# 7 个层的位置 prior（相对画布的归一化 bounding box，xyxy）
# 这是基于 prompt 约定的"猫咪正面 3/4 朝向、居中偏上"画法估计
# SAM 不可用时按这个 prior 启发式裁剪
LAYER_PRIORS = {
    "tail":      (0.55, 0.45, 0.95, 0.95),   # 右下方
    "body":      (0.20, 0.45, 0.85, 0.95),   # 中下半
    "ear_left":  (0.30, 0.10, 0.50, 0.40),
    "ear_right": (0.50, 0.10, 0.70, 0.40),
    "head":      (0.25, 0.20, 0.75, 0.65),
    "eye_left":  (0.35, 0.35, 0.50, 0.50),
    "eye_right": (0.50, 0.35, 0.65, 0.50),
}

WATERMARK_BBOX = (0, 920, 120, 1024)  # 左下 120x120 防水印区


def remove_watermark_zone(img: Image.Image) -> Image.Image:
    """左下角 120x120 全部清空透明，去除可能的 AI 水印"""
    arr = np.array(img.convert("RGBA"))
    x1, y1, x2, y2 = WATERMARK_BBOX
    arr[y1:y2, x1:x2, 3] = 0
    return Image.fromarray(arr)


def parse_filename(path: Path):
    """orange_baby_idle.png -> ('orange_baby_idle', 'orange_baby_idle')"""
    stem = path.stem
    return stem


def slice_heuristic(img: Image.Image, out_dir: Path):
    """
    无 SAM 时的启发式切片：
    每层用 prior bbox 直接裁出整张图的对应区域，alpha 通道复用原图。
    切片之间会有重叠，渲染时按 Z 序叠加，多余部分被上层覆盖。
    """
    arr = np.array(img.convert("RGBA"))
    h, w = arr.shape[:2]
    out_dir.mkdir(parents=True, exist_ok=True)

    for layer, (x1n, y1n, x2n, y2n) in LAYER_PRIORS.items():
        x1, y1 = int(x1n * w), int(y1n * h)
        x2, y2 = int(x2n * w), int(y2n * h)
        # 在原图同样大小画布上，只保留 bbox 内的内容
        layer_arr = np.zeros_like(arr)
        layer_arr[y1:y2, x1:x2] = arr[y1:y2, x1:x2]
        Image.fromarray(layer_arr).save(out_dir / f"{layer}.png")
        print(f"  [ok] {layer}.png")


def slice_sam(img: Image.Image, out_dir: Path):
    """
    SAM 自动分割版：使用 SamPredictor + 文本/位置 prompt 提取每个部位
    需要 GPU，加载 ~1GB 模型权重
    """
    try:
        from segment_anything import sam_model_registry, SamPredictor
        import torch
    except (ImportError, ModuleNotFoundError):
        print("[info] segment-anything 未安装，使用启发式切片")
        slice_heuristic(img, out_dir)
        return

    sam_path = Path("models/sam_vit_b_01ec64.pth")
    if not sam_path.exists():
        print(f"[warn] {sam_path} 不存在，降级启发式分割")
        slice_heuristic(img, out_dir)
        return

    sam = sam_model_registry["vit_b"](checkpoint=str(sam_path))
    sam.to("cuda" if torch.cuda.is_available() else "cpu")
    predictor = SamPredictor(sam)

    img_rgb = np.array(img.convert("RGB"))
    predictor.set_image(img_rgb)

    arr = np.array(img.convert("RGBA"))
    h, w = arr.shape[:2]
    out_dir.mkdir(parents=True, exist_ok=True)

    for layer, (x1n, y1n, x2n, y2n) in LAYER_PRIORS.items():
        # 用 prior bbox 中心点作为 SAM 的点 prompt
        cx = int((x1n + x2n) / 2 * w)
        cy = int((y1n + y2n) / 2 * h)
        masks, scores, _ = predictor.predict(
            point_coords=np.array([[cx, cy]]),
            point_labels=np.array([1]),
            multimask_output=True,
        )
        # 选最高分的 mask，且不能太大（避免选到整只猫）
        best_idx = -1
        best_score = -1
        max_area = (x2n - x1n) * (y2n - y1n) * w * h * 1.5
        for i, m in enumerate(masks):
            area = m.sum()
            if area < max_area and scores[i] > best_score:
                best_score = scores[i]
                best_idx = i
        if best_idx == -1:
            best_idx = 0

        mask = masks[best_idx]
        layer_arr = arr.copy()
        layer_arr[~mask] = 0
        Image.fromarray(layer_arr).save(out_dir / f"{layer}.png")
        print(f"  [ok] {layer}.png (score={scores[best_idx]:.2f})")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    inp = Path(sys.argv[1])
    if not inp.exists():
        print(f"[error] {inp} not found")
        sys.exit(1)

    stem = parse_filename(inp)
    if stem.endswith("_egg"):
        # egg 阶段不切片，直接拷贝（去水印）
        out = Path("app-v2/src/main/assets/pets") / f"{stem}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        cleaned = remove_watermark_zone(Image.open(inp))
        cleaned.save(out)
        print(f"[ok] {inp.name} → {out}")
        return

    out_dir = Path("app-v2/src/main/assets/pets") / stem
    img = remove_watermark_zone(Image.open(inp))
    print(f"[slicing] {inp.name} → {out_dir}/")
    slice_sam(img, out_dir)
    print(f"[ok] 7 layers written to {out_dir}/")


if __name__ == "__main__":
    main()
