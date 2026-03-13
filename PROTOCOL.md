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
│       Pack          │  Append channel ID (optional) + MetaInfo byte
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│    AES-256-GCM      │  Encrypt with derived key; nonce = timestamp || random
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

A single **master key** (128, 192, or 256 bits) is shared between parties. An encryption subkey is derived using HKDF (RFC 5869) with HMAC-SHA256:

```
HKDF-Extract:
  salt = "k-sms-hkdf-v2" (fixed, public)
  IKM  = master_key
  PRK  = HMAC-SHA256(salt, IKM)

HKDF-Expand (encryption key):
  info   = "k-sms-v2-enc"
  length = 32 bytes
  enc_key = HKDF-Expand(PRK, info, 32)
```

The salt and info strings are fixed protocol constants, not secrets. HKDF provides domain separation and ensures the encryption key is uniformly distributed regardless of master key quality.

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

The encoded text bytes are packed into a payload with minimal metadata:

```
┌──────────────┬─────────────────┬──────────┐
│ encoded_text │ channel_id (4B) │ meta (1B)│
│ (variable)   │ (optional)      │          │
└──────────────┴─────────────────┴──────────┘
```

### Fields

**encoded_text** (variable length): Message text compressed by the plain data encoder.

**channel_id** (4 bytes, optional): Conversation channel identifier. Present only if the `isChannel` flag is set in MetaInfo. Little-endian uint32.

**MetaInfo** (1 byte): Bit-packed metadata byte:

```
Bit layout: [C][VVV][MMMM]

Bits 0-3 (MMMM): Plain data encoder mode (0-6)
Bits 4-6 (VVV):   Protocol version (currently 3)
Bit 7 (C):        Channel ID present flag (0 = no, 1 = yes)
```

No padding, no HMAC, no timestamp in the payload. Timestamp is carried in the GCM nonce (see below). Authentication is provided by the GCM tag.

## Encryption

The packed payload is encrypted using **AES-256-GCM** with a **128-bit authentication tag**:

```
timestamp = current Unix time (4 bytes, little-endian)
random    = SecureRandom(8 bytes)
nonce     = timestamp || random     (12 bytes total)

ciphertext || tag = AES-256-GCM(enc_key, nonce, payload)
                    tag is 128 bits (16 bytes)
```

Wire format:

```
┌────────────────────────────┬────────────────────────────────┐
│        nonce (12B)         │  ciphertext + GCM tag (16B)    │
│ [timestamp 4B][random 8B] │                                │
└────────────────────────────┴────────────────────────────────┘
```

### Properties

- **Nonce structure**: 12 bytes total — first 4 bytes are the Unix timestamp (little-endian), last 8 bytes are random via `SecureRandom`. This provides 2⁶⁴ random space per second, making nonce collision astronomically unlikely (~58 million years at 1000 messages/day).
- **GCM tag**: 128 bits (16 bytes), the maximum permitted by NIST SP 800-38D. Forgery probability: 2⁻¹²⁸ per attempt.
- **Cipher**: `AES/GCM/NoPadding` (javax.crypto, hardware-accelerated on Android). GCM operates in CTR mode internally, so no block padding is needed — arbitrary-length plaintext is accepted natively.
- **Timestamp in nonce**: The nonce is transmitted in cleartext (before the ciphertext). This allows the receiver to validate message freshness *before* performing the expensive GCM decryption, enabling early rejection of old/replayed messages at near-zero CPU cost. **Trade-off**: Since timestamp validation occurs before GCM authentication, an attacker could learn whether a forged message passed the timestamp check (via timing difference). This is an acceptable trade-off for SMS — it only reveals that the forged timestamp is within the 7-day window, which is trivially guessable anyway. The GCM tag still fully protects against forgery.
- **No separate HMAC**: GCM is an AEAD (Authenticated Encryption with Associated Data) cipher — the GCM tag already provides both integrity and authenticity for the entire payload. A separate HMAC would be redundant.
- **No PKCS#7 padding**: GCM/CTR is a stream cipher mode and handles arbitrary plaintext lengths. Block padding is unnecessary and would waste bytes.

### Overhead

Fixed overhead per message: **29 bytes** (12B nonce + 16B GCM tag + 1B MetaInfo).

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

Replay protection uses two independent mechanisms:

### 1. Timestamp validation

Each message carries a 4-byte Unix timestamp embedded in the first 4 bytes of the GCM nonce. On decryption, the timestamp is extracted and validated **before** GCM decryption:

