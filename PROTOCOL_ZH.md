# Lapka SMS 加密协议

版本：**3** (`psms-lib`)

本文档描述了 Lapka SMS 用于保护短信内容的加密协议。该协议提供带有重放保护和隐写编码的认证加密。

## 概述

```
明文
   │
   ▼
┌─────────────────────┐
│  Plain Data Encoder  │  将文本压缩为字节（自动选择最佳编码器）
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│       Pack          │  附加 channel ID（可选）+ MetaInfo 字节
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│    AES-256-GCM      │  使用派生密钥加密；nonce = 时间戳 || 随机数
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Encrypted Data      │  将密文编码为 Base64 / 西里尔字母 Base64 /
│ Encoder             │  俄语词汇 / 英语词汇
└──────────┬──────────┘
           ▼
      短信文本
```

## 密钥派生

双方共享一个**主密钥**（128、192 或 256 位）。使用 HKDF（RFC 5869）和 HMAC-SHA256 派生加密子密钥：

```
HKDF-Extract:
  salt = "k-sms-hkdf-v2" (固定，公开)
  IKM  = master_key
  PRK  = HMAC-SHA256(salt, IKM)

HKDF-Expand (encryption key):
  info   = "k-sms-v2-enc"
  length = 32 bytes
  enc_key = HKDF-Expand(PRK, info, 32)
```

salt 和 info 字符串是固定的协议常量，不属于秘密信息。HKDF 提供域分离，确保无论主密钥质量如何，加密密钥都是均匀分布的。

## 明文数据编码

在加密之前，明文消息使用最紧凑的可用编码器压缩为字节数组。编码器根据消息内容自动选择：

| 模式 | ID | 描述 |
|---|---|---|
| SHORT_CP1251_PREFER_CYRILLIC | 0 | 紧凑的 CP1251 编码，为西里尔字母优化 |
| SHORT_CP1251_PREFER_LATIN | 1 | 紧凑的 CP1251 编码，为拉丁字母优化 |
| CP1251 | 2 | 标准 CP1251 编码 |
| UTF_8 | 3 | 标准 UTF-8（任意文本的后备方案） |
| ASCII | 4 | 7 位 ASCII |
| HUFFMAN_CYRILLIC | 5 | 为西里尔字母文本优化的 Huffman 编码 |
| HUFFMAN_LATIN | 6 | 为拉丁字母文本优化的 Huffman 编码 |

编码器 ID 存储在 MetaInfo 字节中（见下文），以便接收方知道如何解码。

## 消息打包

编码后的文本字节被打包为带有最少元数据的有效载荷：

```
┌──────────────┬─────────────────┬──────────┐
│ encoded_text │ channel_id (4B) │ meta (1B)│
│ (variable)   │ (optional)      │          │
└──────────────┴─────────────────┴──────────┘
```

### 字段

**encoded_text**（可变长度）：由明文数据编码器压缩的消息文本。

**channel_id**（4 字节，可选）：会话频道标识符。仅在 MetaInfo 中设置了 `isChannel` 标志时存在。小端序 uint32。

**MetaInfo**（1 字节）：位打包的元数据字节：

```
Bit layout: [C][VVV][MMMM]

Bits 0-3 (MMMM): Plain data encoder mode (0-6)
Bits 4-6 (VVV):   Protocol version (currently 3)
Bit 7 (C):        Channel ID present flag (0 = no, 1 = yes)
```

有效载荷中没有填充、没有 HMAC、没有时间戳。时间戳携带在 GCM nonce 中（见下文）。认证由 GCM 标签提供。

## 加密

打包后的有效载荷使用 **AES-256-GCM** 和 **128 位认证标签**进行加密：

```
timestamp = current Unix time (4 bytes, little-endian)
random    = SecureRandom(8 bytes)
nonce     = timestamp || random     (12 bytes total)

ciphertext || tag = AES-256-GCM(enc_key, nonce, payload)
                    tag is 128 bits (16 bytes)
```

线路格式：

```
┌────────────────────────────┬────────────────────────────────┐
│        nonce (12B)         │  ciphertext + GCM tag (16B)    │
│ [timestamp 4B][random 8B] │                                │
└────────────────────────────┴────────────────────────────────┘
```

### 特性

