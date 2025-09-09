[![codecov](https://codecov.io/github/ipirangad3v/my-diary-android-app/branch/master/graph/badge.svg?token=4I1V1EHFBE)](https://codecov.io/github/ipirangad3v/my-diary-android-app)

## My Diary - A Private, Encrypted Android Diary

My Diary is a modern, offline-first journaling application for Android, built with security and privacy as its core principles. It demonstrates a robust, multi-layered encryption strategy to ensure that user entries remain confidential and accessible only to them, even across multiple devices.

## Download
<a href='https://play.google.com/store/apps/details?id=digital.tonima.mydiary' target="_blank" rel="noopener noreferrer"><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='200'/></a>
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

[![](https://mermaid.ink/img/pako:eNqlVW1P2zAQ_isnI02bVCC0hdJsYur6xmuBdmzaWj4E59pGpHZmO5Su4r_v7LQlMAaI-UOVuPc8d_fcY2fOuAyR-WykgmQMXxsDAbR0epVtDFgrUtpANxWwCR0JZ4HWU6nCAcsi7ar1a0kCPRMocwnr63vwZX4SaINqFQ1cYWAw_Hz38R72hWItpUXU-z0KQeFDD02awCP8ZQ5Wd4DG_EJTQMar4R1wKYaRmmhIFph8qobDNF2diZKTxGgYSgVXkZygURHXYCSg4GqWmOeSNx1Rq_-4v0gv0RjCNDJjCPEm4gjXOKPidHCDD3hajqdNPJGArPXl3yjCgfhrDB2pJkEM1DTN4ULEkl_nR-C0_EFCWNr9lZjHFPYw8b6LOMjUU_grRU1apI4wr9iBizt8QbEQX1Ts0FbWSzlHnVXXfrbPLnJ5g2oGUkAHp9BwMuZbdYSNLHFEUa0givWnK7X3vnEvOR8HYoThB5fxaKXHSSBSUnGZJF_nkQs9zoRBQe3ox23l9Tm2VRwILpVCbu6NYElO-r2xnJIjlCLFqES1bEo_mLRdJ1mFj5nrT_F2XphGt7n-Dwtnluw0v-dsma-i4_hPn7K1sy4EIzJqHnH6ilk6c7fogKYKdX6EbQc-y7SucTtGnRf3zEpQC0NoCkOq2eDz1QxX-_lyzl1Qd96jau1RtLBUR2L03Ay7D3pYJf4W4XSzgTEazBXQ6zeiIJYj0G62C-eTNHTxGEpoHbg4-A4pnTvzNfaeTmfPKNjBuqP7hKBmFiPUYBjFsb82rA4L2ih5jf5aqVRaPK9Po9CM_WJyW-Aylspf8zwvD68v4Jy_Cb6_yl59C_zo_-DtBbw6fDWcFeiLFoXMNyrFApsg3Z72lc0t8YCZMU7oVvHpMQyUu0rvCJME4qeUkyVMyXQ0Zv4wiDW9pUlI3xpyAfl7stpVNCpUdZkKw_zibtWRMH_Obpm_VSxtVLzKjlcubldKW5XdcoHNmL-9u-F51IBX8crlYrVcviuw3y7t1kZxt1Lxdii4TL_bW-W7P5jOWlE?type=png)](https://mermaid.live/edit#pako:eNqlVW1P2zAQ_isnI02bVCC0hdJsYur6xmuBdmzaWj4E59pGpHZmO5Su4r_v7LQlMAaI-UOVuPc8d_fcY2fOuAyR-WykgmQMXxsDAbR0epVtDFgrUtpANxWwCR0JZ4HWU6nCAcsi7ar1a0kCPRMocwnr63vwZX4SaINqFQ1cYWAw_Hz38R72hWItpUXU-z0KQeFDD02awCP8ZQ5Wd4DG_EJTQMar4R1wKYaRmmhIFph8qobDNF2diZKTxGgYSgVXkZygURHXYCSg4GqWmOeSNx1Rq_-4v0gv0RjCNDJjCPEm4gjXOKPidHCDD3hajqdNPJGArPXl3yjCgfhrDB2pJkEM1DTN4ULEkl_nR-C0_EFCWNr9lZjHFPYw8b6LOMjUU_grRU1apI4wr9iBizt8QbEQX1Ts0FbWSzlHnVXXfrbPLnJ5g2oGUkAHp9BwMuZbdYSNLHFEUa0givWnK7X3vnEvOR8HYoThB5fxaKXHSSBSUnGZJF_nkQs9zoRBQe3ox23l9Tm2VRwILpVCbu6NYElO-r2xnJIjlCLFqES1bEo_mLRdJ1mFj5nrT_F2XphGt7n-Dwtnluw0v-dsma-i4_hPn7K1sy4EIzJqHnH6ilk6c7fogKYKdX6EbQc-y7SucTtGnRf3zEpQC0NoCkOq2eDz1QxX-_lyzl1Qd96jau1RtLBUR2L03Ay7D3pYJf4W4XSzgTEazBXQ6zeiIJYj0G62C-eTNHTxGEpoHbg4-A4pnTvzNfaeTmfPKNjBuqP7hKBmFiPUYBjFsb82rA4L2ih5jf5aqVRaPK9Po9CM_WJyW-Aylspf8zwvD68v4Jy_Cb6_yl59C_zo_-DtBbw6fDWcFeiLFoXMNyrFApsg3Z72lc0t8YCZMU7oVvHpMQyUu0rvCJME4qeUkyVMyXQ0Zv4wiDW9pUlI3xpyAfl7stpVNCpUdZkKw_zibtWRMH_Obpm_VSxtVLzKjlcubldKW5XdcoHNmL-9u-F51IBX8crlYrVcviuw3y7t1kZxt1Lxdii4TL_bW-W7P5jOWlE)

 - On a daily basis, the user authenticates with their fingerprint.

- This unlocks the key in the Android Keystore.

- This key is used to decrypt the master password, which is held only in memory.

- The master password is then used to decrypt the diary entries.

- Device Migration: When restoring from a backup on a new device, the hardware-backed key is no longer available. The app detects this and prompts the user to manually enter their master password. Once verified, the master password is then re-encrypted using the new device's Keystore, seamlessly re-enabling biometric unlock.
