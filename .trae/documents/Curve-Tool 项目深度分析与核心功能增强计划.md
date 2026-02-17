## 现状与定位（基于源码与 README）
- 这是一个轻量级 Java “曲线/时间序列”处理库：用函数式接口把遍历、条件、计算、赋值/消费抽象成链式 API（[ICurve](file:///Users/bytedance/java/curve-tool/src/curve/ICurve.java)、[Curve](file:///Users/bytedance/java/curve-tool/src/curve/Curve.java)）。
- 支持：单曲线逐点 process、双曲线按下标对齐叠加 biProcess、按 Key 分组后的组内处理/叠加（[ICurveGroup](file:///Users/bytedance/java/curve-tool/src/curve/ICurveGroup.java)、[CurveGroup](file:///Users/bytedance/java/curve-tool/src/curve/CurveGroup.java)）。
- 关键缺陷（README 也点到）：
  - 只能“严格下标对齐”，无法应对真实场景的时间戳对齐/缺点/乱序（[README.md:L19-L23](file:///Users/bytedance/java/curve-tool/README.md#L19-L23)）。
  - 对齐失败采取“静默 no-op”策略：长度不等直接跳过（[AbstractCurve.biTraversal](file:///Users/bytedance/java/curve-tool/src/curve/AbstractCurve.java#L30-L38)），分组缺 key 可能传入 null 触发 NPE（[AbstractCurveGroup.keySetTraversal](file:///Users/bytedance/java/curve-tool/src/curve/AbstractCurveGroup.java#L36-L44)）。
  - 工程形态更像源码片段库：无 Maven/Gradle、无测试、无可运行入口（仅 11 个 Java 文件）。

## 总体目标（不加冷门功能，优先补齐“真实可用”能力）
- 把它从“只适用于理想对齐数据”的工具，增强为可覆盖更广泛业务数据（时间序列、分组指标、缺点、左右对齐）的通用曲线处理库。
- 保持当前用法尽量不破坏；新增能力通过“新 API/新类/可选策略”提供。

## 核心优化与新增功能（按优先级）
### 1) 工程化与可验证性（让项目可依赖、可测试、可发布）
- 引入 Maven（或 Gradle）标准目录结构与构建脚本，补充最小化依赖（仅测试框架）。
- 增加单元测试覆盖：
  - 单曲线 process/条件 process
  - 双曲线对齐与失败策略
  - 分组 biProcess 在缺 key 情况下的行为
- 增加 runnable 示例（examples/ 或 test 中的演示），对应 README 里的场景。

### 2) “对齐失败”策略显式化（把静默失败变成可控行为）
- 为双曲线/分组叠加新增可选策略（不替换现有方法默认行为，避免破坏兼容）：
  - 长度不等：STRICT(抛异常)、TRUNCATE(按最短)、PAD_NULL(补 null)、SKIP(现有默认)
  - 分组 key 匹配：INNER_JOIN、LEFT_JOIN、FULL_OUTER + 缺失 curve 处理（空曲线/跳过/异常）
- 落地点：新增 `ZipPolicy/JoinPolicy` 这类小型策略对象，并提供新的 `biProcess(...)`/`forCurve(...)` 重载。

### 3) 时间戳/键对齐的 join（解决 README 的第一大缺陷）
- 新增“按 key（典型是 timestamp）对齐”的双曲线叠加：
  - `joinByKey(curve2, key1Fn, key2Fn, joinType, ...)`
  - 输出可选择：
    - 仍然“原地写回 T”（通过 BiConsumer）
    - 或生成“新曲线/新结果列表”（纯函数化，降低副作用调试成本）
- 支持常用 join：inner/left/right/full，以及重复 key 的处理策略（取最后/聚合/报错）。

### 4) 常用曲线算子（贴近现实工具：pandas/PromQL/Grafana 常用能力）
- 在不引入外部依赖的前提下，新增高频、通用、非冷门算子：
  - `map/flatMap/filter`（返回新 ICurve，而非只支持消费式 process）
  - `reduce/aggregate`（sum/min/max/avg/count，可用 extractor 得到 double/long）
  - `rolling(window)` + `movingAverage`（时间序列最常见）
  - `diff`（一阶差分）、`rate`（按时间差的变化率，可选）
- 保持现有 API 风格：既提供“返回新曲线”的纯算子，也提供“就地写回”的 consumer 式算子。

### 5) 性能与可调试性（在函数式风格下补齐“工程体验”）
- 增加可选的 imperative 遍历实现（减少 lambda 层级与分配），并保留现有实现。
- 为复杂链路提供可选的“命名步骤/中间结果捕获”能力：例如 `tap(name, consumer)` 或 `peek` 风格（不打印、不引入日志依赖，避免污染）。
- 并行能力先不默认启用：仅在返回新结果且无副作用 consumer 时提供 `parallelMap` 之类的安全接口（避免线程安全坑）。

## 架构调整建议（兼容优先，逐步演进）
- 当前 `ICurve extends List`、`Curve extends ArrayList` 的“继承集合类型”会把大量 List/Map 可变操作暴露为公共 API；短期保留以兼容。
- 中期建议新增“组合式”封装（例如 `CurveSeries<T>` 内部持有 `List<T>`），把“曲线运算 API”与“容器 API”隔离，减少误用与兼容负担；同时保留旧类型作为适配层。

## 实施步骤（执行阶段会按这个顺序落地）
1. 工程化：补 Maven/目录结构、基础测试框架、把现有源码迁移到标准结构。
2. 可靠性：新增对齐/缺 key 策略与对应的新重载 API；补齐 NPE 风险路径的测试。
3. 核心能力：实现 joinByKey（时间戳对齐）与常用算子（map/filter/reduce/rolling/movingAverage/diff）。
4. 示例与文档：更新 README，给出“严格对齐 vs 按时间戳对齐”的对比示例与最佳实践。
5. 回归验证：跑全量单测；补充边界用例（空曲线、null 值、乱序、重复 timestamp、分组缺 key）。

## 验收标准（完成后你能直观看到的变化）
- 项目可直接 `mvn test` 运行并通过。
- 典型业务数据（timestamp 不完全一致/存在缺点）可通过 joinByKey 正确叠加。
- 失败行为可选择严格抛错或宽松处理，不再默默跳过导致线上埋雷。
- README 有可复制粘贴运行的示例，覆盖单曲线、下标对齐叠加、时间戳对齐叠加、分组 join。
