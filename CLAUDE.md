# Lapka SMS

Open-source зашифрованный SMS-мессенджер для Android. End-to-end шифрование SMS со стеганографией — зашифрованные сообщения выглядят как обычный текст.

## Что делает

Защищает SMS от перехвата мобильным оператором. Обе стороны должны иметь Lapka SMS с одинаковым ключом шифрования. Форк Partisan-SMS (форк QKSMS).

## Стек

- **Kotlin** 1.9.22 (~345 файлов в main, ~380 всего), Android `minSdk 23` / `targetSdk 35` / `compileSdk 35`
- **AGP** 8.1.4
- **Clean Architecture**: 5 модулей — `presentation` / `domain` / `data` / `psms-lib` / `common`
- **DI**: Dagger 2.52
- **DB**: Realm 10.18.0 (шифрование через Android Keystore)
- **Reactive**: RxJava 2.1.4 + RxKotlin 2.1.0 + AutoDispose 1.3.0
- **Navigation**: Conductor 2.1.5 (не Fragments)
- **UI**: Material Components 1.12.0 (`Theme.Material3.*`), Compose не используется
- **Crypto**: AES-256-GCM (psms-lib), HKDF-SHA256 (RFC 5869)
- **Прочее**: ZXing 3.5.3 (QR), OkHttp 4.12.0, Glide 4.16.0, Moshi 1.15.1, Coroutines 1.9.0

**Текущая версия**: `5.0.4` (versionCode 5004)

## Структура

```
presentation/    # UI: Activities, Conductor Controllers, ViewModels
  feature/       # blocking, changelog, compose, contacts, conversationinfo,
                 # conversations, keysettings, main, notificationprefs,
                 # qkreply, settings, themepicker
  injection/     # Dagger DI (@Singleton, @ActivityScope, @ControllerScope)
  common/        # base, view, util (общие UI-компоненты)
  interactor/    # presentation-level use cases

domain/          # Бизнес-логика, use cases (Interactors), модели
  model/         # Message, Conversation, Contact, Recipient, MmsPart,
                 # BlockedNumber, ContactGroup, PhoneNumber, SyncLog
  repository/    # Интерфейсы репозиториев
  interactor/    # Use cases
  blocking/, experiment/, listener/, manager/, mapper/, util/

data/            # Реализации: Realm, SMS/MMS провайдеры
  repository/    # Realm-based реализации
  crypto/        # ConversationKeyStore, KSmsEncryptorFactory
  migration/     # QkMigration, QkRealmMigration
  receiver/      # BroadcastReceivers (SMS, MMS, boot, default-changed, ...)
  service/       # AutoDeleteService, HeadlessSmsSendService
  blocking/, compat/, filter/, listener/, manager/, mapper/, util/

psms-lib/        # Библиотека шифрования (41 .kt в main, ~51 с тестами)
  encryptor/                 # AesGcmEncryptor (+ Encryptor интерфейс)
  encrypted_data_encoder/    # Base64, CyrillicBase64, EncryptedDataEncoderFactory
    text_encoder/            # TextEncoder (рус/англ слова, даты, пунктуация)
  plain_data_encoder/        # Huffman (Cyrillic/Latin), Cp1251, Ascii, Utf8,
                             # ShortCp1251Cyrillic/Latin, NotAlignedEncoder
  PSmsEncryptor.kt           # Главный оркестратор шифрования
  NonceCache.kt              # Anti-replay (HashSet + TTL, persist via save/loadFrom)
  Hkdf.kt                    # HKDF-SHA256 (RFC 5869)
  MetaInfo.kt, Message.kt, InvalidDataException.kt, InvalidVersionException.kt

common/          # Утилиты, расширения, glide-gifencoder vendored copy
```

## Сборка

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release (требует keystore)
./gradlew test                   # Unit тесты
```

**Требования**: JDK 17, Android SDK 35

## Протокол шифрования (v3)

- AES-256-GCM, 128-bit auth tag
- HKDF-SHA256 key derivation
- Per-conversation ключи (не глобальный)
- Replay protection (NonceCache)
- Message padding (скрывает длину)
- Fingerprint verification: SHA-256 emoji fingerprint

## Схемы стеганографии

- **Base64** — компактный
- **Cyrillic Base64** — выглядит как кириллица
- **Russian Words** — словарь ~84K слов
- **English Words** — словарь ~150K слов

## Realm модели

- **Message**: id, threadId, body, date, boxId, type, deliveryStatus, attachmentType...
- **Conversation**: id, archived, blocked, pinned, encryptionKey, encodingSchemeId, encryptionEnabled, deleteEncryptedAfter...
- **Recipient**, **Contact**, **BlockedNumber**, **MmsPart**, **SyncLog**, **ContactGroup**

## Безопасность

- `FLAG_SECURE` — скрытие из переключателя задач (настраиваемо)
- Android Keystore — аппаратное хранение ключей
- EncryptedSharedPreferences
- Network security config — запрет cleartext
- R8 обфускация отключена (ложные срабатывания антивирусов)

## Особенности

- QR-код для обмена ключами
- Dual SIM поддержка
- Отложенная отправка
- Автоудаление зашифрованных сообщений (таймер)
- SMS reset command (удалённая очистка ключей)
- ~40 языков (39 локалей в `presentation/src/main/res/values-*` + дефолт `en`)
- CI/CD: GitHub Actions (`.github/workflows/android.yml`) + CircleCI (`.circleci/config.yml`)

## Namespace

`org.lapka.sms.*` (бывший `com.moez.QKSMS`); `applicationId = "org.lapka.sms"`

## Гитовые особенности

- `.gitignore` исключает `mempalace.yaml` и `entities.json` (per-project MemPalace files, issue #185).
- `secrets.tar.enc` — зашифрованный архив релизных секретов, расшифровывается в CI.
- `core.fileMode=true`: если на воркинг-копии случайно выставлен executable bit (`chmod -R +x .`), git покажет массовые "modified" по mode без изменения content. Проверять через `git diff --numstat` (`0\t0` → только mode-change, контент идентичен).
