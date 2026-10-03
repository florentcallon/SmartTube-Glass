# SmartTube Glass — Lot 4 (cartes vidéo et typographie) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cartes « texte libre » : miniature arrondie 16 dp, titre et chaîne posés directement sur le fond d'ambiance, contour accent au focus, pastilles de durée arrondies, barre de progression arrondie ; police Figtree sur les cartes, les titres de rangées et le menu.

**Architecture:** Aucun code Java SmartTube modifié. Les couleurs que les presenters upstream appliquent (fonds et textes de carte) deviennent transparentes ou suivent le thème : attributs de thème `cardDefaultBackground`/`cardSelectedBackground` et surcharge de trois ressources couleur upstream par des `ColorStateList` à attribut de thème (API 23). L'arrondi vient d'un petit conteneur `GlassRoundedFrameLayout` (contour arrondi + `clipToOutline`) placé dans la surcharge de `text_badge_image_view.xml`. La typographie passe par les styles de thème Leanback (`imageCardViewTitleStyle`, `imageCardViewContentStyle`, `rowHeaderStyle`).

**Tech Stack:** Android resources (styles, color state lists, font family), Java 8, Robolectric 4.11.1, Gradle 7.5 / JDK 17.

**Spec:** `docs/superpowers/specs/2026-10-03-smarttube-glass-design.md` (§5.3, §5.4, §6.2, §9 lot 4). Maquette : `glass/mockup/index.html`.

## Global Constraints

- Décisions utilisateur (2026-10-03) : style **texte libre** (pas de bande sous le titre, même au focus) ; police **Figtree** (OFL) téléchargée depuis `https://github.com/google/fonts/raw/main/ofl/figtree/` ; **minSdk 23** pour `stglass` (couleurs à attribut de thème).
- Aucun fichier Java upstream modifié. Fichiers upstream surchargés à inscrire dans `glass/overrides.lock` : `smarttubetv/src/main/res/layout/text_badge_image_view.xml`, `smarttubetv/src/main/res/layout/settings_card.xml`. Garder tous leurs `android:id` et classes.
- Ressources couleur upstream redéfinies dans `stglass` (fichiers `res/color/*.xml`, sélecteur à un seul `<item android:color="?attr/…"/>`) : `card_default_text` → `?attr/glassCardText`, `card_selected_text_grey` → `?attr/glassCardSelectedText`, `card_selected_background_white` → `?attr/glassCardSelectedBackground`. Utilisées par `VideoCardPresenter`, `ChannelCardPresenter`, `SettingsCardPresenter`, `TagPresenter`.
- Nouveaux attributs (`glass_attrs.xml`) et valeurs :

| Attribut | `GlassDefaults` (thèmes classiques = upstream) | Glass Noir / Rose |
|---|---|---|
| `glassCardText` | `#FFFFFF` | `#C7FFFFFF` |
| `glassCardSelectedText` | `#343434` | `#FFFFFF` |
| `glassCardSelectedBackground` | `#FFFFFF` | `#00000000` |

