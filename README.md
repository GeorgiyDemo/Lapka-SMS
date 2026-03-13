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

Lapka SMS is a full-featured SMS app with built-in message encryption. Encrypted messages are encoded using steganography — they can look like Cyrillic text or Russian words to anyone intercepting them. The primary adversary is the **mobile operator** who can read SMS content in transit.

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
| Russian Words | `молоко дерево книга` | Looks like natural language |

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
- 40 languages

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

### 2. Set up encryption

1. Open **Settings** → **Encryption Key Settings**
2. Tap **Generate Key** to create an AES-256 key
3. Share the key with your contact via **QR code** (meet in person or use a secure channel)
4. Verify the **key fingerprint** matches on both devices
5. Choose an **encoding scheme** (Base64 / Cyrillic Base64 / Russian Words)

### 3. Send encrypted messages

Just send messages as usual. If encryption is enabled for a conversation, messages are encrypted and encoded automatically. Incoming encrypted messages are decrypted transparently.

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
