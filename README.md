# Cold Sweat Automation（冷汗自动化兼容模组）

一个 NeoForge 1.21.1 的附加模组，为 [Cold Sweat（冷汗）](https://github.com/Momo-Softworks/Cold-Sweat) 的壁炉（Hearth）、锅炉（Boiler）、冰箱（Icebox）添加物品自动化联动，并把冰箱的食物槽扩充到 108 格（GUI 同步修改）。

## 前置 / 依赖

| 模组 | 是否强制 | 说明 |
| --- | --- | --- |
| Cold Sweat（冷汗）`cold_sweat` | **强制** | 版本 `2.4.4`（本模组的 Mixin 按该版本编写） |
| Create（机械动力）`create` | 可选 | 装了它就会启用 Create 漏斗/溜槽联动 |
| AE2（应用能源2）`ae2` | 可选 | 装了它就会启用 AE2 总线/接口联动 |

运行时的检测规则：

- 只装了 **Create** → 只启用 Create 联动；
- 只装了 **AE2** → 只启用 AE2 联动；
- **两个都装** → 两个联动都启用；
- **两个都没装** → 仅启用冰箱 108 格扩容（自动化相关能力不注册）。

> 说明：Create 和 AE2 的物品传输都走 NeoForge 标准的 `IItemHandler` capability，所以本模组只需注册一个通用能力即可同时兼容两者，无需引入它们的 API 作为编译依赖。

## 功能

1. **燃料输入**：壁炉 / 锅炉 / 冰箱现在可以从**任意面**接收燃料（AE2 的 ME 接口/输出总线，Create 的漏斗/溜槽）。
   - 原来是 `WorldlyContainer` 只能从水平面进燃料、从顶/底面出成品，而顶面往往被烟囱占用；现在任意面都能进燃料、出成品。
2. **锅炉成品输出**：锅炉 1~9 号槽位（加热好的水袋 / 净化后的饮料）可从任意面被 AE2 输入总线或 Create 溜槽抽出。
   - 只有**加热完成**的水袋（温度 ≥ 50）才允许被自动化设备取走，避免总线提前抽走半成品。
3. **冰箱 108 格**：冰箱食物槽从 9 格扩充到 108 格（9 列 × 12 行），菜单与 GUI 一并更新；燃料槽保持不变（第 0 格）。

## 实现要点

| 文件 | 作用 |
| --- | --- |
| `AutomationEvents` | 给 `cold_sweat:icebox` / `boiler` / `hearth` 注册 `Capabilities.ItemHandler.BLOCK`，返回 `HearthItemHandler` |
| `compat/HearthItemHandler` | 包装 `WorldlyContainer`，按各方块自身的 `canPlaceItemThroughFace` / `canTakeItemThroughFace` 规则校验；插入后立即 `setChanged()` 触发 `checkForFuel()`，燃料立即被消耗 |
| `mixin/IceboxBlockEntityMixin` | `getContainerSize()` → 109（1 燃料 + 108 食物）、食物槽冷却循环扩展到 108、`getSlotsForFace` 暴露全部食物槽、`createMenu` 返回扩容器 |
| `container/BigIceboxContainer` | 独立菜单类型（`cold_sweat_automation:big_icebox`），1 燃料 + 108 食物(9×12) + 玩家背包 + 快捷栏 |
| `client/gui/BigIceboxScreen` | 放在 `com.momosoftworks.coldsweat.client.gui` 包下（需覆写父类的包级私有 `getBlockEntity()`），GUI 176×350 |
| `mixin/IceboxOpenersMixin` | 让冰箱的盖子动画/开合音效识别新的菜单类型 |
| `assets/.../big_icebox_gui.png` | 108 格 GUI 贴图（程序生成，配色与原版冷汗 GUI 一致） |

### 为什么不用 Mixin 改 GUI？

NeoForge 的 `@Shadow` 要求成员**声明在目标类自身**，而 `imageWidth` / `menu` / `leftPos` 等字段都声明在 vanilla 的 `AbstractContainerScreen` 里，不在 `IceboxScreen` 里——直接 `@Shadow` 会在启动时抛
`@Shadow field imageWidth was not located in the target class ... IceboxScreen`。

同时 NeoForge 的 `RegisterMenuScreensEvent` **不允许重复注册**同一个菜单类型（会抛 `IllegalStateException`），所以不能覆盖冷汗的冰菜单类型。

因此本模组改用：自有菜单类型 + 自有 Screen 类（继承冷汗的 `AbstractHearthScreen`，保留燃料仪表、供电指示和粒子开关），完全绕开上述两个限制。

## 构建

前置：JDK 21。

```powershell
git clone https://github.com/chessMCman/ColdSweatAutomation.git
.\gradlew build
```

### 依赖坐标说明

`build.gradle` 里用 `compileOnly files("libs/ColdSweat-2.4.4.jar")` 引用冷汗，jar 已提交到仓库的 `libs/` 目录，构建不依赖外部 Maven 镜像。

> NeoForge 21.1 不在构建时做重混淆：Mixin 类随 jar 带 Mojang 映射，由 NeoForge 运行时在加载阶段重映射（冷汗本身也是如此），所以直接依赖裸 jar 即可，无需额外重映射。

## 已知限制

- Mixin 按 Cold Sweat 2.4.4 编写，升级冷汗后若版本不兼容可能注入失败（`defaultRequire=1` 时会直接崩溃提示）。
- 旧版本存档里的冰箱物品栏（9 格）会继续存在，新增的 108 格从空位开始。

## 免责声明

本模组通过 Mixin 修改 Cold Sweat 2.4.4 的内部实现。
