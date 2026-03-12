# Lapka SMS Encryption Protocol

Version: **3** (`psms-lib`)

This document describes the encryption protocol used by Lapka SMS to protect SMS message content. The protocol provides authenticated encryption with replay protection and steganographic encoding.

## Overview

```
Plaintext
   │
   ▼
┌─────────────────────┐
│  Plain Data Encoder  │  Compress text into bytes (best encoder auto-selected)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│       Pack          │  Append channel ID + timestamp + metainfo + HMAC
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│    PKCS#7 Padding   │  Pad to 16-byte boundary
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│    AES-256-GCM      │  Encrypt with derived encryption key
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Encrypted Data      │  Encode ciphertext as Base64 / Cyrillic Base64 /
│ Encoder             │  Russian Words
└──────────┬──────────┘
           ▼
      SMS text
```

## Key Derivation

A single **master key** (128, 192, or 256 bits) is shared between parties. Two subkeys are derived using HKDF (RFC 5869) with HMAC-SHA256:

```
HKDF-Extract:
  salt = "k-sms-hkdf-v2" (fixed, public)
  IKM  = master_key
  PRK  = HMAC-SHA256(salt, IKM)

HKDF-Expand (encryption key):
  info   = "k-sms-v2-enc"
  length = 32 bytes
  enc_key = HKDF-Expand(PRK, info, 32)

HKDF-Expand (MAC key):
  info   = "k-sms-v2-mac"
  length = 32 bytes
  mac_key = HKDF-Expand(PRK, info, 32)
```

The salt and info strings are fixed protocol constants, not secrets. They ensure domain separation between the encryption and MAC keys.

## Plain Data Encoding

Before encryption, the plaintext message is compressed into a byte array using the most compact encoder available. The encoder is selected automatically based on the message content:

| Mode | ID | Description |
|---|---|---|
| SHORT_CP1251_PREFER_CYRILLIC | 0 | Compact CP1251 encoding, Cyrillic-optimized |
| SHORT_CP1251_PREFER_LATIN | 1 | Compact CP1251 encoding, Latin-optimized |
| CP1251 | 2 | Standard CP1251 encoding |
| UTF_8 | 3 | Standard UTF-8 (fallback for any text) |
| ASCII | 4 | 7-bit ASCII |
| HUFFMAN_CYRILLIC | 5 | Huffman coding optimized for Cyrillic text |
| HUFFMAN_LATIN | 6 | Huffman coding optimized for Latin text |

The encoder ID is stored in the MetaInfo byte (see below) so the receiver knows how to decode.

## Message Packing

The encoded text bytes are packed into a payload with metadata:

```
┌──────────────┬─────────────────┬───────────────┬──────────┬──────────────┐
│ encoded_text │ channel_id (4B) │ timestamp (4B)│ meta (1B)│ hmac (8B)    │
│ (variable)   │ (optional)      │               │          │              │
└──────────────┴─────────────────┴───────────────┴──────────┴──────────────┘
                                                            ▲
                                          HMAC covers ──────┘
                                    encoded_text + channel_id + timestamp
```

### Fields

**encoded_text** (variable length): Message text compressed by the plain data encoder.

**channel_id** (4 bytes, optional): Conversation channel identifier. Present only if the `isChannel` flag is set in MetaInfo. Little-endian uint32.

**timestamp** (4 bytes): Unix timestamp in seconds, little-endian uint32. Used for replay protection.

**MetaInfo** (1 byte): Bit-packed metadata byte:

```
Bit layout: [C][VVV][MMMM]

Bits 0-3 (MMMM): Plain data encoder mode (0-6)
Bits 4-6 (VVV):   Protocol version (currently 3)
Bit 7 (C):        Channel ID present flag (0 = no, 1 = yes)
```

**HMAC** (8 bytes): Truncated HMAC-SHA256 over `encoded_text + channel_id + timestamp`, computed with `mac_key`. Only the first 8 bytes of the full 32-byte HMAC are stored to save space in SMS.

### PKCS#7 Padding

After packing, the payload is padded to a multiple of 16 bytes using PKCS#7:

- Pad length = `16 - (payload_length % 16)`
- Pad bytes: each byte equals the pad length (1-16)
- Always adds at least 1 byte of padding

This hides the exact message length from traffic analysis.

## Encryption

The padded payload is encrypted using **AES-256-GCM**:

```
nonce = SecureRandom(12 bytes)
ciphertext || tag = AES-256-GCM(enc_key, nonce, padded_payload)
```

Wire format of encrypted data:

```
┌──────────────┬──────────────────────────┐
│ nonce (12B)  │ ciphertext + GCM tag     │
│              │ (tag is 16 bytes)        │
└──────────────┴──────────────────────────┘
```

Properties:
- **Nonce**: 12 bytes (96 bits), randomly generated via `SecureRandom` for each message
- **GCM tag**: 128 bits (16 bytes), provides authenticated encryption
- **Cipher**: `AES/GCM/NoPadding` (javax.crypto, hardware-accelerated on Android)

