# 🎵 Aura Music Desktop

<div align="center">

![Aura Music Banner](https://img.shields.io/badge/Aura--Music-Desktop-7C3AED?style=for-the-badge&logo=java&logoColor=white)
![Java Version](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21-276DC3?style=for-the-badge&logo=java&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.x-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-000000?style=for-the-badge)

*A premium, borderless, glassmorphic modern desktop audio experience built with JavaFX 21 & LibVLC/VLCJ.*

</div>

---

## 🌟 Overview

**Aura Music Desktop** is a modern, high-performance desktop music player featuring a sleek glassmorphic dark interface, fluid micro-animations, rich audio playback support, synchronized LRC lyrics, custom theme accents, YouTube/online integration, and native Windows System Media Transport Controls (SMTC).

---

## 🚀 Feature Status & Roadmap

### ✅ Current Features (Available Now)
- 🎧 **High-Fidelity Audio Engine**: Powered by LibVLC/VLCJ & JavaFX, supporting MP3, FLAC, WAV, AAC, OGG, and streaming audio.
- 🎨 **Modern Glassmorphic UI**: Transparent borderless windows, customizable dark themes, interactive glass components, and fluid UI micro-animations.
- 🎤 **Synchronized LRC Lyrics**: Real-time line-by-line synced lyrics display with smooth scrolling and dynamic preview.
- 📺 **Multiple View Modes**:
  - **Home Dashboard**: Quick access to recent plays, favorites, daily special recommendations, and top tracks.
  - **Full Screen Immersive Player**: Distraction-free listening mode with interactive controls and background visuals.
  - **Mini Player**: Lightweight floating desktop widget for quick playback controls while multi-tasking.
  - **Album & Playlist Manager**: Detailed album views, metadata tagging, and custom playlist creation.
- 🌐 **Online Streaming & YouTube Integration**: Search, stream, and play YouTube audio tracks seamlessly.
- 🎛️ **Windows SMTC Integration**: Control playback via native Windows media keys and system overlays.
- 💾 **Smart State & Preference Persistence**: Saves last played history, volume preferences, custom library directories, and theme settings across restarts.

---

### 🏗️ In Development (Work in Progress)
- 📊 **Dynamic Audio Visualizer & Spectrum**: Real-time reactive audio frequency spectrum analyzer with custom visual modes.
- 🎚️ **Equalizer & DSP Effects**: Built-in multi-band equalizer with presets (Bass Boost, Pop, Rock, Vocal) and custom gain controls.
- 📝 **Interactive Lyrics Editor**: Built-in LRC editor to adjust offset timestamps and create custom synced lyrics directly in-app.
- 🔄 **Cloud & Local Auto-Sync**: Background directory watcher to automatically reflect file changes in the local music library instantly.

---

### 🔮 Future Roadmap (Planned Features)
- 🌐 **Cross-Platform Global Media Keys**: Native system media control support for macOS (MPRemoteCommandCenter) and Linux (MPRIS2).
- ☁️ **Spotify & Last.fm Scrobbling**: Real-time music scrobbling, scrobble caching, and top tracks stats integration.
- 📱 **Remote Control Companion App**: Control desktop playback, queue, and volume from your mobile phone on the local Wi-Fi network.
- 🎨 **Custom Theme Builder**: Full theme engine support allowing users to design and export custom colors, background blurs, and CSS accents.
- 🔊 **Gapless Playback & Crossfade**: Smooth track transition options with adjustable crossfade durations.

---

## 🛠️ Tech Stack & Key Libraries

| Component | Technology / Library |
| :--- | :--- |
| **Language** | Java 21 (LTS) |
| **UI Framework** | JavaFX 21 |
| **Audio Engine** | VLCJ / LibVLC (`uk.co.caprica:vlcj`) |
| **Metadata Tagging** | Jaudiotagger (`net.jthink:jaudiotagger:3.0.1`) |
| **FLAC Decoder** | JFLAC (`org.jflac:jflac-codec:1.5.2`) |
| **Persistence / JSON** | Gson (`com.google.code.gson:gson:2.10.1`) |
| **Build Tool** | Gradle |

---

## 💻 Getting Started

### Prerequisites
- **JDK 21** or higher installed.
- **Git** installed.
- *(Recommended)* **VLC Media Player** installed on system for LibVLC integration.

### Quick Start Commands

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/AuraMusicDesktop.git
cd AuraMusicDesktop

# Run application via Gradle Wrapper
# Windows:
gradlew.bat run

# macOS / Linux:
./gradlew run
```

### Packaging & Installers (Windows)

AuraMusic Desktop supports building standalone installers bundled with an embedded Java runtime:

```bash
# Build executable (.exe) installer
gradlew packageExe

# Build MSI installer
gradlew packageMsi
```
The output installers will be generated under `build/installer/`.

---

## 📁 Project Structure

```
AuraMusicDesktop/
├── build.gradle          # Dependencies and Gradle build configuration
├── settings.gradle       # Project settings
├── gradlew / gradlew.bat # Gradle wrapper scripts
└── src/main/
    ├── java/aura/music/
    │   ├── Launcher.java # Main application launcher entrypoint
    │   ├── Main.java     # Primary Stage & UI initialization
    │   ├── audio/        # Playback engine & LibVLC handler
    │   ├── library/      # Indexer & metadata extractor
    │   ├── lyrics/       # LRC lyrics parser & synchronizer
    │   ├── model/        # Data domain models (Track, Album, Playlist)
    │   ├── online/       # YouTube and online stream handlers
    │   ├── theme/        # Theme engine & accent managers
    │   ├── ui/           # Custom FX views, components & overlays
    │   ├── utils/        # System utilities & SMTC integration
    │   └── viewmodel/    # MVVM architecture view models
    └── resources/aura/music/
        └── styles.css    # Modern CSS stylesheet & glassmorphic theme rules
```

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check out existing issues or open a pull request.

---

<div align="center">
  <sub>Built with ❤️ using JavaFX 21</sub>
</div>
