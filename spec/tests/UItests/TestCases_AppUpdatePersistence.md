# WordMaster 覆盖安装数据持久性测试流程

## 概述
本测试验证 WordMaster 应用在覆盖安装后，用户数据（单词、等级、复习时间等）是否完整保留。

---

## 前置准备
1. 确保 WordMaster 应用已在模拟器中安装并正常运行
2. 确保有可用于覆盖安装的 APK 文件
3. 确保测试设备/模拟器已连接且 adb 可用

---

## 完整测试流程

### 第 1 阶段：批量导入单词（UI 自动化）
1. 启动 WordMaster 应用
2. 点击右下角 `+` 按钮
3. 点击 `批量导入` 菜单
4. 在 JSON 输入框中粘贴以下内容：
```json
[{"word":"eloquent","definition":"雄辩的，有说服力的","example":"She gave an eloquent speech at the ceremony."},{"word":"resilient","definition":"有弹性的，能迅速恢复的","example":"Children are often more resilient than adults."},{"word":"pragmatic","definition":"务实的，实用主义的","example":"We need a pragmatic approach to solve this problem."},{"word":"ambiguous","definition":"模棱两可的，含糊不清的","example":"The statement was deliberately ambiguous."},{"word":"tenacious","definition":"坚韧不拔的，顽强的","example":"She was tenacious in pursuing her goals."}]
```
5. 点击 `解析` 按钮
6. 等待解析完成，确保预览列表显示 5 个单词
7. 点击 `导入 5 个单词` 按钮
8. 等待返回首页，确保首页显示：
   - `您有 5 个单词需要复习` 提示
   - 5 个单词在"我的单词本"列表中

### 第 2 阶段：复习部分单词（UI 自动化）
1. 点击 `复习单词` 卡片
2. 等待进入复习页面
3. 点击第一个单词的 `记住了` 按钮
4. 点击第二个单词的 `忘记了` 按钮
5. 点击左上角 `←` 返回按钮
6. 验证返回首页

### 第 3 阶段：保存状态（验证准备）
1. 确认首页显示所有 5 个单词
2. 记录或截屏当前状态（可选）

### 第 4 阶段：强制停止应用
执行命令：
```bash
adb shell am force-stop com.example.wordmaster
```

### 第 5 阶段：覆盖安装 APK
执行命令（替换为你的 APK 路径）：
```bash
adb install -r /path/to/WordMaster/app/build/outputs/apk/debug/app-debug.apk
```

### 第 6 阶段：冷启动应用并验证（UI 验证）
1. 点击应用图标冷启动 WordMaster
2. 验证首页显示：
   - `我的单词本` 标题
   - 所有 5 个单词完整显示
   - 单词文本、释义、等级与安装前一致

---

## 可调用的 UI 自动化代码

### A. 阶段 1-3：导入、复习、保存状态（自动化）
调用测试：
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.wordmaster.E2EDatabaseVerificationTest#tc26_appUpdateDataPersistenceVerification
```

这个测试会执行：
1. UI 导入 5 个单词
2. UI 复习前两个单词
3. 保存状态快照
4. 杀掉并重启 Activity（模拟冷启动）
5. 验证所有数据完整保留

**注意**：这个测试用例在现有代码库中已完整实现，位于：
`app/src/androidTest/java/com/example/wordmaster/E2EDatabaseVerificationTest.kt:tc26_appUpdateDataPersistenceVerification`

---

### B. 纯 UI 导入测试（独立）
如果只需要执行 UI 导入，可以单独调用：
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.wordmaster.E2EImportTest#tc01_standardJsonParseAndImport
```

---

## 手动验证步骤（可选）
如果自动化测试不可用，可以手动执行上述所有步骤。

## 预期结果
✅ 所有 5 个单词完整保留在应用中
✅ 每个单词的等级与安装前一致
✅ 复习时间和上次复习时间正确
✅ 数据库无数据丢失

---
*创建日期：2026-05-23*
