Setup: Ruling: execution started right after the user answered the plan's two open decisions (free-text cards; Figtree + minSdk 23) — cost if wrong: plan changes requested mid-lot.
Final: Ruling: classic schemes in stglass show rounded thumbnails on their opaque square card background (corner wedges) — the layout override is flavor-wide; same deferral as lot 3 for classic schemes — cost if wrong: cosmetic on non-default schemes.
Final: minor (deferred): animated card preview covers the focus outline and rounded corners (preview_container outside the rounded frame; previews off by default).
Final: minor (deferred): focused cards may cast a rectangular shadow (UI_TWEAK_ROUNDED_CORNERS outline + 10dp focused Z) — check on device.
Final: minor (deferred): glass_figtree.xml comment about API 23-25 fallback is wrong (framework TextView falls back to system sans-serif; TTF default instance is wght 300).
Final: minor (deferred): Figtree not applied to channel/settings/tag titles nor player-suggestion cards.
Final: minor (deferred): progress fill end is square (clip cuts the rounded shape).
Final: minor (deferred): state_focused item of glass_card_focus.xml never triggers.
Final: minor (deferred): test gaps — card setSelected reaching the rounded frame, settings foreground.
Final: minor (deferred): overrides.lock does not watch ComplexImageView.java, lb_image_card_view.xml, colors.xml, card presenters.
Final: minor (deferred): GlassFadingGridView may mis-fade for one frame when getChildAdapterPosition returns NO_POSITION.
Ruling: zero focused-card elevation is flavor-wide (resource, not theme) — classic schemes lose the upstream focus shadow — cost if wrong: subtle visual difference on classic schemes.
Ruling: menu label font (Figtree) and 1.0 header select scale stay flavor-wide in classic schemes — resources/layout attrs, not worth extra attributes — cost if wrong: minor difference from upstream.
Ruling: glow drawn by a dedicated view rather than coloured elevation shadows — shadow colour needs API 28 and shadow alpha is a window-wide theme value (would darken the side panel shadow too) — cost if wrong: one extra view per card.
