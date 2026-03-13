# Lapka SMS — Encryption Setup Guide

A step-by-step guide for setting up encrypted messaging between two people.

**What you need:**
- Both you and your contact install Lapka SMS
- A way to meet in person (best) or a secure channel to share the key (Signal, Telegram secret chat, etc.)

---

## Step 1. Install and set as default

Download and install the APK. On first launch, the app will ask to become the default SMS app — accept.

> ![Screenshot: permission prompt to set as default SMS app](screenshots/set_default.png)

---

## Step 2. Open encryption settings

Go to **Settings** (gear icon in top right) → scroll down to the **Security** section → tap **Global Encryption Key**.

> ![Screenshot: Settings screen with Security section highlighted](screenshots/settings_security.png)

---

## Step 3. Enable encryption

Turn on the **Encryption key** toggle.

> ![Screenshot: Key settings screen with toggle OFF](screenshots/key_toggle_off.png)

---

## Step 4. Create a key

Tap **Generate new key**. A random 256-bit AES key will be created. You'll see:

- The key text (Base64 string)
- A **QR code** for easy sharing
- A **fingerprint** — a short code to verify the key later

> ![Screenshot: Key generated — QR code, key field, fingerprint visible](screenshots/key_generated.png)

> **Don't share the key text over regular SMS or unencrypted messengers.** Anyone who has the key can read your messages.

---

## Step 5. Share the key with your contact

### Option A: QR code (recommended)

The safest way — meet in person:

1. Show the QR code on your screen
2. Your contact opens **Settings** → **Global Encryption Key** → enables the toggle
3. They tap **Scan QR-code with key** and scan your screen

> ![Screenshot: QR code scanner in action](screenshots/scan_qr.png)

### Option B: Copy and send via secure channel

1. Tap the **copy button** next to the key field
2. Send the key via Signal, Telegram secret chat, or another encrypted messenger
3. Your contact pastes it into the key field in their Lapka SMS

> The key is automatically removed from clipboard after 30 seconds.

---

## Step 6. Verify the fingerprint

After both devices have the key, compare the **fingerprint** (shown below the QR code). It looks like this:

```
A1 B2 C3 D4 E5 F6 78 90 AB CD EF 12 34 56 78 90
```

Read it aloud to each other (in person or by phone). If the fingerprints match — the key is correct. If they don't — someone may have tampered with the key during transfer.

> ![Screenshot: Fingerprint displayed under QR code](screenshots/fingerprint.png)

---

## Step 7. Choose an encoding scheme

Below the key field, select how encrypted messages will look:

| Scheme | What it looks like | When to use |
|---|---|---|
| **Base64** | `dGVzdCBtZXNzYWdl` | Universal, most compact |
| **Cyrillic Base64** | `дГВздСБтЗХНзYWdl` | Looks like Cyrillic gibberish |
| **Russian Words** | `молоко дерево утро книга` | Looks like real Russian text |

> ![Screenshot: Encoding scheme selector with three options](screenshots/encoding_scheme.png)

**Recommendations:**
- Use **Russian Words** if you want messages to look natural to a human glancing at the screen
- Use **Base64** for the shortest messages (saves money on multi-part SMS)
- Use **Cyrillic Base64** as a middle ground

> **Both parties must use the same encoding scheme!** Otherwise the message won't decrypt. Agree on the scheme when you share the key.

---

## Step 8. Done! Send a test message

Go back to conversations, open a chat with your contact, and send a test message. It will be encrypted automatically.

Your contact receives the message and sees the decrypted text — as if nothing happened. If they look at the raw SMS (in another app), they'd see the encoded version.

> ![Screenshot: Conversation showing a normal-looking message (decrypted)](screenshots/conversation_encrypted.png)

---

## Additional security settings

### Auto-delete encrypted messages

In **Settings** → **Security** → **Delete Encrypted Messages After**, choose a timer:

| Option | When to use |
|---|---|
| Do not delete | Keep messages (default) |
| 5 sec – 1 hour | Messages disappear after being read |

> ![Screenshot: Auto-delete timer options](screenshots/auto_delete.png)

You can also set this per conversation: open a conversation → menu → **Details** → **Delete Encrypted Messages After**.

---

### Hide app from task switcher

In **Settings** → **Security** → disable **Show App Content in Task Switcher**.

When disabled, the app shows a blank screen in the recent apps list, and screenshots/screen recording are blocked.

> ![Screenshot: Task switcher toggle](screenshots/task_switcher.png)

---

### SMS for Reset (emergency key wipe)

In **Settings** → **Security** → **SMS for Reset**, set a secret phrase.

If someone sends you an SMS containing this exact phrase, all encryption keys are **permanently erased**. Messages remain on the device but become unreadable.

**Use case:** If your phone is confiscated, a trusted person can send this SMS to destroy all keys remotely.

> ![Screenshot: SMS for Reset field](screenshots/sms_reset.png)

> **Remember the phrase or write it down separately.** If you lose it, you can't trigger the reset remotely.

---

## Per-conversation keys (advanced)

You can set a **different key for each conversation** instead of (or in addition to) the global key:

1. Open a conversation → menu (three dots) → **Details**
2. Tap **Conversation Encryption Key**
3. Set up a key the same way as the global one

> ![Screenshot: Conversation info screen with encryption key option](screenshots/conversation_info.png)

**How priority works:**
- If a conversation has its own key → that key is used
- If not → the global key is used
- If neither is set → messages are sent as plain SMS

---

## Troubleshooting

### I see garbled text instead of a decrypted message

- **Wrong key**: make sure both sides have the same key. Compare fingerprints.
- **Wrong encoding scheme**: both sides must use the same scheme (Base64 / Cyrillic / Russian Words).
- **Message too old**: messages older than 24 hours are rejected for security. This can happen if delivery was delayed.

### My contact doesn't have Lapka SMS

Encrypted messaging requires Lapka SMS on both sides. Without it, your contact will see the raw encoded text (Base64 or Russian words). Regular unencrypted SMS works normally with any phone.

### I forgot my key

The key is stored encrypted on your device. You can view it in **Settings** → **Global Encryption Key**. There's no way to recover a key that has been erased.

### "Bad key!" error when scanning QR code

The scanned data is not a valid AES key. Make sure you're scanning a QR code generated by Lapka SMS (not a random QR code).

---

## Quick reference

| Action | Where |
|---|---|
| Set global key | Settings → Security → Global Encryption Key |
| Set per-conversation key | Conversation → Menu → Details → Conversation Encryption Key |
| Choose encoding scheme | Inside key settings (below the key field) |
| Auto-delete timer | Settings → Security → Delete Encrypted Messages After |
| Hide from task switcher | Settings → Security → Show App Content in Task Switcher |
| Emergency key wipe | Settings → Security → SMS for Reset |
| View key / QR / fingerprint | Settings → Security → Global Encryption Key |
