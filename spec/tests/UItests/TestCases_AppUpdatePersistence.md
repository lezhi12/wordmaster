# WordMaster 覆盖安装数据持久性测试流程

## 概述

本测试验证 WordMaster 应用在覆盖安装后，用户数据（单词、等级、复习时间等）是否完整保留。

***

## ⚠️ 前置条件检查

执行本测试前，必须确保：

1. **测试用例存在性检查**：
   - 测试用例 `E2EDatabaseVerificationTest.tc26_appUpdateDataPersistenceVerification` 必须存在，位置（相对于项目根目录）：
     `app/src/androidTest/java/com/example/wordmaster/E2EDatabaseVerificationTest.kt`
   - 如果此测试用例不存在，**停止测试并报错**
2. **模型能力检查**：
   - 必须使用支持视觉对比的 AI 模型来执行此测试
   - 如果不支持视觉能力，**停止测试并报错**
3. **环境检查**：
   - 确保测试设备/模拟器已连接且 adb 可用
   - 确保有可用于覆盖安装的 APK 文件

4.截图对比失败，即报测试失败，不要再去做数据库对比。一切以截图为准。

***

## 完整测试流程

### 第 0 阶段：

### 第 1 阶段：准备测试数据（通过 TC-26 自动化）

执行以下步骤准备测试数据：

1. 安装主应用和测试应用：

```bash
# 在项目根目录下执行
./gradlew installDebug installDebugAndroidTest
```

1. 用 am instrument 直接运行 TC-26 测试（避免 connectedAndroidTest 卸载应用）：

```bash
adb shell am instrument -w -e class "com.example.wordmaster.E2EDatabaseVerificationTest#tc26_appUpdateDataPersistenceVerification" "com.example.wordmaster.test/androidx.test.runner.AndroidJUnitRunner"
```

这个测试会自动执行：

- UI 导入 5 个单词
- UI 复习前两个单词（第一个"记住了"，第二个"忘记了"）
- 截图保存

1. 从设备拉取 TC-26 生成的截图（覆盖安装前）：

```bash
adb pull /data/local/tmp/tc27_step1.png ./覆盖安装测试/tc26_before_install.png
```

1. 验证截图存在性：确保 `./覆盖安装测试/tc26_before_install.png` 文件存在且有效

### 第 2 阶段：强制停止应用

执行命令：

```bash
adb shell am force-stop com.example.wordmaster
```

### 第 3 阶段：覆盖安装 APK

执行命令（在项目根目录下执行）：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 第 4 阶段：冷启动应用并验证（视觉对比）

1. 冷启动应用：

```bash
adb shell am start -n com.example.wordmaster/.MainActivity
```

1. 等待 5 秒，让应用完全加载
2. 截取覆盖安装后的状态：

```bash
adb shell screencap -p /data/local/tmp/tc26_after_install.png
adb pull /data/local/tmp/tc26_after_install.png ./覆盖安装测试/tc26_after_install.png
```

1. **视觉对比验证**（必须使用视觉模型）：
   - 对比 `./覆盖安装测试/tc26_before_install.png` 和 `./覆盖安装测试/tc26_after_install.png`
   - 验证首页显示内容完全一致，包括：
     - `我的单词本` 标题
     - 所有单词完整显示（应该显示 4-5 个单词）
     - 单词文本、释义、等级与安装前一致（第一个单词 eloquent 等级应为 1，其他单词等级应为 0）

***

## 预期结果

✅ 所有单词完整保留在应用中
✅ 每个单词的等级与安装前一致
✅ 复习时间和上次复习时间正确
✅ 数据库无数据丢失

***

*创建日期：2026-05-23*
*更新日期：2026-05-24*
*更新内容：修正测试执行方式，改用 am instrument 避免应用被卸载；*