- **Nonce 结构**：共 12 字节——前 4 字节为 Unix 时间戳（小端序），后 8 字节为通过 `SecureRandom` 生成的随机数。每秒提供 2⁶⁴ 的随机空间，使 nonce 碰撞的概率极低（以每天 1000 条消息计算，约需 5800 万年）。
- **GCM 标签**：128 位（16 字节），为 NIST SP 800-38D 允许的最大值。每次尝试的伪造概率为 2⁻¹²⁸。
- **密码算法**：`AES/GCM/NoPadding`（javax.crypto，在 Android 上支持硬件加速）。GCM 内部以 CTR 模式运行，因此无需块填充——原生支持任意长度的明文。
- **Nonce 中的时间戳**：nonce 以明文形式传输（位于密文之前）。这允许接收方在执行开销较大的 GCM 解密*之前*验证消息的新鲜度，从而以几乎零 CPU 成本提前拒绝旧消息/重放消息。**权衡**：由于时间戳验证发生在 GCM 认证之前，攻击者可能通过时间差异得知伪造的消息是否通过了时间戳检查。对于短信而言这是可接受的权衡——它只能揭示伪造的时间戳是否在 7 天窗口内，而这本身就很容易猜到。GCM 标签仍然完全防止伪造。
- **无单独的 HMAC**：GCM 是一种 AEAD（带关联数据的认证加密）密码——GCM 标签已经为整个有效载荷提供了完整性和真实性。单独的 HMAC 是多余的。
- **无 PKCS#7 填充**：GCM/CTR 是流密码模式，可处理任意长度的明文。块填充是不必要的，且会浪费字节。

### 开销

每条消息的固定开销：**29 字节**（12B nonce + 16B GCM 标签 + 1B MetaInfo）。

## 加密数据编码（隐写术）

加密后的字节数组被编码为文本字符串，以便通过短信传输。有四种方案可用：

### Base64（方案 0）

标准 Base64 编码。紧凑但在视觉上明显是编码数据。

```
Input:  [0xDE, 0xAD, ...]
Output: "3q0t..."
```

### 西里尔字母 Base64（方案 1）

Base64，但将拉丁字母替换为西里尔字母：

```
Latin:    ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=
Cyrillic: АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя
```

输出在普通观察者看来像是西里尔字母文本。

### 俄语词汇/文本（方案 2）

使用子编码器系统将加密字节编码为看起来自然的俄语文本：

- **词汇子编码器**：将字节值映射到俄语词典中的单词（约 84K 个单词）
- **日期时间子编码器**：将值编码为日期/时间字符串
- **标点符号子编码器**：将值编码为标点符号

编码过程将加密数据视为一个大整数，并在可用的子编码器之间进行混合基数分解。如有需要，会在前面添加一个随机填充字节，以确保整数能完整映射到可用的词汇空间。

输出看起来像一串带有自然标点和空格的俄语词汇。解码时，最多去除 256 个前置填充字节以找到正确的对齐位置。

### 英语词汇/英文文本（方案 3）

与俄语词汇架构相同，但使用英语词典（约 150K 个单词）。输出看起来像一串带有自然标点和空格的英语词汇。当在接收者的环境中英语文本比俄语更不引起怀疑时非常有用。

## 重放保护

重放保护使用两种独立机制：

### 1. 时间戳验证

每条消息在 GCM nonce 的前 4 字节中携带一个 4 字节的 Unix 时间戳。在解密时，时间戳会在 GCM 解密**之前**被提取和验证：

- 超过 **7 天**的消息将被拒绝
- 比当前时间超前 **10 分钟**以上的消息将被拒绝

这提供了无需任何密码学开销即可快速拒绝旧消息的能力。

### 2. Nonce 重放缓存

在成功的 GCM 解密（证明消息是真实的）之后，12 字节的 nonce 将与内存中最近已见 nonce 的缓存进行比对。如果该 nonce 已经出现过，则该消息将被作为重放拒绝。

缓存在 GCM 认证*之后*才进行检查，以防止缓存污染——攻击者无法通过发送伪造消息来将 nonce 强制写入缓存，因为伪造消息在缓存检查之前就会因 GCM 认证失败而被拒绝。

缓存属性：
- **大小**：最多 1000 个条目（约 12 KB 内存）
- **TTL**：7 天——超过 7 天的条目自动被淘汰
- **淘汰策略**：按年龄（TTL）和容量超限时的 FIFO
- **持久化**：仅内存（应用重启时清除）
- **线程安全**：同步访问

## 解密流程

