# WordMaster 批量导入功能方案

## 设计决策

| # | 决策 | 选择 | 理由 |
|---|------|------|------|
| 1 | 数据格式 | JSON | 项目已有 kotlin-serialization，零额外依赖；主流大模型擅长输出 JSON |
| 2 | 页面方式 | 新建独立 ImportWordScreen | 手动添加和批量导入心智模型不同，独立页面空间充足 |
| 3 | 首页入口 | FAB 点击弹出 BottomSheet | 入口统一，扩展性好，符合 Material3 惯例 |
| 4 | 交互流程 | 单页式 | 操作快，粘贴即预览，反馈即时 |
| 5 | Prompt | 硬编码 | 保证格式稳定，MVP 阶段简单可靠优先 |
| 6 | 重复处理 | 预览时标记已存在 + 勾选框 | 用户有控制权，可视化反馈 |
| 7 | 解析失败 | 容错解析 + 错误提示 | 大模型常返回 ```json...``` 包裹，容错覆盖 90% 场景 |

## 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `NavigationKeys.kt` | 修改 | 添加 `ImportWord` 路由键 |
| `Navigation.kt` | 修改 | 添加 ImportWord 路由，FAB 改 BottomSheet |
| `HomeScreen.kt` | 修改 | FAB 改为弹出 BottomSheet 选择 |
| `ImportWordScreen.kt` | 新建 | 导入页面：prompt + 粘贴 + 预览 + 导入 |
| `WordMasterViewModel.kt` | 修改 | 添加批量导入 + 查重方法 |
| `DataRepository.kt` | 修改 | 添加批量导入 + 按单词查重方法 |
| `WordDao.kt` | 修改 | 添加按单词名查询 + 批量插入方法 |

## 页面布局（ImportWordScreen）

```
┌─────────────────────────────┐
│  ← 批量导入                  │  TopAppBar
├─────────────────────────────┤
│  ┌─────────────────────────┐│
│  │ 📋 Prompt（可复制）      ││  Prompt 卡片
│  │ 复制以下 prompt 给大模型  ││  默认折叠，点击展开
│  │ [复制 Prompt]            ││
│  └─────────────────────────┘│
│                             │
│  ┌─────────────────────────┐│
│  │ 粘贴大模型返回的 JSON    ││  粘贴输入区
│  │                         ││
│  └─────────────────────────┘│
│                             │
│  预览 (3 个单词)             │  预览列表
│  ┌─────────────────────────┐│
│  │ ☑ eloquent  雄辩的       ││  正常单词
│  │ ☑ ephemeral 短暂的       ││
│  │ ☐ ubiquitous 已存在      ││  重复单词标记
│  └─────────────────────────┘│
│                             │
│  [导入 2 个单词]             │  导入按钮
└─────────────────────────────┘
```

## 硬编码 Prompt

```
请将我给出的英文单词整理为 JSON 数组格式返回，每个单词包含以下字段：
- word: 英文单词
- definition: 中文释义
- example: 英文例句

请严格按以下格式返回，不要添加任何其他内容：
[
  {
    "word": "eloquent",
    "definition": "雄辩的，有说服力的",
    "example": "She gave an eloquent speech at the ceremony."
  }
]

单词列表：
```

## JSON 解析容错策略

1. 直接解析 `Json.decodeFromString`
2. 失败后尝试提取 ```json ... ``` 代码块内容
3. 再失败则显示错误信息

## 数据流

```
用户粘贴 JSON
    ↓
ImportWordScreen 解析 JSON → List<ParsedWord>
    ↓
对比已有单词列表 → 标记重复
    ↓
用户勾选确认
    ↓
ViewModel.addWords(List<Word>)
    ↓
Repository.addWords() → WordDao.insertAll()
```
