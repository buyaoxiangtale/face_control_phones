# FaceControl - Android Face Gesture Assistant 🚀

[中文版](./README_CN.md) | English

**FaceControl** is an innovative Android open-source application that leverages computer vision to enable users to control their devices through simple facial gestures such as blinking, nodding, and shaking.

This project aims to provide a new way of interaction for people with disabilities, or to make device operation accessible when manual handling is inconvenient (e.g., washing hands, cooking).

---

## ✨ Key Features

- **Real-time Face Tracking**: Powered by **Google MediaPipe Face Mesh**, supporting 468 3D facial landmarks detection in real-time.
- **Continuous Background Monitoring**: Combines **CameraX** with a **Foreground Service**, ensuring the app responds even when in the background or when the screen is locked.
- **Global Gesture Simulation**: Utilizes the **Accessibility Service API** to simulate swipe and click gestures across any third-party apps (e.g., TikTok, Browser) without requiring Root access.
- **Anti-Mistouch Logic**: Built-in EAR (Eye Aspect Ratio) state machine with support for "Double Blink" triggering to distinguish between natural blinks and control commands.

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Vision Computing**: [MediaPipe Tasks Vision](https://developers.google.com/mediapipe)
- **Camera Support**: CameraX (Camera2)
- **System Interface**: Android Accessibility Service API
- **Background Architecture**: Lifecycle-aware Foreground Service

---

## 📸 Gesture Mapping (MVP Stage)

| Gesture | Logic | Simulated Action | Use Case |
| :--- | :--- | :--- | :--- |
| **Double Blink** | Two valid blinks within 800ms | **Swipe Up** | Scrolling TikTok / Web pages |
| **Head Shake** | Nose horizontal displacement | **Swipe Left** | Page turning / Dismissing notifications |
| **Head Nod** | Pitch angle detection (Reserved) | **Screen Click** | Confirm / Like |

---

## 🚀 Quick Start

### Requirements
- Android Device (Running Android 8.0+ / API 26+)
- Physical device for testing (Emulators do not support hardware camera acceleration well)

### 1. Clone and Build

```bash
git clone https://github.com/your-id/FaceControl-Android.git
```

### 2. Model File (Crucial)
Due to size limits, the MediaPipe model file is not included in the repository. Please follow these steps:
1. Download [face_landmarker.task](https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task).
2. Place the file into the `app/src/main/assets/` folder of your project.

### 3. Permissions
After running the app, please enable:
- **Camera Permission** (For face recognition)
- **Overlay Permission** (For foreground service operation)
- **Accessibility Service** (Find `FaceControl Service` in System Settings and turn it on)

---

## 🏗️ Project Structure

```
com.example.camera
├── FaceAnalyzer.kt              # Vision Core (EAR calculation, gesture detection)
├── FaceAccessibilityService.kt  # Execution Engine (Gesture simulation)
├── FaceControlForegroundService.kt # Background Orchestrator
├── MainActivity.kt              # User Guidance & Permission UI
└── ui/theme/                    # Compose UI Styles
```

---

## 📅 Roadmap

- [x] Blink detection based on EAR algorithm
- [x] Support for "Double Blink" to avoid accidental triggers
- [x] Head shake detection logic
- [ ] Add Mouth Open trigger
- [ ] User-customizable [Gesture -> Command] mapping
- [ ] Sensitivity adjustment and haptic feedback

---

## ⚠️ Privacy Policy

Privacy is our priority:
- All vision analysis is performed **locally on-device**. No images or videos are uploaded to any server.
- Camera access is used solely for gesture recognition and is not recorded or stored.
- Accessibility Service is used only to simulate predefined gesture commands.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

**If you like this project, please give it a Star ⭐️! Contributions via Issues or PRs are welcome.**
