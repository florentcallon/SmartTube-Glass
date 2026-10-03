# SmartTube Glass — Lot 5 (lecteur vidéo) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Contrôles du lecteur dans une barre verre teintée flottante (arrondie 24 dp), titre et chaîne dans une capsule verre au-dessus, commandes principales centrées, boutons ronds au focus verre/accent, barre de progression plus épaisse, Figtree — sans flou temps réel au-dessus de la vidéo (spec §5.2) et sans changement dans les thèmes classiques.

**Architecture:** Comme aux lots 3–4 : surcharges de mises en page SmartTube/Leanback dont tout ce qui est « glass » passe par des attributs de thème (`GlassDefaults` = rendu upstream). Les thèmes `App.Theme.Glass.*.Player` reçoivent les valeurs verre. Aucun Java upstream modifié.

**Tech Stack:** Android resources, Robolectric 4.11.1, Gradle 7.5 / JDK 17.

**Spec:** `docs/superpowers/specs/2026-10-03-smarttube-glass-design.md` (§5.2 « lecteur vidéo : toujours teinte », §6.3 et sa note validée sur maquette).

## Global Constraints

- Aucun fichier Java upstream modifié. Surcharges à inscrire dans `glass/overrides.lock` : `smarttubetv/src/main/res/layout/lb_playback_transport_controls_row.xml`, `leanback-1.0.0/src/main/res/layout/lb_control_button_primary.xml`, `leanback-1.0.0/src/main/res/layout/lb_control_button_secondary.xml`. Tous les `android:id` et classes conservés.
- Nouveaux attributs (`GlassDefaults` → upstream ; Glass Player → verre) :

| Attribut | `GlassDefaults` | Glass Noir / Rose Player |
|---|---|---|
| `glassPlayerBar` (référence) | `@null` | `@drawable/glass_player_bar` (24 dp, `?attr/glassPlayerTint`, trait `?attr/glassStroke`) |
| `glassPlayerBarInset` (dimension) | `0dp` | `24dp` (marges gauche/droite/bas de la barre) |
| `glassPlayerBarPadding` (dimension) | `0dp` | `16dp` (marge intérieure de la barre) |
| `glassPlayerCapsule` (référence) | `@null` | `@drawable/glass_player_capsule` (18 dp, même teinte) |
| `glassControlsAlignStart` (booléen) | `true` | `false` |
| `glassControlsCenter` (booléen) | `false` | `true` |
| `glassControlButtonPrimary` (référence) | `@drawable/lb_control_button_primary` | `@drawable/glass_control_button` |
| `glassControlButtonSecondary` (référence) | `@drawable/lb_control_button_secondary` | `@drawable/glass_control_button` |

- `glass_control_button` : sélecteur ; focalisé → disque `?attr/glassTintFocus` + trait 1 dp `?attr/glassStroke` ; sinon transparent. Taille inchangée (`lb_control_button_diameter`).
- Barre de progression (ressources, donc toute la variante, Ruling) : `lb_playback_transport_progressbar_bar_height` 6 dp, `…_active_bar_height` 8 dp, `…_active_radius` 9 dp. Couleur de progression = `playbackProgressPrimaryColor` (accent, déjà en place au lot 2) ; curseur blanc (fixé par le code upstream).
- Typographie : `playbackControlsTimeStyle` → `Glass.PlayerTime` (Figtree) dans les thèmes Glass Player.
- **Écarts assumés par rapport à la maquette** : le titre reste au-dessus de la barre (dans une capsule) et non en haut à gauche de l'écran, car il fait partie de la rangée de contrôles Leanback ; toutes les commandes principales de SmartTube sont centrées (lecture, précédent/suivant, retour/avance et les quelques boutons que SmartTube y place), les secondaires restent sous la barre ; l'assombrissement plein écran de Leanback est inchangé (couleur lue sans thème par le code).

## Review Focus

