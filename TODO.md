# TODO

## FINDROID-1: Rebrand fork to Findroid+

Rename the package and app to distinguish this fork from upstream Findroid.

- [x] App name: `Findroid+` (`app_name` in `core/src/{main,debug,staging}/res/values/strings.xml`)
- [x] Package / applicationId: `dev.pschmitt.findroidplus` (`app/phone`, `app/tv`;
      library module namespaces stayed `dev.jdtech.jellyfin.*` — internal only, not
      user-facing, not worth the churn of moving every source file)
- [x] Bump version to `2.0.0` (`buildSrc/src/main/kotlin/Versions.kt`, `APP_CODE` 32→33)
- [x] App icon: added a "+" badge (green circle, white plus) to
      `core/src/main/res/drawable/ic_launcher_foreground.xml` and
      `core/src/main/ic_launcher-playstore.png`. TV banner left as-is (lower priority,
      different composition).
- [x] Branding text updated: README (title, banner image regenerated, install
      instructions rewritten for Obtainium/GitHub Releases since this fork isn't on
      Google Play/Amazon/F-Droid/IzzyOnDroid under this package name), `welcome`/
      `welcome_text`/`privacy_policy_notice` strings, fastlane metadata
      (`title.txt`/`full_description.txt`), `release-latest.yaml` release-notes text.
      `sync-upstream.yaml` deliberately left untouched (must keep pointing at the real
      upstream repo).
  - Deferred: ~30 non-English translated `strings.xml` files still say "Findroid" in
    the `welcome`/`privacy_policy_notice` strings — cosmetic only, low priority for a
    single-maintainer fork, not blocking.
- [x] GitHub repo renamed `pschmitt/findroid` → `pschmitt/findroidplus` (`gh repo
      rename`), local `origin` remote updated, repo description updated to mention
      this is a vibe-coded fork with better download handling + Sonarr/Radarr
      integration. README badges/store links repointed or removed as appropriate
      (Play/Amazon/F-Droid/IzzyOnDroid links removed — not applicable to this fork;
      stat badges removed rather than repointed at the request of the user).
- [x] Verified: remote build (`just gradle rofl-13.brkn.lol ktfmtCheck
      :app:phone:assembleLibreDebug :app:tv:assembleLibreDebug`) succeeded; installed
      the phone debug build on the Mi Pad 4 and confirmed it launches
      (`dev.pschmitt.findroidplus.debug/dev.jdtech.jellyfin.MainActivity` resumed, no
      fatal errors in logcat).

Status: **done**.