GCM provides both confidentiality and integrity. Any modification to the ciphertext, nonce, or tag will cause decryption to fail.

## Encrypted Data Encoding (Steganography)

The encrypted byte array is encoded into a text string for transmission via SMS. Three schemes are available:

### Base64 (Scheme 0)

Standard Base64 encoding. Compact but visually obvious as encoded data.

```
Input:  [0xDE, 0xAD, ...]
Output: "3q0t..."
```

### Cyrillic Base64 (Scheme 1)

Base64 with the Latin alphabet replaced by Cyrillic characters:

```
Latin:    ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=
Cyrillic: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя
```

The output looks like Cyrillic text to a casual observer.

### Russian Words / Text (Scheme 2)

Encodes encrypted bytes as natural-looking Russian text using a system of sub-encoders:

- **Words sub-encoders**: Map byte values to Russian words from dictionaries
- **DateTime sub-encoder**: Encodes values as date/time strings
- **Punctuation sub-encoder**: Encodes values as punctuation marks

The encoding treats the encrypted data as a large integer and performs mixed-radix decomposition across the available sub-encoders. A random front-padding byte is prepended if needed to ensure the integer maps cleanly to the available word space.

The output looks like a sequence of Russian words with natural punctuation and spacing. During decoding, up to 256 front-padding bytes are stripped to find the correct alignment.

## Replay Protection

Each message includes a 4-byte Unix timestamp. On decryption, the timestamp is validated:

- Messages older than **24 hours** are rejected
- Messages more than **5 minutes** in the future are rejected

This prevents replay attacks where an intercepted ciphertext is re-sent later.

## HMAC Verification

Before decryption succeeds, the truncated HMAC is verified using **constant-time comparison** (`MessageDigest.isEqual()`). This prevents timing attacks that could leak information about the expected HMAC value.

The HMAC is computed over the plaintext payload (before encryption), providing an additional authentication layer inside the GCM ciphertext. While GCM already provides authentication, the inner HMAC:

1. Authenticates the packed structure (text + metadata) independently
2. Allows early rejection of corrupted data after decryption but before full parsing

## Key Storage (Android)

### Message Encryption Keys

Master keys for message encryption are stored using **EncryptedSharedPreferences** (AndroidX Security):

- Master key: AES-256-GCM (generated via Android Keystore)
- Key encryption: AES-256-SIV
- Value encryption: AES-256-GCM

Keys can be set globally (for all conversations) or per-conversation (stored in encrypted Realm database).

### Realm Database Key

The Realm database is encrypted with a 512-bit key:

1. A 64-byte random key is generated via `SecureRandom`
2. An AES-256-GCM key is created in **Android Keystore** (hardware-backed TEE)
3. The Realm key is encrypted with the Keystore key
4. The encrypted Realm key and IV are stored in SharedPreferences
5. On each app start, the Realm key is decrypted using the Keystore key

```
┌────────────┐     AES-256-GCM      ┌─────────────────────┐
│  Android   │ ───────────────────►  │  SharedPreferences   │
│  Keystore  │   encrypt(realm_key)  │  (encrypted key + IV)│
│  (TEE)     │ ◄─────────────────── │                      │
│            │   decrypt(realm_key)  │                      │
└────────────┘                       └─────────────────────┘
```

## Key Exchange

Keys are exchanged **out-of-band** between users:

1. **QR code**: One user generates a QR code containing the Base64-encoded key; the other scans it
2. **Manual entry**: Users copy/paste the Base64-encoded key

Key authenticity can be verified by comparing **SHA-256 fingerprints** (first 16 bytes, displayed as hex):

```
Key bytes → SHA-256 → first 16 bytes → "A1 B2 C3 D4 E5 F6 ... "
```

Both parties should verify the fingerprint matches via a separate secure channel.

## Security Properties

| Property | Mechanism |
|---|---|
| Confidentiality | AES-256-GCM |
| Integrity | GCM authentication tag (128-bit) + HMAC-SHA256 (truncated 64-bit) |
| Replay protection | 4-byte timestamp with 24-hour window |
| Key separation | HKDF with distinct info strings |
| Key storage | Android Keystore (TEE) + EncryptedSharedPreferences |
| Length hiding | PKCS#7 padding to 16-byte boundary |
| Steganography | Base64 / Cyrillic Base64 / Russian Words encoding |
| Timing attack resistance | Constant-time HMAC comparison |
| Nonce reuse resistance | 12-byte random nonce per message via SecureRandom |

## Limitations

- **No forward secrecy**: Compromising the master key decrypts all past and future messages
- **No key ratcheting**: The same master key is used for all messages in a conversation
- **Truncated HMAC**: 64-bit HMAC provides 2^64 collision resistance (sufficient for SMS but below typical 128-bit threshold)
- **Timestamp granularity**: 1-second resolution, 32-bit Unix timestamp (overflows in 2106)
- **SMS size constraints**: Steganographic encoding expands message size; long messages may be split into multiple SMS segments by the carrier
- **No deniability**: Both parties share the same symmetric key and can prove the other sent a message
