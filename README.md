<p align="center">
  <img src="assets/logo.png" width="150" alt="Lapka SMS">
</p>

<h1 align="center">Lapka SMS</h1>

<p align="center">
  <b>Encrypted SMS messenger for Android</b><br>
  SMS encryption via steganography — your messages look like ordinary text
</p>

<p align="center">
  <a href="https://github.com/GeorgiyDemo/Lapka-SMS/actions"><img src="https://github.com/GeorgiyDemo/Lapka-SMS/actions/workflows/android.yml/badge.svg" alt="Build"></a>
  <a href="https://github.com/GeorgiyDemo/Lapka-SMS/releases"><img src="https://img.shields.io/github/v/release/GeorgiyDemo/Lapka-SMS?label=release" alt="Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPLv3-blue" alt="License"></a>
  <img src="https://img.shields.io/badge/Android-6.0%2B-green" alt="Android 6.0+">
</p>

<p align="center">
  <a href="README_RU.md">Русская версия</a> · <a href="README_FA.md">نسخه فارسی</a> · <a href="PROTOCOL.md">Protocol</a>
</p>

---

## What is this?

Lapka SMS is a full-featured SMS app with built-in message encryption. Encrypted messages are encoded using steganography — they look like English or Russian text to anyone intercepting them. The primary adversary is the **mobile operator** who can read SMS content in transit.

> **Both parties need Lapka SMS** with the same encryption key to exchange encrypted messages. Unencrypted SMS works with any phone.

## Features

### Encryption ([protocol details](PROTOCOL.md))
- **AES-256-GCM** authenticated encryption with HKDF-SHA256 key derivation
- **Replay protection** — rejects old and replayed messages
- **Message length hiding** — padding prevents traffic analysis

### Steganography schemes
| Scheme | Output example | Best for |
|---|---|---|
| Base64 | `dGVzdA==` | Universal, compact |
| Cyrillic Base64 | `дГВздА==` | Blends with Cyrillic text |
| Russian Words | `молоко дерево книга` | Looks like Russian text |
| English Words | `Drawcut Foussa Miranda` | Looks like English text |

### Key management
- **Per-conversation keys** — different key for each contact
- **Global key** — fallback key for all conversations
- **QR code sharing** — scan to exchange keys
- **SHA-256 fingerprint** — verify key authenticity
- **Android Keystore** — hardware-backed key storage
- **EncryptedSharedPreferences** — keys encrypted at rest

### Privacy & security
- **FLAG_SECURE** — hide app content in task switcher (configurable)
- **Auto-delete encrypted messages** — configurable timer
- **SMS for reset** — receive a predefined SMS to erase all encryption keys and reset encryption settings. Messages stay, but become undecryptable
- **No analytics, no tracking**
- **Encrypted Realm database**

### SMS app
- Material Design UI with customizable themes
- Night mode (auto/manual/system)
- Per-conversation notification settings
- Delayed sending
- Delivery reports
- Dual SIM support
- Swipe actions
- 38 languages

## Threat Model

| Protected | Not protected |
|---|---|
| Message content (AES-256-GCM) | Communication metadata (who, when, how often) |
| Message length (padding) | The fact that both parties use Lapka SMS |
| Key material at rest (Keystore + EncryptedSharedPreferences) | Physical access to unlocked device |
| App content in task switcher (FLAG_SECURE) | Recipient's device security |

## Getting Started

### 1. Install

Download the latest APK from [Releases](https://github.com/GeorgiyDemo/Lapka-SMS/releases) and install it. Set as default SMS app when prompted.

> **Google Play Protect may block installation** because the app is not distributed through Google Play. This is a false positive — the app is open-source and contains no malware. To install:
>
> 1. If you see **"App blocked for your protection"** — you need to temporarily disable Play Protect:
>    - Open **Google Play** → tap your **profile icon** → **Play Protect** → **Settings** (gear icon) → turn off **"Scan apps with Play Protect"**
>    - Install the APK
>    - Re-enable Play Protect after installation
> 2. If you see **"Default SMS app request denied"** after installation — Android 13+ restricts sideloaded apps from becoming the default SMS handler. To fix:
>    - Go to **Android Settings** → **Apps** → find **Lapka SMS** → tap **⋮** (three dots, top right) → **"Allow restricted settings"**
>    - Now open Lapka SMS and accept the default SMS app prompt

### 2. Set up encryption

Open a conversation → tap **⋮** menu → **Details** → **Encryption key**.

1. Enable the **Encryption key** toggle
2. Tap **Generate new key** — a random AES-256 key is created
3. Share the key with your contact via **QR code** (meet in person) or copy it via a secure channel. **Never send the key via regular SMS!**
4. Verify the **emoji fingerprint** matches on both devices
5. Scroll down and choose an **encoding scheme**

<p float="left">
  <img src="assets/guide/generate_key.png" width="300">
  <img src="assets/guide/encoding.png" width="300">
</p>

Available encoding schemes:

| Scheme | Looks like |
|---|---|
| Base64 | `dGVzdA==` — random characters |
| Cyrillic Base64 | `дГВздА==` — Cyrillic random characters |
| Russian Words | `молоко дерево книга` — Russian text |
| English Words | `Drawcut Foussa Miranda` — English text |

### 3. Send encrypted messages

The **lock icon** 🔒 next to each message indicates encryption is active. Just type and send — messages are encrypted automatically. Incoming encrypted messages are decrypted transparently.

<p float="left">
  <img src="assets/guide/encryption.png" width="300">
  <img src="assets/guide/default_messenger.png" width="300">
</p>

*Left: Lapka SMS (decrypted view) · Right: how it looks in a regular SMS app*

## Building from source

Requires **JDK 17**.

```bash
git clone https://github.com/GeorgiyDemo/Lapka-SMS.git
cd Lapka-SMS
./gradlew assembleDebug
```

## Architecture

```
presentation/   Android UI layer (Activities, Conductor Controllers, ViewModels)
domain/          Business logic, interactors, models
data/            Repositories, receivers, Realm persistence
common/          Shared utilities
psms-lib/        Encryption library (AES-GCM, HKDF, steganography encoders)
android-smsmms/  Legacy MMS/SMS framework
```

Key patterns:
- **Conductor** for navigation (Controllers inside Activities)
- **Dagger 2** for dependency injection
- **RxJava 2** + AutoDispose for reactive streams
- **Realm** for encrypted database storage

## How it differs from upstream

Lapka SMS is a fork of [Partisan-SMS](https://github.com/wrwrabbit/Partisan-SMS) (itself a fork of [QKSMS](https://github.com/moezbhatti/qksms)). Changes in Lapka SMS:

- Upgraded encryption protocol to v3 with new steganography encoders
- Upgraded dependencies (Dagger 2.52, Glide 4.16, Kotlin 1.9, compileSdk 35)
- Encrypted database storage (Realm encryption via Android Keystore)
- EncryptedSharedPreferences for key material
- Key fingerprint verification
- In-app language selector
- Security hardening (network security config, private file logging, FLAG_SECURE)
- CI/CD pipeline
- Modernized codebase (deprecated API cleanup, AndroidX migration)

## License

[GNU General Public License v3.0](LICENSE)
