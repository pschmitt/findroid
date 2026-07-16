# TODO

## FINDROID-1: Rebrand fork to Findroid+

Rename the package and app to distinguish this fork from upstream Findroid.

- App name: `Findroid+`
- Package / applicationId: `dev.pschmitt.findroidplus` (app modules only; library module
  namespaces can stay `dev.jdtech.jellyfin.*` unless we decide otherwise)
- Bump version to `2.0.0`
- Update the app icon — consider adding a "+" badge/mark to the existing icon
- Update user-facing branding references (README, about screen, store listing strings)
  where they say "Findroid" — but NOT the upstream sync workflow
  (`.github/workflows/sync-upstream.yaml`), which must keep pointing at the real
  upstream repo (`jarnedemeulemeester/findroid`)
- Scope touches: `app/phone/build.gradle.kts`, `app/tv/build.gradle.kts`,
  `settings.gradle.kts` (`rootProject.name`), `README.md`, translated strings
  mentioning "Findroid" across `core`/`setup` modules, CI/release workflows
  under `.github/workflows/`

Status: **not started** — paused, pending completion of other in-flight agent work.
