# Savio 2.5

Version code: 20. All eight approved readiness changes are implemented.

- Authentication errors keep financial data locked; lock setup requires available device authentication.
- Local snapshots use a device-bound secret protected by Android Keystore. Legacy files are authenticated and re-encrypted with verification before replacement. Use **Backup Vault** before reinstalling or changing phones; password-protected vaults remain portable.
- Spend notifications update through bounded background work instead of an indefinitely running dataSync service. Display, actions and disable controls remain available.
- Content deduplication uses the existing timestamp index while preserving its five-second window and amount tolerance.
- Notification bill analysis reads only manually tracked bills, preserving the current radar behavior.
- Excel file generation runs off the UI thread, displays busy state and prevents concurrent exports.
- Invalid cloud credentials/account failures are suppressed within a scan; rate limits have a bounded cooldown. Settings offers an explicit retry. Cloud-first parsing and custom merchant rules retain their existing behavior.
- Vault export streams JSON into GZIP; restore avoids copying ciphertext. Existing portable backup formats remain supported.

Firebase configuration and dependencies, Room schema v7, and previously rejected cleanup are preserved.

## Verification

- 87 unit/screenshot tests passed, with zero failures, errors or skips.
- R8 release build, resource shrinking and release vital lint passed.
- Android instrumentation APK compiled, including a real Android Keystore test. Device tests were not run: no device/emulator was connected.
- APK version/package and signature verified; root/public APKs have identical checksums.

## APK

`public/savior-2.5.apk` (also available at repository root), 4,477,316 bytes.

SHA-256: `d8a530668fcaa72598aebb392f0cf32ade10b32302c0cbdbc8a91ab29d6b783a`

The APK uses the existing **debug signing certificate**, matching v2.1 for in-place updates. A production upload keystore was not available. The v2.1 APKs are retained for rollback.

## Performance evidence

The audit's desktop SQLite microbenchmark at 10,000 synthetic rows reduced the content-deduplication query from 0.223 ms to 0.00120 ms (99.5%). Recurring input parity tests reduced engine input from 4,604 to 500 rows (89.1%) across six scenarios. Failure-policy tests reduced 100 doomed cloud attempts to one per scan. These are isolated measurements/test fixtures, not Android whole-app latency or battery benchmarks.
