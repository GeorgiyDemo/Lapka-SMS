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
</p>

---

## Features

- **AES-256-GCM encryption** with HKDF key derivation and replay protection
- **Steganography** — encrypted messages encoded as Russian text, Base64, or Cyrillic Base64
- **Per-conversation encryption keys** with QR code sharing
- **Key fingerprint verification** (SHA-256)
- **Encrypted key storage** via Android Keystore
- **In-app language selector** (40 languages)
- **SMS-only** — lightweight, no MMS bloat
- **Themed icons** support (Android 13+)

## Download

Download the latest APK from [Releases](https://github.com/GeorgiyDemo/Lapka-SMS/releases).

## Building

```bash
./gradlew assembleDebug
```

Requires **JDK 17**.

## Threat Model

The primary adversary is the **mobile operator** who can read SMS content. Lapka SMS encrypts message content via
steganography so the operator sees only innocuous-looking messages.

| Protected                                         | Not Protected                                 |
|---------------------------------------------------|-----------------------------------------------|
| Message content (AES-256-GCM)                     | Communication metadata (who, when, how often) |
| Message length (PKCS7 padding)                    | The fact that both parties use Lapka SMS      |
| Key material at rest (EncryptedSharedPreferences) |                                               |

## Encryption Protocol (v2)

- **AES-256-GCM** — authenticated encryption with 12-byte random nonce
- **HKDF** (RFC 5869) — separate encryption and MAC keys from master key
- **Replay protection** — 4-byte timestamp, 48-hour acceptance window
- **PKCS7 padding** — hides message length
- **Constant-time HMAC** — prevents timing attacks

## Credits

Based on open-source projects:

- [Partisan-SMS](https://github.com/wrwrabbit/Partisan-SMS) by Cyber Partisans
- [QKSMS](https://github.com/moezbhatti/qksms) by Moez Bhatti

## License

[GNU General Public License v3.0](LICENSE)
