# 宠物原画放这里

## 流程

1. **agent 集群按 `PET_REDRAW_PROMPTS.md` 出图**
2. **下载 PNG，按命名规范放到这个目录** `E:\Togedy\StudyBuddy\raw\`：
   ```
   raw/
     orange_egg.png
     orange_baby_idle.png
     orange_baby_happy.png
     orange_baby_sad.png
     orange_baby_sleeping.png
     orange_young_idle.png
     ... （共 17 张橘猫）
     siamese_egg.png
     siamese_baby_idle.png
     ... （共 17 张暹罗）
   ```
   总共 34 张

3. **每张跑切片脚本**（在 StudyBuddy 根目录开命令行）：
   ```bash
   python scripts/slice_pet.py raw/orange_baby_idle.png
   python scripts/slice_pet.py raw/orange_baby_happy.png
   ...
   ```
   或者批量：
   ```bash
   for f in raw/*.png; do python scripts/slice_pet.py "$f"; done
   ```

4. **切片自动输出到** `E:\Togedy\StudyBuddy\app-v2\src\main\assets\pets\` 下的子目录：
   ```
   app-v2/src/main/assets/pets/
     orange_egg.png                   ← egg 阶段单图
     orange_baby_idle/                ← 切片目录（7 个图层）
       body.png
       head.png
       ear_left.png
       ear_right.png
       tail.png
       eye_left.png
       eye_right.png
     orange_baby_happy/
       body.png
       ...
     ...
   ```

5. **代码自动加载**，不用改 Kotlin 代码

## 不用 SAM（最快路径）

切片脚本默认尝试 SAM 自动分割，没装也没事，会自动降级到启发式裁剪（按位置 prior 直接切 7 块 bounding box）。CPU 够用，但切片之间会有重叠（代码里靠 Z 序覆盖处理了）。

如果想要更精准的分割再装：
```bash
pip install pillow numpy torch torchvision
pip install git+https://github.com/facebookresearch/segment-anything.git
mkdir models
# 下载 https://dl.fbaipublicfiles.com/segment_anything/sam_vit_b_01ec64.pth 到 models/ 目录
```

## 不会的话

直接把第一张原画发我，我替你跑一遍切片，确认效果后再批量。
