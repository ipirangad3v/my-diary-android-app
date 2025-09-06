## My Diary - A Private, Encrypted Android Diary

My Diary is a modern, offline-first journaling application for Android, built with security and privacy as its core principles. It demonstrates a robust, multi-layered encryption strategy to ensure that user entries remain confidential and accessible only to them, even across multiple devices.

## Features

- <b>Offline-First:</b> All entries are stored locally on your device.

- <b>End-to-End Encryption:</b> Every diary entry is encrypted at rest using AES-256-GCM.

- <b>Biometric Unlock:</b> Conveniently access your journal using your device's fingerprint, PIN, or pattern.

- <b>Backup & Restore Ready:</b> The encryption architecture is designed to allow for secure cloud backups and restoration on a new device.

- <b>Multi-Device Recovery:</b> A secure flow allows you to regain access to your notes on a new device using your master password.

- <b>Modern UI:</b> Built entirely with Jetpack Compose for a clean and responsive user interface.

- <b>Internationalization:</b> Supports English, Spanish, and Brazilian Portuguese.

## Security Architecture
The application's security model is designed to provide both convenience and true data portability without compromising privacy.

- <b>Master Password:</b> All diary entries are encrypted using a strong cryptographic key derived from a user-defined master password via PBKDF2. This is the ultimate key to the user's data.

- <b>Hardware-Backed Biometric Unlock:</b> For daily convenience, the master password itself is encrypted using a separate key stored in the Android Keystore. This hardware-backed key is configured to require biometric authentication for any use.

## The Flow:

 - On a daily basis, the user authenticates with their fingerprint.

- This unlocks the key in the Android Keystore.

- This key is used to decrypt the master password, which is held only in memory.

- The master password is then used to decrypt the diary entries.

- Device Migration: When restoring from a backup on a new device, the hardware-backed key is no longer available. The app detects this and prompts the user to manually enter their master password. Once verified, the master password is then re-encrypted using the new device's Keystore, seamlessly re-enabling biometric unlock.
