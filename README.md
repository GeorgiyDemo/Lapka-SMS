# Lapka SMS - Encrypted SMS Messenger

Lapka SMS is a fork of [Partisan-SMS](https://github.com/wrwrabbit/Partisan-SMS) with modernized dependencies, updated Android APIs, and significantly improved encryption.

## Origin & Attribution

This project is based on the following open-source projects:

- **Partisan-SMS** by [Cyber Partisans](https://github.com/wrwrabbit/Partisan-SMS) - encrypted SMS via steganography
- **QKSMS** by [Moez Bhatti](https://github.com/moezbhatti/qksms) - the original open-source SMS app

All original copyright notices and license terms are preserved.

## Changes from Partisan-SMS

### Encryption (v2 protocol)

- **AES-256-GCM** instead of AES-CFB with 4-byte IV — authenticated encryption with 12-byte random nonce and 128-bit auth tag
- **HKDF key derivation** (RFC 5869) — separate encryption and MAC keys derived from master key
- **Replay protection** — 4-byte timestamp in payload, 48-hour acceptance window
- **PKCS7 padding** — message length hidden from operator (padded to 16-byte blocks)
- **Constant-time HMAC comparison** — prevents timing side-channel attacks
- **Encrypted key storage** — global encryption key stored in Android EncryptedSharedPreferences (AES-256-GCM, backed by Android Keystore)
- **Key fingerprint** — SHA-256 fingerprint displayed in key settings for out-of-band verification

### SMS-only focus

- Removed MMS/attachments support — the app is SMS-only for simplicity and security
- Removed backup/restore functionality
- Removed scheduled messages

### Codebase

- `psms-lib` module built from source instead of pre-compiled AAR
- Updated 15+ dependencies to current versions (Dagger, Glide, Timber, Material, etc.)
- Replaced deprecated Android APIs (onBackPressed, startActivityForResult, Handler without Looper, etc.)
- Removed jcenter() repository dependency
- Added SDK version checks for PackageManager APIs
- Migrated to AndroidX ActivityResult APIs
- In-app language selector (40 languages)

## Threat Model

The primary adversary is the **mobile operator** who can read SMS content and metadata. Lapka SMS encrypts message content via steganography (encoded as Russian text, Base64, or Cyrillic Base64) so the operator sees only innocuous-looking messages.

**What is protected:**
- Message content (AES-256-GCM encryption)
- Message length (PKCS7 padding)
- Key material at rest (EncryptedSharedPreferences)

**What is NOT protected:**
- Communication metadata (who messages whom, when, how often)
- The fact that both parties use Lapka SMS (if operator inspects app installs)

## Building

```
./gradlew assembleDebug
```

Requires JDK 17.

## License

Lapka SMS is released under the **GNU General Public License v3.0 (GPLv3)**, the same license as the original Partisan-SMS and QKSMS projects. The full license text can be found in the `LICENSE` file.

In compliance with GPLv3:
- The complete source code of this fork is available in this repository
- All modifications are documented in the git history
- Original copyright notices from Partisan-SMS and QKSMS are preserved in all source files

## Original Projects

- Partisan-SMS: https://github.com/wrwrabbit/Partisan-SMS
- QKSMS: https://github.com/moezbhatti/qksms
