Setup: Ruling: work on branch glass directly (as lots 1-2) — cost if wrong: none.
Task 2: Ruling: GlassDefaults applied in onActivityCreated, which runs BEFORE MotherActivity.initTheme() (plan said after) — initTheme's setTheme reuses the same Theme object and applies the scheme with force, so Glass schemes override the defaults and classic ones keep them; comment corrected — cost if wrong: Glass values shadowed (would be caught on device: no ambient in Glass).
Final: minor (deferred): HEADERS_DISABLED would get an 88dp gutter (mCanShowHeaders not checked) — unused by SmartTube.
Final: minor (deferred): touch strip widened to 138dp in the rail (touch devices only).
Final: minor (deferred): header items may draw outside the panel's rounded ends when scrolled (no vertical padding/clip on the grid).
Final: minor (deferred): classic schemes paint an opaque brandColor rectangle over the panel in stglass.
Final: minor (deferred): RTL: panel insets use left/right.
Final: minor (deferred): overrides.lock does not watch IconHeaderItemPresenter.java (null-parent inflate) nor dimens.xml (browse_headers_vertical_spacing).
Final: minor (deferred): no automated test of the rail widths in BrowseSupportFragment.
Final: minor (deferred): active rail icon stays at 50% alpha (presenter drives alpha by focus).
Device round 1 (v32.56-glass.3): user photos — pill overflow (leanback 1.2x header select scale), pill touching rail edge, last item overflowing panel, title orbs centred in panel, red bar looks odd. Fixed: select scale 1.0, root paddingEnd 16 (pill 24-72 rail / 24-254 expanded), grid paddingTop 88/Bottom 24 + panel top 88, title paddingStart 24, bar removed + accent icon (GlassHeaders.tintIcon, Glide avatars skipped). Tests 24/24 (2 new RED→GREEN). Ruling: top clipping via grid paddingTop relies on WINDOW_ALIGN_NO_EDGE keeping the fixed keyline from the view top — cost if wrong: header list offset by 88dp, visible on device.
