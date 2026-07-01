# 🎵 AuraMusicFX

A premium, borderless, and highly customizable modern desktop music player built with **JavaFX 21** and **Gradle**. AuraMusicFX brings a sleek, glassmorphic design and rich features to your desktop audio experience, featuring smooth micro-animations, theme support, metadata extraction, and synchronized lyrics support.

---

## ✨ Features

- **📺 Immersive Visuals & Borderless Window**: A customized, transparent, and modern borderless UI with a dark aesthetic, custom title bars, and fluid CSS-based transition effects.
- **🎧 Audio playback Engine**: High-fidelity audio playback supporting MP3, FLAC (via `jflac-codec`), WAV, and other standard formats.
- **📂 Smart Library Management**: Scans directories, extracts metadata (artist, album, year, and embedded cover art) via `jaudiotagger`, and indexes your music library efficiently.
- **🎤 Synchronized Lyrics**: Full support for `.lrc` files, parsing and scrolling synchronized lyrics line-by-line.
- **🔄 Multiple View Modes**:
  - **Home Dashboard**: Quick access to your tracks, recent albums, and favorites.
  - **Album Detail View**: Dedicated views for albums with high-res cover art.
  - **Full Screen Immersive Player**: Focus mode for distraction-free listening.
  - **Mini Player Window**: A compact, floating widget for easy control while multi-tasking.
  - **Playlist Manager**: Create, customize, and edit local playlists.
- **⚙️ Preferences**: Configurable library paths, audio options, and visual settings.

---

## 🛠️ Technology Stack

- **Language**: Java 21+
- **UI Framework**: JavaFX 21
- **Build Automation**: Gradle
- **Key Dependencies**:
  - `net.jthink:jaudiotagger:3.0.1` - Professional audio metadata reader/writer.
  - `com.google.code.gson:gson:2.10.1` - Fast JSON serialization for playlists and preferences.
  - `org.jflac:jflac-codec:1.5.2` - FLAC audio format support.

---

## 🚀 Getting Started

### Prerequisites

Make sure you have the following installed:
- **Java Development Kit (JDK) 21** or higher.
- **Git** (for cloning the repository).

### Cloning the Repository

```bash
git clone https://github.com/YOUR_USERNAME/AuraMusicDesktop.git
cd AuraMusicDesktop
```

### Running the Application

You can easily run the application using the Gradle wrapper provided in the project:

```bash
# On Windows
gradlew.bat run

# On macOS / Linux
./gradlew run
```

### Building the Distribution

To package the application:

```bash
# Compile and build
gradlew assemble
```

### Packaging and Installers (Windows)

AuraMusicFX is configured with custom `jpackage` tasks to build standalone Windows installers (which bundle their own lightweight Java Runtime Environment, meaning end-users do not need Java installed on their machine):

```bash
# Generate .exe installer
gradlew packageExe

# Generate .msi installer
gradlew packageMsi
```

The compiled installers will be generated under the `build/installer/` directory.

---

## 📁 Project Structure

```
AuraMusicDesktop/
├── .gitignore
├── build.gradle          # Gradle project configuration and dependencies
├── settings.gradle       # Gradle settings
├── gradlew / gradlew.bat # Gradle wrappers
└── src/
    └── main/
        ├── java/
        │   └── aura/
        │       └── music/
        │           ├── Main.java         # Application Entrypoint & Stage setup
        │           ├── Launcher.java     # Main Launcher class
        │           ├── audio/            # Audio engine and playback control
        │           ├── library/          # Media indexing and tag extraction
        │           ├── lyrics/           # LRC parser and synchronized lyrics structures
        │           ├── model/            # Tracks, Albums, and Playlists domain models
        │           ├── theme/            # Accent color configurations
        │           ├── ui/               # Views, Custom Components, and Animations
        │           └── viewmodel/        # MVVM view models to decouple UI logic
        └── resources/
            └── aura/
                └── music/
                    └── styles.css        # Global CSS stylesheet for AuraMusicFX
```

---

## 🎨 Customizing the Interface

All styles are governed by [styles.css](file:///src/main/resources/aura/music/styles.css). You can adjust theme colors, font families, and container borders to customize your local player look.

---

## 🤝 Contributing

Contributions are welcome! If you find bugs or want to add new features, please open an issue or submit a pull request.
