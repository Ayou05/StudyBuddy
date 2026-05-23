# StudyBuddy v2 宠物切片原画生成 Prompt

> 核心理念：agent 画 **1 张高质量原画**，开发侧切成 7 块独立部位，Compose 代码驱动每块做动画。这样动起来比 frame-by-frame 多张贴图更精致，agent 也只需做最稳定的事——画一张静态。

---

## 通用规范（橘猫 / 暹罗共用）

### 风格基调
- **设计语言**：Claude warm canvas 极简插画风（参考 Anthropic / Linear / Notion 配图气质）
- **画法**：soft smooth illustration，柔和渐变 + 细线描边，**不要**像素风、矢量扁平、漫画日系、3D 渲染、科幻金属感
- **氛围**：温柔、安静、有"想摸一下"的毛绒触感
- **核心调色板**（必须使用）：
  - canvas 底色 `#FAF9F5`（仅作参考，输出不画背景）
  - coral 强调色 `#CC785C`（仅极少装饰，不滥用）
  - ink 主线条 `#181715`（柔细线，不要硬黑边）
  - 中性灰 `#6C6A64` 用于阴影
- 整体毛色不超过 4 个层次，留白要足

### 技术规格（**必须严格遵守，否则需重画**）

1. **画布尺寸**：1024×1024 像素，正方形（高分辨率，方便切片后保留细节）
2. **背景**：**完全透明 PNG**（alpha=0）
3. **前景实色保护**（重要 ⚠️）：猫咪本体所有原本应该是白色 / 浅色的部分（如肚皮、爪垫、暹罗的浅毛区），**必须使用 alpha=1.0 的实色**（如 `#FAF6E8` 暖白），**绝对不能**让前景出现半透明 / 透明洞。生成器有把白色误判为透明的倾向，请明确填实色
4. **左下角 120×120 像素安全裁剪区**：左下角不要画任何关键内容（脸、四肢、尾巴等），可以放无关装饰或留空 —— 这块区域将在导入时被裁剪掉以去除 AI 水印
5. **居中构图**：猫咪整体居于画布中央偏上，上下左右各留 ≥80px 边距
6. **正面 3/4 朝向**：猫咪面向画面右前方约 3/4 角度，**不要正侧面、不要正面**。后期镜像即可得到左朝向版本

### 切片友好的画法约束（最重要）

为了后期能切成 **身体 / 头 / 左耳 / 右耳 / 尾巴 / 左眼 / 右眼** 7 个独立图层，请遵守：

7. **关节处必须 5-10px 实色重叠**：
   - 头和身体连接处（脖子）：头的下沿要往身体方向多画 8px，确保切了之后头部独立移动时不会露出底下空白
   - 耳朵和头连接处：耳朵根部往头方向多画 5px
   - 尾巴和身体连接处：尾巴根部往身体方向多画 8px

8. **眼睛画成可分离的椭圆**：
   - 左右眼分得开（两眼间距 ≥ 眼宽的 1.2 倍）
   - 眼睛不要画在脸轮廓线上
   - 眼周不要画过深的眼线（影响后期单独提取）

9. **耳朵不要相连**：左右耳之间留可见间隔，绝不能两只耳朵连成一条横线

10. **尾巴不要遮挡身体**：尾巴自然垂在身体右侧或后方，不要绕到身前覆盖肚皮

11. **四肢明确**：前后腿都要清晰可辨，不要藏到身下或互相重叠遮挡

### 5 阶段成长

| stage | 描述 |
|---|---|
| `egg` | 暖米色椭圆蛋，蛋面有品种色斑纹，蛋顶可有一小撮萌芽毛尖 —— **不切片**，作为单图使用 |
| `baby` | 圆滚滚奶猫，头大身体小，眼睛占脸 1/3，四肢短粗，毛绒绒 |
| `young` | 半大少年猫，比例介于 baby 和 adult，腿略修长 |
| `adult` | 标准成猫，姿态从容稳定 |
| `ultimate` | 完全体，毛发更丰盈，眼神更深邃，加 1 个品种特征细节 |

### 4 表情

| emote | 描述 |
|---|---|
| `idle` | 平静坐姿，眼睛半眯，神态安详 |
| `happy` | 嘴角上扬呈"^"形，眼睛弯成月牙 |
| `sad` | 耳朵往后压，眼睛朝下 |
| `sleeping` | 蜷成一团或卧倒，眼睛闭合成一条线 |

### 数量
- 每品种：1 egg + 4 stage × 4 emote = **17 张原画/品种**
- 双品种共 **34 张原画**
- 切片后双品种共 (34-2) × 7 + 2 = **226 个图层文件**，但生成阶段只需 34 张原画

### 文件命名
`{breed}_{stage}_{emote}.png`，全小写下划线
- breed: `orange` / `siamese`
- stage: `egg` / `baby` / `young` / `adult` / `ultimate`
- emote: `idle` / `happy` / `sad` / `sleeping`（egg 不分 emote）

例：`orange_baby_happy.png`、`siamese_adult_sleeping.png`、`orange_egg.png`

