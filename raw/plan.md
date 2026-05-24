# 猫咪原画资产生成计划

## 任务概述
为两个品种（橘猫 orange / 暹罗 siamese）各生成17张角色原画，共34张1024×1024透明PNG。

## 文件清单

### 橘猫（orange）— 17张
- orange_egg.png
- orange_baby_idle.png, orange_baby_happy.png, orange_baby_sad.png, orange_baby_sleeping.png
- orange_young_idle.png, orange_young_happy.png, orange_young_sad.png, orange_young_sleeping.png
- orange_adult_idle.png, orange_adult_happy.png, orange_adult_sad.png, orange_adult_sleeping.png
- orange_ultimate_idle.png, orange_ultimate_happy.png, orange_ultimate_sad.png, orange_ultimate_sleeping.png

### 暹罗（siamese）— 17张
- siamese_egg.png
- siamese_baby_idle.png, siamese_baby_happy.png, siamese_baby_sad.png, siamese_baby_sleeping.png
- siamese_young_idle.png, siamese_young_happy.png, siamese_young_sad.png, siamese_young_sleeping.png
- siamese_adult_idle.png, siamese_adult_happy.png, siamese_adult_sad.png, siamese_adult_sleeping.png
- siamese_ultimate_idle.png, siamese_ultimate_happy.png, siamese_ultimate_sad.png, siamese_ultimate_sleeping.png

## 执行策略

### Stage 1 — 并行生成（关键路径）
- 子代理 A：生成全部17张橘猫原画（sequential within agent, batched by tool calls）
- 子代理 B：生成全部17张暹罗猫原画（sequential within agent, batched by tool calls）

两个子代理完全独立，并行执行。

### Stage 2 — 验证与交付
- 检查所有34张文件是否成功生成
- 验证文件命名、数量
- 交付用户