```
短信文本
   │
   ▼
┌─────────────────────┐
│ Encrypted Data      │  从 Base64/西里尔字母/俄语词汇/英语词汇解码
│ Decoder             │  （如需要则去除前置填充）
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Extract nonce       │  读取前 12 字节；从字节 0-3 提取时间戳
│ Validate timestamp  │  如超出 7 天窗口则拒绝（在 GCM 解密之前）
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ AES-256-GCM decrypt │  验证 128 位标签 + 解密有效载荷
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Check nonce cache   │  如 nonce 已出现过则拒绝（防重放）
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Unpack              │  解析 MetaInfo，提取 channel ID 和文本字节
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ Plain Data Decoder  │  根据 MetaInfo 模式解压文本字节
└──────────┬──────────┘
           ▼
      明文
```

## smsForReset 保护

"SMS for reset"功能允许使用特定触发短语远程擦除所有加密密钥。该功能通过基于哈希的比较进行保护：

1. 当用户设置触发短语时，短语本身（存储在 EncryptedSharedPreferences 中）及其 **SHA-256 哈希**都会被保存
2. 当收到的消息被解密后，其文本会用 SHA-256 进行哈希
3. 使用**常量时间比较**（`MessageDigest.isEqual()`）将哈希与存储的哈希进行比对

这提供了纵深防御：即使 EncryptedSharedPreferences 以某种方式被攻破，比较操作也不会泄露有关短语的时序信息。

## 密钥存储（Android）

### 消息加密密钥

消息加密的主密钥使用 **EncryptedSharedPreferences**（AndroidX Security）存储：

- 主密钥：AES-256-GCM（通过 Android Keystore 生成）
- 密钥加密：AES-256-SIV
- 值加密：AES-256-GCM

密钥按会话设置（存储在加密的 Realm 数据库中）。

### Realm 数据库密钥

Realm 数据库使用 512 位密钥加密：

1. 通过 `SecureRandom` 生成 64 字节的随机密钥
2. 在 **Android Keystore** 中创建 AES-256-GCM 密钥（硬件支持的 TEE）
3. 使用 Keystore 密钥加密 Realm 密钥
4. 将加密后的 Realm 密钥和 IV 存储在 SharedPreferences 中
5. 每次应用启动时，使用 Keystore 密钥解密 Realm 密钥

```
┌────────────┐     AES-256-GCM      ┌─────────────────────┐
│  Android   │ ───────────────────►  │  SharedPreferences   │
│  Keystore  │   encrypt(realm_key)  │  (encrypted key + IV)│
│  (TEE)     │ ◄─────────────────── │                      │
│            │   decrypt(realm_key)  │                      │
└────────────┘                       └─────────────────────┘
```

## 密钥交换

密钥通过**带外方式**在用户之间交换：

1. **二维码**：一方生成包含 Base64 编码密钥的二维码；另一方扫描
2. **手动输入**：用户复制/粘贴 Base64 编码的密钥

可以通过比较 **SHA-256 指纹**（前 16 字节，显示为 emoji）来验证密钥的真实性：

```
Key bytes → SHA-256 → first 16 bytes → each byte mapped to one of 256 unique emoji
Displayed as 4 rows of 4 emoji for easy visual comparison
```

双方应通过单独的安全渠道验证指纹是否匹配。

## 安全属性

| 属性 | 机制 |
|---|---|
| 机密性 | AES-256-GCM |
| 完整性与真实性 | GCM 认证标签（128 位） |
| 重放保护 | nonce 中的时间戳（7 天窗口）+ nonce 重放缓存（1000 个条目，7 天 TTL） |
| 密钥分离 | HKDF，使用特定域的 info 字符串 |
| 密钥存储 | Android Keystore (TEE) + EncryptedSharedPreferences |
| 隐写术 | Base64 / 西里尔字母 Base64 / 俄语词汇 / 英语词汇编码 |
| 时序攻击抵抗 | 用于 smsForReset 的常量时间哈希比较 |
| Nonce 重用抵抗 | timestamp(4B) + random(8B) = 每秒 2⁶⁴ 随机空间 |

## 线路格式摘要

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

## 局限性

- **无前向保密**：主密钥泄露将导致所有过去和未来的消息被解密
- **无密钥棘轮**：同一会话中所有消息使用相同的主密钥
- **时间戳精度**：1 秒分辨率，32 位 Unix 时间戳（2106 年溢出）
- **短信大小限制**：隐写编码会扩大消息大小；较长的消息可能被运营商拆分为多条短信
- **无可否认性**：双方共享相同的对称密钥，可以证明对方发送了某条消息
- **Nonce 缓存仅在内存中**：应用重启后重放检测将丢失；在 7 天时间戳窗口内，重启后允许重放之前已见的消息