---

## Prompt 1：橘猫（orange）

按上方"通用规范"画 17 张橘猫原画，包含 1 张 egg 和 4 stage × 4 emote = 16 张全身像。

**橘猫品种特征**：
- 主毛色：暖橘色 `#E8965A` 到深橘 `#C76936` 的柔和渐变（虎斑可有可无，有则细腻）
- 肚皮 / 下巴 / 爪垫 / 胸前小三角白毛：实色暖白 `#FAF6E8`（**必须不透明**）
- 眼睛：琥珀金 `#D4A017` 椭圆瞳
- 鼻头：粉色 `#E8A19A` 小三角
- 性格暗示：圆脸圆眼，憨态可掬，毛茸茸的圆润感，让人想揉

**ultimate 阶段特殊细节**：胸前白毛延伸成像围嘴一样的形状，毛色更浓郁

请生成（**严格按命名**）：
- `orange_egg.png`（不分 emote，1 张）
- `orange_baby_idle.png` / `orange_baby_happy.png` / `orange_baby_sad.png` / `orange_baby_sleeping.png`
- `orange_young_idle.png` / `orange_young_happy.png` / `orange_young_sad.png` / `orange_young_sleeping.png`
- `orange_adult_idle.png` / `orange_adult_happy.png` / `orange_adult_sad.png` / `orange_adult_sleeping.png`
- `orange_ultimate_idle.png` / `orange_ultimate_happy.png` / `orange_ultimate_sad.png` / `orange_ultimate_sleeping.png`

共 **17 张**。

**再次强调技术约束**：
- 1024×1024 px 透明 PNG，正面 3/4 朝向
- 左下角 120×120 px 不要放主体（防水印裁切）
- 浅色 / 白色部位必须用实色（暖白 #FAF6E8）填充，**禁止半透明**
- 关节处必须 5-10px 实色重叠（脖子 / 耳根 / 尾根）
- 眼睛分离、耳朵分离、尾巴不遮身、四肢清晰
- soft smooth illustration 风格，**绝不要**像素 / 扁平 / 日系 / 3D / 科幻

---

## Prompt 2：暹罗（siamese）

按上方"通用规范"画 17 张暹罗猫原画，包含 1 张 egg 和 4 stage × 4 emote = 16 张全身像。

**暹罗品种特征**：
- 主毛色：浅米色身体 `#EFE5D2` 到深米 `#D9C9AC` 渐变
- 重点色（脸、耳、四肢、尾巴末端）：巧克力棕 `#5C3A2A` 到深棕 `#3D2418` 渐变
- 重点色和身体的过渡要柔和，不能死板分块
- 浅色身体：实色暖米 `#EFE5D2`（**必须不透明**）
- 眼睛：冰川蓝 `#7BA8C9`，是暹罗最大特点
- 鼻头：深棕 `#3D2418`
- 性格暗示：尖脸、长身、修长四肢，优雅、聪慧、略带高冷但不冰冷

**ultimate 阶段特殊细节**：眼神更尖锐，毛色对比更深，耳尖更明显的深棕

请生成（**严格按命名**）：
- `siamese_egg.png`
- `siamese_baby_idle.png` / `siamese_baby_happy.png` / `siamese_baby_sad.png` / `siamese_baby_sleeping.png`
- `siamese_young_idle.png` / `siamese_young_happy.png` / `siamese_young_sad.png` / `siamese_young_sleeping.png`
- `siamese_adult_idle.png` / `siamese_adult_happy.png` / `siamese_adult_sad.png` / `siamese_adult_sleeping.png`
- `siamese_ultimate_idle.png` / `siamese_ultimate_happy.png` / `siamese_ultimate_sad.png` / `siamese_ultimate_sleeping.png`

共 **17 张**。

**再次强调技术约束**：
- 1024×1024 px 透明 PNG，正面 3/4 朝向
- 左下角 120×120 px 不要放主体（防水印裁切）
- 浅色身体必须用实色（暖米 #EFE5D2）填充，**禁止**让浅毛区出现半透明 / 透明洞
- 关节处必须 5-10px 实色重叠（脖子 / 耳根 / 尾根）
- 眼睛分离、耳朵分离、尾巴不遮身、四肢清晰
- soft smooth illustration 风格，**绝不要**像素 / 扁平 / 日系 / 3D / 科幻

---

## 后续流程（你拿到 PNG 之后我做的）

1. 写切片脚本 `scripts/slice_pet.py`：用 Segment Anything Model (SAM) 自动分割每张原画为 7 块图层
2. 输出到 `app-v2/src/main/assets/pets/{breed}_{stage}_{emote}/{layer}.png`，layer ∈ {body, head, ear_left, ear_right, tail, eye_left, eye_right}
3. Compose 渲染层 `LayeredPetSprite.kt`：把 7 块按规定 Z 序叠加，每块独立 graphicsLayer 驱动呼吸 / 眨眼 / 摇尾 / 抖耳 / 喂食低头等动画
4. PetScreen 替换现有 AppPetImage 调用为 LayeredPetSprite
