# AGENTS.md

Repository instructions for AI coding agents working on Findroid.

## Dev environment

- `nix develop` provides the full toolchain (JDK 21, Android SDK, `just`, `ktfmt`) and
  installs the repo's pre-commit hooks (see `flake.nix`'s `git-hooks.nix` integration —
  trailing whitespace, EOF fixer, merge-conflict/large-file checks, `nixfmt`, `statix`,
  and `ktfmt` on staged Kotlin files). The generated `.pre-commit-config.yaml` is
  gitignored — it's regenerated from `flake.nix` on every shell entry, don't hand-edit it.
- Prefer the `justfile` recipes over raw `./gradlew`/`ssh`/`adb` invocations — run
  `just --list` for the full set. It wraps everything below (remote builds, fetching
  APKs, and Mi Pad 4 install/logcat/adb-enable) in composable recipes.

## Builds

- **Never run Gradle builds locally on this machine (`fnuc`)** — this is a multi-module
  Android project and local compiles are heavy. Always build on `rofl-13.brkn.lol` or
  `rofl-14.brkn.lol` instead. The `justfile` automates this:
  - `just sync [host]` — rsync the working tree to the remote build host (excludes
    `.git/`, `build/`, `.gradle/`).
  - `just gradle [host] <tasks...>` — sync, then run arbitrary Gradle tasks remotely via
    `nix develop --command ./gradlew <tasks>`.
  - `just build-phone-debug` / `just build-tv-debug` — build the libre-flavor debug APK.
  - `just lint` — remote `ktfmtCheck` (mirrors `.github/workflows/lint.yaml`).
  - `just test` — remote unit test suites for `:data` and `:core`.
  - `just fetch-phone-debug` / `just fetch-tv-debug` — scp the built APK split back to
    `./dist/` locally.
  - `just build-and-fetch-phone-debug` — build + fetch in one step.
  - Manually, the equivalent is:
    1. `rsync -az --delete --exclude='.git/' --exclude='**/build/' --exclude='.gradle/' --exclude='**/.gradle/' ./ rofl-13.brkn.lol:~/devel/private/pschmitt/findroid-verify/`
    2. `ssh rofl-13.brkn.lol 'cd ~/devel/private/pschmitt/findroid-verify && nix develop --command ./gradlew <tasks>'`
    3. Re-run the rsync after every local edit before rebuilding remotely — there is no
       watch/sync daemon, it's a one-shot copy each time.
  - Plain Nix derivation builds (non-Gradle) already offload to rofl-13/rofl-14 via
    configured remote builders; only Gradle itself needs this manual redirect, since
    Gradle always executes wherever it's invoked regardless of Nix's remote-builder config.
- Gradle modules use product flavors (at least a "Libre" flavor), so bare task names like
  `:core:compileDebugKotlin` are ambiguous. Use the flavor-qualified task name (e.g.
  `compileLibreDebugKotlin`), or run `./gradlew tasks` in the target module first to confirm
  the exact name.
- Formatting/linting a Kotlin file directly (not a full Gradle build) is fine to run
  locally: `just format` runs the standalone `ktfmt` CLI over all tracked `.kt`/`.kts`
  files, matching what the pre-commit hook and CI's `ktfmtCheck` enforce.

## Physical test device

- A **Mi Pad 4** (`arm64-v8a`) is available for installing and checking debug builds.
  Reachable via SSH at `mi-pad-4.lan`, port `8022` (Termux). The `justfile` wraps the
  common operations:
  - `just mipad-adb-enable` — starts/restarts `adbd` in TCP mode via root (works even if
    USB debugging was never toggled on), then `adb connect`s to it.
  - `just mipad-install <apk>` — scp the APK to the device and `pm install -r` it via a
    root shell over SSH. Doesn't depend on `adb` pairing/authorization at all.
  - `just deploy-phone-debug` — build the phone debug APK remotely, fetch it, and
    install it on the Mi Pad 4 in one step.
  - `just mipad-logcat [filter]` — tail `logcat` from the device, optionally grepped.
  - `just mipad-uninstall <pkg>` — `pm uninstall` a package (see the signature-mismatch
    gotcha below).
  - `just mipad-shell` — interactive SSH shell on the device.
  - The device is **rooted**, so all of the above use `su -c '...'` over SSH rather than
    depending on `adb`'s own authorization/pairing flow.
  - Signature mismatch gotcha: if the device already has a build signed with a different
    key than the one you're installing, install fails with
    `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (or `pm install` reports an equivalent signature
    error). Fix is `just mipad-uninstall <applicationId>` then install fresh — this wipes
    local app data (Room DB, playback positions, download records). Confirm with the user
    before doing this if it's not their own throwaway data.
