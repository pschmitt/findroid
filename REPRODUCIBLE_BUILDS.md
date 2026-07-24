# Reproducible builds

This documents what's currently in place to let a third party verify a released Findroid+ APK
was actually built from the source at the commit it claims, and what's still missing. It's an
ongoing effort, not a finished guarantee — see "Known gaps" below.

## What's already pinned

- **App version**: `versionCode`/`versionName` are static, checked into
  [`buildSrc/src/main/kotlin/Versions.kt`](buildSrc/src/main/kotlin/Versions.kt) — not derived
  from a timestamp or build counter.
- **Dependencies**: every dependency in
  [`gradle/libs.versions.toml`](gradle/libs.versions.toml) is pinned to an exact version (no
  `+`/`latest.release` ranges).
- **Toolchain**: Gradle, AGP, and Kotlin versions are all exact-pinned (see
  `gradle/wrapper/gradle-wrapper.properties` and `gradle/libs.versions.toml`). The Gradle wrapper
  also verifies the downloaded distribution against a `distributionSha256Sum`.
- **Nix dev shell**: [`flake.nix`](flake.nix) + `flake.lock` pin an exact JDK and Android SDK
  toolchain (locked nixpkgs revision), used by the `just` recipes for local/remote builds (see
  `AGENTS.md`).
- **CI JDK**: GitHub Actions workflows pin an exact Temurin patch version (`21.0.12`) rather than
  a floating major version, so the JDK used to build a given release doesn't silently drift
  between runs.
- **Embedded commit**: every release APK (phone and TV) embeds the exact commit it was built from
  as `BuildConfig.GIT_REVISION`, computed via `git describe --always --abbrev=12 --dirty`. You can
  check this in the app under **Settings → About**, or directly from the APK:

  ```sh
  unzip -p findroid-plus-latest-arm64-v8a-release.apk 'classes*.dex' | strings | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+'
  ```

  CI ([`.github/workflows/release-latest.yaml`](.github/workflows/release-latest.yaml)) verifies
  this landed correctly in the built APK before publishing, the same check `just build --release`
  runs for local builds (see [`justfile`](justfile)).
- **Checksums**: the `latest` GitHub release includes a `SHA256SUMS` file covering every published
  APK.

## Rebuilding a release yourself

```sh
git clone https://github.com/pschmitt/findroidplus
cd findroidplus
git checkout <commit-or-tag>
nix develop --command ./gradlew :app:phone:assembleLibreRelease
```

The resulting APK at `app/phone/build/outputs/apk/libre/release/` will:
- Match the published `versionCode`/`versionName` for that commit.
- Contain the same `GIT_REVISION` string (checkable as above), if you also export `GIT_REVISION`
  before building — CI does this automatically, but a local build without it embeds `"unknown"`.
- Be functionally and byte-for-byte identical in its **unsigned content** (dex, resources,
  manifest) to the published build, assuming the same JDK/AGP/Kotlin/Gradle versions — which the
  Nix flake pins for you.

It will **not** have the same signature bytes as the published APK, because the CI signing key is
private (necessarily — anyone who could reproduce the signature could impersonate a real release).
To compare your rebuild against a published release without needing the private key, strip both
APKs' signatures and diff the remaining content, e.g. with `apksigner` and a tool like
[`diffoscope`](https://diffoscope.org/) or [`apkdiff`](https://github.com/facebookarchive/apkdiff).

## Known gaps

- **CI doesn't build through the Nix flake.** GitHub Actions installs JDK/Gradle directly on the
  bare runner rather than via `nix develop`, so the toolchain that's actually pinned in this repo
  (`flake.nix`/`flake.lock`) isn't strictly the one producing shipped APKs — only the JDK *patch
  version* is pinned to match. Full toolchain parity (same JDK vendor/build, same Android SDK
  build-tools binary) between the Nix dev environment and CI is a future improvement, not yet
  guaranteed.
- **No Gradle dependency-verification metadata.** Dependency versions are pinned, but nothing
  checksums the actual resolved artifacts (`gradle/verification-metadata.xml` doesn't exist yet).
- **`publish.yaml`** (the Play Store / tagged-release workflow) doesn't yet embed `GIT_REVISION`
  or publish a `SHA256SUMS` file the way `release-latest.yaml` does.