- Thèmes Glass Browse : `cardDefaultBackground` et `cardSelectedBackground` = `@android:color/transparent`. Règle de non-régression (cause des « carrés » du lot 2) : un fond de carte est soit opaque, soit totalement transparent, jamais translucide.
- Géométrie : miniature arrondie 16 dp ; contour de focus 2 dp `?attr/glassAccent` arrondi 16 dp (états `selected` et `focused`) ; pastille de durée/badge arrondie 6 dp, marges 6 dp ; barre de progression 4 dp arrondie, marges horizontales 10 dp et basse 6 dp, piste blanc 25 %, progression `?attr/glassAccent` ; carte réglages : contour de focus 2 dp arrondi 12 dp.
- Police : `res/font/figtree_variable.ttf` + famille `res/font/glass_figtree.xml` (poids 400 et 600 via `android:fontVariationSettings`, API 26+, repli sur l'instance par défaut en dessous) ; licence copiée dans `glass/fonts/OFL.txt`.
- Les correctifs de suivi du lot 3 (fondus du menu, fond assombri) partent avec la release de ce lot.

## Review Focus

1. **Thèmes classiques dans `stglass`** (Dark Grey, Teal) : cartes identiques à SmartTube (fond, texte gris foncé sur fond blanc au focus), grâce aux valeurs `GlassDefaults` des trois nouveaux attributs. Test `GlassThemeTest.classicSchemeKeepsUpstreamCardColours`.
2. **Avatars et cartes chaîne / tags / réglages** : mêmes presenters que les vidéos pour les couleurs → aucun texte blanc sur fond blanc ni gris sur fond sombre. Test sur les trois ressources couleur dans les deux familles de thèmes + vérification box (Abonnements → Chaînes, Réglages).
3. **Aperçu animé** (`preview_container`, lecture muette au focus) : reste au-dessus de la miniature ; il peut rester à angles droits (hors du conteneur arrondi), sans décalage. Vérification box.
4. **Badges « EN DIRECT » / « NOUVEAU »** colorés par `setBadgeColor` (fond uni remplacé par le code) : restent arrondis grâce au conteneur, lisibles. Vérification box.
5. **Translucidité empilée** (cause des carrés) : aucune couleur de fond de carte partiellement transparente dans les thèmes Glass. Test `GlassThemeTest.cardBackgroundsAreOpaqueOrTransparent`.

---

### Task 1: Figtree et Android 6.0 minimum

**Files:**
- Modify: `glass/glass.gradle` (`minSdkVersion 23`, commentaire : couleurs à attribut de thème).
- Create: `smarttubetv/src/stglass/res/font/figtree_variable.ttf` (téléchargé), `smarttubetv/src/stglass/res/font/glass_figtree.xml`, `glass/fonts/OFL.txt` (téléchargé).
- Modify: `smarttubetv/src/stglass/res/values/glass_themes.xml` — styles `Glass.CardTitle` (parent `Widget.Leanback.ImageCardView.TitleStyle`), `Glass.CardContent` (parent `Widget.Leanback.ImageCardView.ContentStyle`), `Glass.RowHeader` (parent `Widget.Leanback.Row.Header`) avec `android:fontFamily="@font/glass_figtree"` ; affectés à `imageCardViewTitleStyle`, `imageCardViewContentStyle`, `rowHeaderStyle` dans les deux thèmes Browse.
- Modify: `smarttubetv/src/stglass/res/layout/icon_header_item.xml` — `android:fontFamily="@font/glass_figtree"` sur le libellé.
- Test: `GlassThemeTest.cardAndRowTextsUseFigtree`.

- [ ] **Step 1: Télécharger** `Figtree[wght].ttf` et `OFL.txt` depuis `https://github.com/google/fonts/raw/main/ofl/figtree/` ; vérifier `file` (TrueType) et la taille.
- [ ] **Step 2: Test qui échoue** — `cardAndRowTextsUseFigtree` : pour les deux thèmes Glass, le style résolu par `imageCardViewTitleStyle`, `imageCardViewContentStyle` et `rowHeaderStyle` a `android:fontFamily` = `R.font.glass_figtree` (`TypedArray.getResourceId`).
- [ ] **Step 3: Lancer** → FAIL. **Step 4: Implémenter.** **Step 5: Lancer** → PASS ; build `assembleStglassDebug` ; `aapt2 dump badging` → `minSdkVersion:'23'`.
- [ ] **Step 6: Commit** — `feat(glass): Figtree typography, minSdk 23`.

### Task 2: Couleurs « texte libre »

**Files:**
- Modify: `glass_attrs.xml`, `glass_defaults.xml`, `glass_themes.xml` (valeurs du tableau ; `cardDefaultBackground`/`cardSelectedBackground` transparents).
- Create: `smarttubetv/src/stglass/res/color/card_default_text.xml`, `card_selected_text_grey.xml`, `card_selected_background_white.xml`.
- Create: `smarttubetv/src/stglass/res/drawable/glass_settings_focus.xml` (sélecteur `state_focused` → contour 2 dp accent arrondi 12 dp, sinon transparent).
- Create: `smarttubetv/src/stglass/res/layout/settings_card.xml` — copie de l'upstream + `android:foreground="@drawable/glass_settings_focus"` sur la racine.
- Modify: `glass/overrides.lock` (+ `settings_card.xml`).
- Test: `GlassThemeTest` — remplacer `cardBackgroundsAreOpaque` par `cardBackgroundsAreOpaqueOrTransparent` (alpha 0 ou 255 pour `cardDefaultBackground` et `cardSelectedBackground`, deux thèmes Glass) ; ajouter `glassSchemesUseFreeTextCardColours` (Glass Rose : `card_default_text` = `#C7FFFFFF`, `card_selected_text_grey` = `#FFFFFFFF`, `card_selected_background_white` = `#00000000`, via `ContextCompat.getColor` sur le contexte thémé) et `classicSchemeKeepsUpstreamCardColours` (Dark Grey + défauts : `#FFFFFFFF`, `#FF343434`, `#FFFFFFFF`).

- [ ] **Step 1: Tests qui échouent** (ci-dessus). **Step 2: Lancer** → FAIL.
- [ ] **Step 3: Implémenter.** **Step 4: Lancer** → PASS (toute la suite).
- [ ] **Step 5: Commit** — `feat(glass): free-text cards - transparent card backgrounds, theme-driven card text`.

### Task 3: Miniature arrondie, badges et progression

**Files:**
- Create: `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassRoundedFrameLayout.java` — `FrameLayout` lisant l'attribut `glassCornerRadius` (dimension) ; `ViewOutlineProvider` en rectangle arrondi ; `setClipToOutline(true)` ; méthode `float getCornerRadius()`.
- Modify: `glass_attrs.xml` — `<declare-styleable name="GlassRoundedFrameLayout"><attr name="glassCornerRadius" format="dimension"/></declare-styleable>`.
- Create: `smarttubetv/src/stglass/res/layout/text_badge_image_view.xml` — copie de l'upstream ; `main_image` enveloppée dans un `GlassRoundedFrameLayout` (16 dp, `android:foreground="@drawable/glass_card_focus"`) ; `extra_text_badge` enveloppé dans un `GlassRoundedFrameLayout` (6 dp, marges fin/bas 6 dp, `layout_gravity end`) ; `clip_progress` avec marges 10/10/6 dp et hauteur 4 dp.
- Create: `smarttubetv/src/stglass/res/drawable/glass_card_focus.xml` (sélecteur `state_selected` et `state_focused` → contour 2 dp `?attr/glassAccent` arrondi 16 dp, sinon transparent), `glass_card_progress.xml` (`layer-list` : piste `#40FFFFFF` arrondie 2 dp, `clip` progression `?attr/glassAccent` arrondie 2 dp).
- Modify: `glass_themes.xml` — style `Glass.CardProgress` (parent `ProgressBarHorizontal.Dark`, `android:progressDrawable` = `@drawable/glass_card_progress`) affecté à `cardProgressStyle` dans les deux thèmes Browse.
- Modify: `glass/overrides.lock` (+ `text_badge_image_view.xml`), `glass/README.md` (ressources couleur upstream redéfinies).
- Test: `smarttubetv/src/testStglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassCardTest.java`.

**Interfaces:**
- Produces : `GlassRoundedFrameLayout` (réutilisable aux lots 5–6).

- [ ] **Step 1: Tests qui échouent** (`GlassCardTest`, Robolectric, thème Glass Noir + défauts) : `thumbnailIsClippedToRoundedCorners` — gonfler `R.layout.text_badge_image_view` dans un `RelativeLayout` ; le parent de `R.id.main_image` est un `GlassRoundedFrameLayout`, `getClipToOutline()` vrai, `getCornerRadius()` = 16 dp ; `badgeIsRounded` — parent de `R.id.extra_text_badge` arrondi à 6 dp ; `badgeColourFromCodeKeepsRoundedClip` — après `badge.setBackgroundColor(Color.RED)`, le parent clippe toujours ; `progressUsesGlassDrawable` — le style `cardProgressStyle` résolu a `android:progressDrawable` = `R.drawable.glass_card_progress`.
- [ ] **Step 2: Lancer** → FAIL. **Step 3: Implémenter.** **Step 4: Lancer** → PASS (toute la suite) ; `check-overrides.sh` → 0 ; `assembleStglassRelease` (lint) → OK.
- [ ] **Step 5: Commit** — `feat(glass): rounded thumbnails, pill badges, rounded progress`.

### Task 4: Relecture, publication et vérification sur la box

- [ ] **Step 1: Relecture indépendante** de la branche depuis le début du lot (avant push).
- [ ] **Step 2: Push** (utilisateur) → `32.56-glass.5`.
- [ ] **Step 3: Check-list** : accueil (miniatures arrondies, texte libre lisible sur fond clair et foncé, contour accent + zoom au focus, pastilles arrondies, progression arrondie), aperçu animé au focus (Focus 3), badges EN DIRECT/NOUVEAU (Focus 4), Abonnements → Chaînes et Réglages (Focus 2), Figtree visible (cartes, rangées, menu), thème Dark Grey après redémarrage (Focus 1), menu : fondus haut/bas et fond plus sombre (suivi lot 3).
- [ ] **Step 4: Consigner** dans le ledger.
