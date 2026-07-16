# Findroid task runner.
#
# Gradle must never run on this machine directly - it's a heavy multi-module Android
# project, so every build/test/lint recipe here shells out to a remote host
# (rofl-13.brkn.lol or rofl-14.brkn.lol) over SSH instead. See AGENTS.md.

set shell := ["bash", "-euo", "pipefail", "-c"]

remote_host := env_var_or_default("FINDROID_REMOTE_HOST", "rofl-13.brkn.lol")
remote_path := env_var_or_default("FINDROID_REMOTE_PATH", "~/devel/private/pschmitt/findroid-verify")
local_dist := env_var_or_default("FINDROID_DIST_DIR", "./dist")

mipad_host := env_var_or_default("MIPAD_HOST", "mi-pad-4.lan")
mipad_ssh_port := env_var_or_default("MIPAD_SSH_PORT", "8022")
mipad_adb_port := env_var_or_default("MIPAD_ADB_PORT", "5555")
mipad_abi := env_var_or_default("MIPAD_ABI", "arm64-v8a")

# List all available recipes
default:
    @just --list

# --- Remote build (rofl-13 / rofl-14) -------------------------------------

# Sync the working tree to the remote build host (excludes .git/build/.gradle)
sync host=remote_host:
    rsync -az --delete \
        --exclude='.git/' --exclude='**/build/' \
        --exclude='.gradle/' --exclude='**/.gradle/' \
        ./ {{host}}:{{remote_path}}/

# Run one or more Gradle tasks on the remote host (syncs first)
gradle host=remote_host *tasks: (sync host)
    ssh {{host}} 'cd {{remote_path}} && nix develop --command ./gradlew {{tasks}}'

# Build the phone debug APK (libre flavor) remotely
build-phone-debug host=remote_host: (gradle host ":app:phone:assembleLibreDebug")

# Build the TV debug APK (libre flavor) remotely
build-tv-debug host=remote_host: (gradle host ":app:tv:assembleLibreDebug")

# ktfmt check via Gradle, remotely (mirrors .github/workflows/lint.yaml)
lint host=remote_host: (gradle host "ktfmtCheck")

# Run the fast unit test suites remotely
test host=remote_host: (gradle host ":data:testLibreDebugUnitTest" ":core:testLibreDebugUnitTest")

# Remote `./gradlew clean`
clean host=remote_host: (gradle host "clean")

# Copy the built phone debug APK split back to ./dist locally
fetch-phone-debug host=remote_host abi=mipad_abi:
    mkdir -p {{local_dist}}
    scp {{host}}:{{remote_path}}/app/phone/build/outputs/apk/libre/debug/phone-libre-{{abi}}-debug.apk {{local_dist}}/

# Copy the built TV debug APK split back to ./dist locally
fetch-tv-debug host=remote_host abi=mipad_abi:
    mkdir -p {{local_dist}}
    scp {{host}}:{{remote_path}}/app/tv/build/outputs/apk/libre/debug/tv-libre-{{abi}}-debug.apk {{local_dist}}/

# Build the phone debug APK remotely and copy it back to ./dist
build-and-fetch-phone-debug host=remote_host abi=mipad_abi: (build-phone-debug host) (fetch-phone-debug host abi)

# --- Mi Pad 4 test device (rooted, Termux SSH on port 8022) --------------

# Run an arbitrary command on the Mi Pad 4 over SSH
mipad-ssh +cmd:
    ssh -p {{mipad_ssh_port}} {{mipad_host}} "{{cmd}}"

# Interactive shell on the Mi Pad 4
mipad-shell:
    ssh -p {{mipad_ssh_port}} {{mipad_host}}

# Ensure adbd is running and listening over TCP (uses root, since USB debugging
# may be off and there's no cable attached) - then `adb connect` to it
mipad-adb-enable:
    ssh -p {{mipad_ssh_port}} {{mipad_host}} "su -c 'setprop service.adb.tcp.port {{mipad_adb_port}} && stop adbd && start adbd'"
    sleep 1
    adb connect {{mipad_host}}:{{mipad_adb_port}}

# Install an APK on the Mi Pad 4 via root shell over SSH (doesn't depend on adb
# pairing/authorization at all - just scp + `pm install -r`)
mipad-install apk:
    scp -P {{mipad_ssh_port}} {{apk}} {{mipad_host}}:/sdcard/findroid-install.apk
    ssh -p {{mipad_ssh_port}} {{mipad_host}} "su -c 'pm install -r /sdcard/findroid-install.apk'"
    ssh -p {{mipad_ssh_port}} {{mipad_host}} "rm -f /sdcard/findroid-install.apk"

# Uninstall a package from the Mi Pad 4 (e.g. after a signing-key mismatch -
# see AGENTS.md). WARNING: wipes that app's local data (Room DB, playback
# positions, downloads).
mipad-uninstall pkg:
    ssh -p {{mipad_ssh_port}} {{mipad_host}} "su -c 'pm uninstall {{pkg}}'"

# Tail logcat from the Mi Pad 4, optionally filtered by a grep pattern
mipad-logcat filter="":
    #!/usr/bin/env bash
    set -euo pipefail
    if [ -n "{{filter}}" ]; then
        ssh -p {{mipad_ssh_port}} {{mipad_host}} "su -c 'logcat'" | grep -i --line-buffered "{{filter}}"
    else
        ssh -p {{mipad_ssh_port}} {{mipad_host}} "su -c 'logcat'"
    fi

# Build the phone debug APK remotely, fetch it, and install it on the Mi Pad 4
deploy-phone-debug host=remote_host abi=mipad_abi: (build-and-fetch-phone-debug host abi)
    just mipad-install {{local_dist}}/phone-libre-{{abi}}-debug.apk

# --- Formatting / hooks ----------------------------------------------------

# Format Kotlin sources locally with ktfmt (lightweight - not a Gradle build,
# safe to run on this machine). Mirrors the pre-commit hook and CI's ktfmtCheck.
format:
    ktfmt --kotlinlang-style $(git ls-files '*.kt' '*.kts')

# Nix formatting/lint for this repo's flake.nix (per global AI context rules)
nix-fmt:
    nixfmt flake.nix

nix-lint:
    nix develop --command statix check