1. **Thèmes classiques** : lecteur identique à SmartTube (aucune barre, capsule ni bouton verre, commandes alignées à gauche). Tests `GlassPlayerTest.classicSchemeKeepsUpstreamPlayer` + box.
2. **Aperçu de recherche** (vignettes `thumbs_row` au-dessus de la barre pendant l'avance rapide) : toujours visible, non masqué par la capsule ni coupé par la barre. Box.
3. **Barre masquée / affichée** (auto-hide, touche Haut `top_edge`) : pas de fond verre résiduel quand les contrôles sont cachés. Box.
4. **Lecture 4K 10 min** : fluide (aucun flou temps réel, uniquement des fonds teintés). Box.
5. **Chat en direct / sous-titres / horloge globale** (`lb_playback_fragment.xml`, non modifié) : inchangés. Box.

---

### Task 1: Barre verre, capsule et commandes centrées

**Files:**
- Modify: `smarttubetv/src/stglass/res/values/glass_attrs.xml`, `glass_defaults.xml`, `glass_themes.xml` (attributs du tableau ; style `Glass.PlayerTime` parent `Widget.Leanback.PlaybackControlsTimeStyle` + Figtree, affecté à `playbackControlsTimeStyle` dans les thèmes Glass Player).
- Create: `smarttubetv/src/stglass/res/drawable/glass_player_bar.xml`, `glass_player_capsule.xml`.
- Create: `smarttubetv/src/stglass/res/layout/lb_playback_transport_controls_row.xml` — copie de l'upstream SmartTube ; `transport_row` : `android:background="?attr/glassPlayerBar"`, marges `?attr/glassPlayerBarInset`, padding vertical `?attr/glassPlayerBarPadding` (le padding horizontal reste `?attr/browsePaddingStart/End`) ; `controls_card` : `android:background="?attr/glassPlayerCapsule"` ; `controls_dock` (rangée principale) : `layout_alignParentStart="?attr/glassControlsAlignStart"`, `layout_centerHorizontal="?attr/glassControlsCenter"`.
- Modify: `glass/overrides.lock`.
- Test: `smarttubetv/src/testStglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassPlayerTest.java` (Robolectric, gonflage de `R.layout.lb_playback_transport_controls_row` avec le thème Player + défauts).

- [ ] **Step 1: Tests qui échouent** : `glassPlayerHasFloatingBar` (Glass Noir Player : `transport_row` a un fond non nul, marges 24 dp ; `controls_card` a un fond ; règle `CENTER_HORIZONTAL` sur `controls_dock` et pas `ALIGN_PARENT_START`) ; `classicSchemeKeepsUpstreamPlayer` (Dark Grey Player : fonds nuls, marges 0, `ALIGN_PARENT_START`) ; `playerTimeUsesFigtree`.
- [ ] **Step 2: Lancer** → FAIL. **Step 3: Implémenter.** **Step 4: Lancer** → PASS (suite complète).
- [ ] **Step 5: Commit** — `feat(glass): floating glass player bar, title capsule, centred primary controls`.

### Task 2: Boutons ronds et barre de progression

**Files:**
- Create: `smarttubetv/src/stglass/res/layout/lb_control_button_primary.xml`, `lb_control_button_secondary.xml` — copies upstream ; `android:src` du bouton = `?attr/glassControlButtonPrimary` / `Secondary`.
- Create: `smarttubetv/src/stglass/res/drawable/glass_control_button.xml`.
- Modify: `smarttubetv/src/stglass/res/values/glass_dimens.xml` (3 dimensions de la barre de progression).
- Modify: `glass/overrides.lock`.
- Test: `GlassPlayerTest.controlButtonsUseGlassFocus` (Glass : `R.id.button` a pour drawable un `StateListDrawable` issu de `glass_control_button` — comparer `getConstantState` à celui de `ContextCompat.getDrawable(ctx, R.drawable.glass_control_button)`), `classicControlButtonsAreUpstream`.

- [ ] **Step 1: Tests qui échouent.** **Step 2: Lancer** → FAIL. **Step 3: Implémenter.** **Step 4: Lancer** → PASS ; `check-overrides.sh` → 0 ; `assembleStglassRelease` → OK.
- [ ] **Step 5: Commit** — `feat(glass): glass control buttons, thicker progress bar`.

### Task 3: Relecture, publication, vérification

- [ ] **Step 1:** relecture indépendante (avant push). **Step 2:** push (utilisateur) → `32.56-glass.7` (inclut le halo du lot 4). **Step 3:** check-list Review Focus + halo des cartes + rendu général du lecteur en Glass Noir et Glass Rose. **Step 4:** ledger.
