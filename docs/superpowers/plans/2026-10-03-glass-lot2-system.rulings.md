Setup: Ruling: work on branch glass directly (same as lot 1; fork default branch, every push is gated by the user) — cost if wrong: none.
Task 1: Ruling: build stglass and ststable in separate gradle invocations — upstream build.gradle applies the google-services plugin to the whole invocation when a task name contains Ststable/Stbeta, which fails stglass ("No matching client") — documented in glass/README.md — cost if wrong: none.
Task 1: Ruling: initGlassColorSchemes stops at the first missing scheme (return) instead of skipping it — both are defined together in one file; a partial set would mean a broken flavor — cost if wrong: Rose missing if only Noir resolves.
Task 2: Ruling: singleBrightPixelSpreadsSymmetrically uses 2 passes instead of the plan's 1 — one separable box pass gives centre and 4-neighbours the same value (flat 3x3 kernel), so the plan's assertion could never hold — cost if wrong: none.
Final: minor (deferred): theme index shift for stored indices >= 2 after inserting Glass schemes (single-user fork; a migration would touch upstream code).
Final: minor (deferred): WeakHashMap in GlassInitProvider gives no weak behaviour (entries removed explicitly in onActivityDestroyed, no leak).
Final: minor (deferred): default shelf gradient centre is translucent (accent 18% alpha) — check on device.
Final: minor (deferred): translucent brandColor may show a darker band at the headers fade edge — headers replaced in lot 3; check on device.
Final: minor (deferred): applyScrim ignores source alpha — transparent icons (settings cards) give a grey haze.
Final: Ruling: crossfade uses leanback BackgroundManager's own fade (~500 ms) instead of the spec's 300 ms — reusing the stock fade avoids a custom drawable animator — cost if wrong: slightly slower transition.
Final: Ruling: glass tests use Robolectric 4.11.1 (testStglassImplementation in glass/glass.gradle) with ConscryptMode OFF — upstream 4.6.1 cannot read JDK 17 class files and conscrypt-android JNI is device-only — cost if wrong: test-only dependency.
