# Agnes AI Studio - Android 版

基于 [Agnes AI Studio](https://github.com/agnes-ai/agnes-ai-studio) 的 Android 客户端，封装 Agnes AI API，支持短剧一键生成。

## 功能

- 🖼 **文生图** - 通过文本描述生成高质量图片
- 🎨 **图生图** - 基于参考图片进行风格转换
- 🎬 **文生视频** - 通过文本描述生成视频
- 🖼→🎬 **图生视频** - 让静态图片动起来
- 🎭 **短剧生成** - 一键生成完整短剧（剧本→分镜→素材→视频）
- ⚙ **API 配置** - 手动配置 API Key 和地址

## 内置 API 地址

| 厂商 | 地址 |
|------|------|
| Agnes AI 国内站 | `https://api.agnes-ai.cn/v1` |
| Agnes AI 国际站 | `https://apihub.agnes-ai.com/v1` |
| DeepSeek | `https://api.deepseek.com/v1` |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| 豆包 | `https://ark.cn-beijing.volces.com/api/v3` |
| MiniMax | `https://api.minimaxi.com/v1` |
| OpenAI | `https://api.openai.com/v1` |

## 下载

### 方式一：从 GitHub Actions 下载（推荐）

1. 打开 [Actions](../../actions) 页面
2. 点击最新的构建任务
3. 在 Artifacts 中下载 `AgnesStudio-debug.zip`
4. 解压得到 APK 文件，安装到手机

### 方式二：自行构建

```bash
# 克隆项目
git clone https://github.com/你的用户名/agnes-android.git
cd agnes-android

# 构建 Debug APK
./gradlew assembleDebug

# 输出路径: app/build/outputs/apk/debug/app-debug.apk
```

## 使用方法

1. 安装 APK
2. 打开应用
3. 点击「API 配置」区域
4. 选择 API 地址（或输入自定义地址）
5. 输入 API Key
6. 点击「保存配置」
7. 选择模型
8. 输入提示词或选择预设主题
9. 点击「生成剧本」/「生成图片」/「生成视频」/「一键生成短剧」

## 获取 API Key

访问 [Agnes AI 平台](https://platform.agnes-ai.cn) 注册账号并获取免费 API Key。

## 项目结构

```
agnes-android/
├── .github/workflows/build.yml    # GitHub Actions 构建配置
├── app/
│   ├── build.gradle               # 应用构建配置
│   ├── proguard-rules.pro         # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/agnes/studio/
│       │   ├── MainActivity.java      # 主界面
│       │   ├── VideoActivity.java     # 视频播放
│       │   ├── ApiService.java        # API 调用
│       │   └── DramaGenerator.java    # 短剧生成
│       └── res/
│           ├── layout/                # 布局文件
│           ├── drawable/              # 背景样式
│           └── values/                # 颜色/字符串
├── build.gradle                 # 项目构建配置
├── settings.gradle
├── gradle.properties
└── README.md
```

## 技术栈

- **语言**: Java
- **网络**: OkHttp 4
- **JSON**: Gson
- **图片加载**: Glide
- **最低版本**: Android 7.0 (API 24)
- **目标版本**: Android 14 (API 34)

## License

MIT License
