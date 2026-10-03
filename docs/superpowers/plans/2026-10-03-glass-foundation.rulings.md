Setup: Ruling: no worktree — work directly on branch `glass` of a fresh clone (not main/master, no other work in this checkout) — cost if wrong: none, branch is isolated.
Task 1: Ruling: sidebar list scrolls (44dp items, 8dp gap) instead of fitting all entries — 508dp panel height cannot hold 9 airy entries + header + footer — cost if wrong: layout tweak in lot 3.
Task 1: Ruling: version label removed from sidebar footer (kept in Réglages > À propos) — frees vertical space — cost if wrong: trivial.
Ruling: run Task 3 before Task 2 — Task 2 needs a JDK 17 install that requires user approval; Task 3 is independent — cost if wrong: none.
Task 3: Ruling: check-overrides hashes CR-stripped content and gains `--add <path> [lockfile]` — autocrlf checkouts on Windows would otherwise never match CI hashes — cost if wrong: one extra CLI flag.
Task 3: Ruling: check-overrides strips the `*` binary marker Git-for-Windows sha1sum prints — lockfiles written with plain sha1sum stay readable — cost if wrong: none.
Task 3: Ruling: glass/.gitattributes forces LF on *.sh — CRLF checkouts would break bash scripts locally — cost if wrong: none.
Task 4: Ruling: checkout uses ref glass (not the triggering SHA) and release target is `git rev-parse HEAD` — under workflow_call the caller's SHA predates the merge just pushed — cost if wrong: none.
Task 4: Ruling: `latest` release created with --latest=false; versioned releases carry the "Latest" badge — keeps the fixed-tag URL stable — cost if wrong: cosmetic.
Task 5: Ruling: sync pushes the merged upstream tag along with glass — build-release's changelog uses `git describe` on stable tags that a fork may not have — cost if wrong: none.
Task 5: Ruling: upstream workflows (stale, cleanup, virustotal_scan, CI) are disabled from the GitHub UI in Task 6, not deleted — deleting upstream files would create modify/delete conflicts on every upstream change to them — cost if wrong: stale could close our sync issues if left enabled.
Task 6: Ruling: append 4 lines to upstream .gitignore (keystore.properties, *.jks, *.jks.b64) — upstream does not ignore signing files, a local `git add -A` could publish the key — cost if wrong: rare merge conflict at end of .gitignore. Global constraint "1 upstream line" now: build.gradle (1 line) + .gitignore (4 lines).
Task 2: Ruling: override leanbackassistant/src/ststable/res/xml/searchable.xml in stglass (authority org.smarttube.glass), recorded in overrides.lock — ststable hard-codes org.smarttube.stable, so global search suggestions would target the official app — cost if wrong: none.
Task 2: Ruling: also override browse_title ("SmartTube Glass") next to app_name, as ststable does — cost if wrong: cosmetic.
Task 2: Ruling: test-version rejection cases grep -F the range error message — a bare non-zero exit also passes when the build fails for unrelated reasons — cost if wrong: none.
Final: Ruling: re-grade reviewer Minor #7 (multi-line gradle output parsed into UP) to Important — it is hit on every upstream sync (cold Gradle cache) and can turn into a spurious "exceed 99" failure — cost if wrong: one small script.
Final: Ruling: KNOWN_PACKAGES lacks org.smarttube.glass (About screen uses the simple presenter, update check still present) — stands for lot 1, revisit in lot 6 (settings) since it needs an upstream Java edit — cost if wrong: About screen shows fewer entries.
Final: Ruling: no x86 entry in update JSON — x86 TV boxes are out of scope, universal APK is the fallback, same as upstream — cost if wrong: x86 devices download the larger universal APK.
Final: Ruling: reviewer notes stale.yml is workflow_dispatch-only and cleanup.yml skips forks at 32.56s; keep the README advice to disable inherited workflows anyway (future upstream changes) — cost if wrong: one manual click.
Final: minor (deferred): next-revision counts tags instead of max+1 keyed on versionCode (tag deletion / versionName change without versionCode change).
Final: minor (deferred): GH_REPO not set, so failure-issue steps fail when checkout itself failed.
Final: minor (deferred): `git merge --abort` should be `|| true` for non-conflict merge failures.
Final: minor (deferred): keystore.properties is parsed by Properties.load — a backslash in a password would be mangled.
Final: minor (deferred): GitHub disables scheduled workflows after 60 days without repo activity.
Final: Ruling: sync workflow requires a user-created PAT secret (GLASS_SYNC_TOKEN) — GitHub forbids GITHUB_TOKEN pushes touching workflow files — cost if wrong: token expiry stops sync (reported by an issue).
Ruling: commit author email rewritten to 260761052+florentcallon@users.noreply.github.com (GitHub email privacy blocked the push); backup branch backup/glass-before-email-fix — cost if wrong: none, unpublished history
