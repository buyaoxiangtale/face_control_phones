# FaceControl - Android 面部手势辅助工具 🚀

中文版 | [English](./README.md)

**FaceControl** 是一款创新的 Android 开源应用，它利用计算机视觉技术，让用户仅通过面部动作（如眨眼、点头、摇头）即可实现对手机的全局控制。

该项目旨在为残障人士提供一种全新的交互方式，或者在不方便手动操作（如洗手、下厨）时，让手机操作变得触手可及。

---

## ✨ 核心特性

- **实时面部追踪**：基于 **Google MediaPipe Face Mesh**，支持 468 个面部 3D 关键点实时检测。
- **后台持续监听**：结合 **CameraX** 与 **Foreground Service**（前台服务），即使 App 退到后台或屏幕锁定，依然能持续响应。
- **全局手势模拟**：利用 **Accessibility Service**（辅助功能），无需 Root 权限即可在任何第三方应用（如抖音、浏览器）中模拟滑动和点击。
- **防误触逻辑**：内置 EAR（眼径比）状态机判定，支持“双眨眼”触发，区分生理眨眼与控制指令。

---

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **视觉计算**: [MediaPipe Tasks Vision](https://developers.google.com/mediapipe)
- **相机支持**: CameraX (Camera2)
- **系统接口**: Android Accessibility Service API
- **后台架构**: Lifecycle-aware Foreground Service

---

## 📸 动作映射表 (MVP 阶段)

| 面部动作 | 判定逻辑 | 模拟手势 | 典型应用场景 |
| :--- | :--- | :--- | :--- |
| **双击眨眼** | 800ms 内两次有效闭眼 | **向上滑动** | 刷抖音/短视频、网页翻页 |
| **左右摇头** | 鼻尖偏移比例判定 | **向左滑动** | 电子书翻页、清除通知 |
| **点头** | 姿态角检测 (预留) | **屏幕点击** | 确定/点赞 |

---

## 🚀 快速开始

### 环境要求
- Android 设备 (运行系统 Android 8.0+ / API 26+)
- 真机测试（模拟器无法提供硬件摄像头加速）

### 1. 克隆并编译

```bash
git clone https://github.com/your-id/FaceControl-Android.git
```

### 2. 放置模型文件 (重要)
由于体积限制，MediaPipe 模型文件不包含在仓库中。请按照以下步骤操作：
1. 下载 [face_landmarker.task](https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task)。
2. 将文件放入项目的 `app/src/main/assets/` 文件夹下。

### 3. 权限授予
运行 App 后，请依次开启：
- **相机权限** (用于面部识别)
- **悬浮窗权限** (用于前台服务运行)
- **辅助功能服务** (在系统设置中找到 `FaceControl Service` 并开启)

---

## 🏗️ 项目架构

```
com.example.camera
├── FaceAnalyzer.kt              # 视觉处理核心 (EAR计算、手势判定)
├── FaceAccessibilityService.kt  # 手势执行引擎 (模拟点击/滑动)
├── FaceControlForegroundService.kt # 后台调度中心 (管理相机与服务声明周期)
├── MainActivity.kt              # 用户引导与权限管理界面
└── ui/theme/                    # Compose UI 样式定义
```

---

## 📅 开发路线图 (Roadmap)

- [x] 基于 EAR 算法的眨眼判定逻辑
- [x] 支持“双眨眼”避免误触
- [x] 摇头动作识别逻辑
- [ ] 增加张嘴 (Mouth Open) 触发动作
- [ ] 支持用户自定义 [面部动作 -> 手势指令] 的映射表
- [ ] 增加灵敏度调节与震动反馈

---

## ⚠️ 隐私说明

本项目非常重视隐私保护：
- 所有视觉分析均在**本地设备**实时进行，不上传任何图像或视频到服务器。
- 相机权限仅用于姿态识别，不进行保存或录制。
- 辅助功能仅用于模拟预设的手势指令。

---

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 协议。

---

**如果你喜欢这个项目，欢迎点一个 Star ⭐️！有问题请提交 Issue 或 PR。**