- Messages older than **7 days** are rejected
- Messages more than **10 minutes** in the future are rejected

This provides fast rejection of old messages without any cryptographic overhead.

### 2. Nonce replay cache

After successful GCM decryption (proving the message is authentic), the 12-byte nonce is checked against an in-memory cache of recently seen nonces. If the nonce was already seen, the message is rejected as a replay.

The cache is checked *after* GCM authentication to prevent cache poisoning — an attacker cannot force nonces into the cache by sending forged messages, since forged messages fail GCM authentication before the cache check.

Cache properties:
- **Size**: Up to 1000 entries (~12 KB memory)
- **TTL**: 7 days — entries older than 7 days are evicted automatically
- **Eviction**: By age (TTL) and FIFO when capacity is exceeded
- **Persistence**: In-memory only (cleared on app restart)
- **Thread safety**: Synchronized access

## Decryption Pipeline

```
SMS text
   │
   ▼
┌─────────────────────┐
│ Encrypted Data      │  Decode from Base64/Cyrillic/Russian Words
│ Decoder             │  (strip front-padding if needed)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Extract nonce       │  Read first 12 bytes; extract timestamp from bytes 0-3
│ Validate timestamp  │  Reject if outside 7-day window (before GCM decryption)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ AES-256-GCM decrypt │  Verify 128-bit tag + decrypt payload
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Check nonce cache   │  Reject if nonce was already seen (anti-replay)
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Unpack              │  Parse MetaInfo, extract channel ID, text bytes
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Plain Data Decoder  │  Decompress text bytes based on MetaInfo mode
└──────────┬──────────┘
           ▼
      Plaintext
```

## smsForReset Protection

The "SMS for reset" feature allows a special trigger phrase to remotely wipe all encryption keys. This is protected with hash-based comparison:

1. When the user sets a trigger phrase, both the phrase (in EncryptedSharedPreferences) and its **SHA-256 hash** are stored
2. When an incoming message is decrypted, its text is hashed with SHA-256
3. The hash is compared against the stored hash using **constant-time comparison** (`MessageDigest.isEqual()`)

This provides defense-in-depth: even if EncryptedSharedPreferences were somehow compromised, the comparison operation does not leak timing information about the phrase.

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

Key authenticity can be verified by comparing **SHA-256 fingerprints** (first 16 bytes, displayed as emoji):

```
Key bytes → SHA-256 → first 16 bytes → each byte mapped to one of 256 unique emoji
Displayed as 4 rows of 4 emoji for easy visual comparison
```

Both parties should verify the fingerprint matches via a separate secure channel.

## Security Properties

| Property | Mechanism |
|---|---|
| Confidentiality | AES-256-GCM |
| Integrity & Authenticity | GCM authentication tag (128-bit) |
| Replay protection | Timestamp in nonce (7-day window) + nonce replay cache (1000 entries, 7-day TTL) |
| Key separation | HKDF with domain-specific info string |
| Key storage | Android Keystore (TEE) + EncryptedSharedPreferences |
| Steganography | Base64 / Cyrillic Base64 / Russian Words encoding |
| Timing attack resistance | Constant-time hash comparison for smsForReset |
| Nonce reuse resistance | timestamp(4B) + random(8B) = 2⁶⁴ random space per second |

## Wire Format Summary

```
On the wire (after steganographic encoding):

┌────────────────────────────┬────────────────────────────────┐
│        nonce (12B)         │  ciphertext + GCM tag          │
│ [timestamp 4B][random 8B] │  (tag = 16B)                   │
└────────────────────────────┴────────────────────────────────┘

Inside ciphertext (after GCM decryption):

┌──────────────┬─────────────────┬──────────┐
│ encoded_text │ channel_id (4B) │ meta (1B)│
│ (variable)   │ (optional)      │          │
└──────────────┴─────────────────┴──────────┘

Fixed overhead: 29 bytes (12B nonce + 16B GCM tag + 1B MetaInfo)
```

## Limitations

- **No forward secrecy**: Compromising the master key decrypts all past and future messages
- **No key ratcheting**: The same master key is used for all messages in a conversation
- **Timestamp granularity**: 1-second resolution, 32-bit Unix timestamp (overflows in 2106)
- **SMS size constraints**: Steganographic encoding expands message size; long messages may be split into multiple SMS segments by the carrier
- **No deniability**: Both parties share the same symmetric key and can prove the other sent a message
- **Nonce cache is in-memory**: Replay detection is lost on app restart; within the 7-day timestamp window, a restart allows replay of previously seen messages
