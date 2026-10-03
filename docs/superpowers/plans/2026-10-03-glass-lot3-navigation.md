# SmartTube Glass — Lot 3 (navigation latérale en verre) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Le menu de gauche devient un panneau verre flottant (arrondi 24 dp) avec des entrées en « pills » de 44 dp espacées de 8 dp ; quand on navigue dans les vidéos, il se réduit à une colonne d'icônes au lieu de disparaître.

**Architecture:** Le panneau et les pills sont des ressources du flavor `stglass` : surcharge de `lb_headers_fragment.xml` (Leanback) et de `icon_header_item.xml` (SmartTube), drawables lisant les attributs de thème `glass*`. La colonne d'icônes vient d'un réglage optionnel ajouté au Leanback embarqué : la dimension `lb_browse_headers_rail_width`, absente partout sauf dans `stglass` (0 = comportement upstream). Repliée, la racine du menu prend cette largeur au lieu de sortir de l'écran ; la géométrie des pills place le début des libellés exactement au bord, si bien que seuls les icônes restent visibles.

**Tech Stack:** Android resources (layouts, drawables avec `?attr`, dimens), androidx.leanback embarqué (`leanback-1.0.0`), Java 8, Robolectric 4.11.1 (tests de thème), Gradle 7.5 / JDK 17.

**Spec:** `docs/superpowers/specs/2026-10-03-smarttube-glass-design.md` (§5.3, §5.4, §6.1 et sa note « Validé sur maquette », §8, §9 lot 3). Maquette : `glass/mockup/index.html`.

## Global Constraints

- Le lot embarque aussi le correctif du lot 2 déjà commité (`e2d07106d`, fonds de carte opaques) : la release de ce lot le publie.
- Fichiers upstream modifiés par ce lot : `leanback-1.0.0/src/main/java/androidx/leanback/app/BrowseSupportFragment.java` et `HeadersSupportFragment.java`, quelques lignes chacun, sans effet quand `lb_browse_headers_rail_width` n'est pas défini. Les ajouter à la liste de `glass/README.md`.
- Fichiers upstream surchargés (à inscrire avec `glass/scripts/check-overrides.sh --add`) : `leanback-1.0.0/src/main/res/layout/lb_headers_fragment.xml`, `smarttubetv/src/main/res/layout/icon_header_item.xml`. Garder tous leurs `android:id` (`browse_headers_root`, `browse_headers`, `fade_out_edge`, `header_icon`, `header_label`) et leurs classes de vue (`VerticalGridView` avec `style="?attr/headersVerticalGridStyle"`, `NonOverlappingLinearLayout`, `HeaderMarqueeTextViewCompat`).
- Géométrie (dp), à respecter exactement pour que le repli masque les libellés :
  - racine du menu : largeur dépliée = `lb_browse_headers_width` (270, upstream), `paddingEnd` 8 ; largeur repliée = `lb_browse_headers_rail_width` = **88** ;
  - panneau : fond de la racine, encart 16 en haut, en bas et à gauche, rayon 24 → zone visible repliée = 16..80 ;
  - pill : hauteur 44, marge horizontale 8 dans le panneau, rayon 14, `paddingStart` 12, icône 24 → icône centrée à x = 48 ;
  - libellé : `marginStart` 20 après l'icône → commence à x = 80, au bord exact de la zone repliée ;
  - espacement vertical des pills : `browse_headers_vertical_spacing` = 8 (surcharge de la dimension SmartTube).
- Couleurs : panneau = `?attr/glassTint`, bord = `?attr/glassStroke` (plus clair en haut via un dégradé), pill focalisée = `?attr/glassTintFocus` + barre accent 3 dp à gauche (`?attr/glassAccent`), libellé blanc, icône teintée blanc. Les thèmes Glass passent `brandColor` à `@android:color/transparent` (le panneau est dessiné par la mise en page ; corrige aussi la bande sombre du bord, mineur du lot 2).
- L'élément actif quand le menu est replié reste signalé par l'opacité (les autres entrées sont à 50 % par `lb_browse_header_unselect_alpha`, comportement upstream) : pas de code Java SmartTube modifié.
- Hors périmètre de ce lot (reporté) : en-tête « logo + compte » et carte « Réglages » en bas du panneau (la barre de titre upstream garde compte et recherche ; « Réglages » reste une entrée du menu), police Figtree (lot 4, avec les cartes).

## Review Focus

1. **Flavors upstream** : sans `lb_browse_headers_rail_width`, le menu doit se comporter exactement comme avant (sortir de l'écran, éléments invisibles). Vérifié par un build `ststable` et une revue du diff Leanback (aucune branche active si la dimension vaut 0).
2. **Focus avec le menu replié** : depuis une rangée, Haut/Bas ne doivent jamais sauter dans la colonne d'icônes ; seul Gauche (ou Retour) déplie le menu. Vérifié sur la box.
3. **Changement de section depuis le menu déplié puis Droite** : la colonne affiche l'icône de la nouvelle section à pleine opacité. Vérifié sur la box.
4. **Libellés longs et autres langues** (« Abonnements », « À regarder plus tard », allemand) : jamais visibles en mode replié, défilement marquee intact en mode déplié.
5. **Thème classique** (Dark Grey, Teal) dans `stglass` : le menu garde la colonne d'icônes avec les couleurs neutres de `GlassDefaults`, pas de crash, et toujours aucun fond d'ambiance. Tests Robolectric `GlassThemeTest` (Task 2) + vérification sur la box.

---

### Task 1: Colonne d'icônes optionnelle dans le Leanback embarqué

**Files:**
- Modify: `leanback-1.0.0/src/main/java/androidx/leanback/app/BrowseSupportFragment.java`
- Modify: `leanback-1.0.0/src/main/java/androidx/leanback/app/HeadersSupportFragment.java`
- Create: `smarttubetv/src/stglass/res/values/glass_dimens.xml` — `lb_browse_headers_rail_width` = 88dp, `browse_headers_vertical_spacing` = 8dp.

**Interfaces:**
- Produces : dimension optionnelle `lb_browse_headers_rail_width` lue par nom (`getResources().getIdentifier(…, "dimen", context.getPackageName())`, 0 si absente) ; `HeadersSupportFragment.setChildrenVisibleWhenDisabled(boolean)` (package-private).

- [ ] **Step 1: Modifier `BrowseSupportFragment`** — au même endroit que la lecture de `mContainerListMarginStart`, lire la largeur de rail (champ `mHeadersRailWidth`, commentaire `// SmartTube Glass: optional icon rail kept on screen when headers are hidden (0 = stock behaviour)`) et, si > 0, appeler `mHeadersSupportFragment.setChildrenVisibleWhenDisabled(true)` à la création du fragment d'en-têtes. Dans `setHeadersOnScreen(onScreen)`, si rail > 0 : `marginStart` 0 et `lp.width` = largeur d'origine mémorisée (dépliée) ou rail (repliée) ; sinon code inchangé. Dans `expandMainFragment(expand)` : marge de départ dépliée = `mHeadersRailWidth` au lieu de 0. Aucune autre ligne touchée.
- [ ] **Step 2: Modifier `HeadersSupportFragment`** — champ `mChildrenVisibleWhenDisabled` + setter ; dans `updateListViewVisibility()`, enfants visibles si `mHeadersEnabled || mChildrenVisibleWhenDisabled`.
- [ ] **Step 3: Build upstream** — `./gradlew :smarttubetv:assembleStstableDebug` → BUILD SUCCESSFUL ; `aapt2 dump resources` de l'APK ststable ne contient pas `lb_browse_headers_rail_width` (Review Focus 1).
- [ ] **Step 4: Build glass** — `./gradlew :smarttubetv:assembleStglassDebug` (commande séparée, cf. README) → BUILD SUCCESSFUL ; l'APK contient `dimen/lb_browse_headers_rail_width` = 88dp.
- [ ] **Step 5: Commit** — `feat(glass): optional icon rail for collapsed Leanback headers`.

### Task 2: Panneau verre et pills

**Files:**
- Create: `smarttubetv/src/stglass/res/layout/lb_headers_fragment.xml` — copie de l'upstream ; racine avec `android:background="@drawable/glass_headers_panel"` ; `fade_out_edge` conservé avec largeur 0dp.
- Create: `smarttubetv/src/stglass/res/layout/icon_header_item.xml` — même structure que l'upstream ; hauteur 44dp, `layout_marginHorizontal` 8dp, fond `@drawable/glass_header_pill`, `paddingStart` 12dp, icône 24dp teintée blanc, libellé `marginStart` 20dp, `gravity center_vertical`.
- Create: `smarttubetv/src/stglass/res/drawable/glass_headers_panel.xml` — `layer-list` : `inset` 16dp (haut, bas, gauche), forme arrondie 24dp remplie `?attr/glassTint`, trait 1dp `?attr/glassStroke`, et reflet supérieur (dégradé linéaire du blanc 10 % vers transparent sur le premier tiers).
- Create: `smarttubetv/src/stglass/res/drawable/glass_header_pill.xml` — `selector` : `state_focused` → `layer-list` (forme 14dp `?attr/glassTintFocus` + trait 1dp `?attr/glassStroke`, barre 3dp `?attr/glassAccent` à gauche, encart vertical 12dp) ; défaut → transparent.
- Modify: `smarttubetv/src/stglass/res/values/glass_themes.xml` — `brandColor` = `@android:color/transparent` dans les deux thèmes Browse.
- Create: `smarttubetv/src/stglass/res/values/glass_defaults.xml` — style `GlassDefaults` (sans parent) donnant les 7 attributs `glass*` en valeurs neutres : scrim `#B3000000`, teinte `#14FFFFFF`, focus `#29FFFFFF`, trait `#1FFFFFFF`, accent `#FF0033`, texte secondaire `#99FFFFFF`, lecteur `#8C0A0A0C`.
- Create: `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassTheme.java` — `static void applyDefaults(Resources.Theme theme)` = `theme.applyStyle(R.style.GlassDefaults, false)` (`false` : n'écrase pas les valeurs d'un thème Glass).
- Modify: `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassInitProvider.java` — `onActivityCreated` → `GlassTheme.applyDefaults(activity.getTheme())` (appelé pendant `super.onCreate()`, donc après `setTheme` et avant `setContentView`).
- Modify: `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassAmbientController.java` — le test « thème Glass ? » ne peut plus reposer sur la présence de `glassScrim` (les défauts la fournissent) : il utilise un attribut booléen `glassAmbient` (défaut `false` dans `GlassDefaults`, `true` dans les deux thèmes Glass Browse).
- Modify: `glass/overrides.lock` (2 lignes), `glass/README.md`.
- Test: `smarttubetv/src/testStglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassThemeTest.java` — nouveau cas.

**Interfaces:**
- Consumes : attributs `glassTint`, `glassTintFocus`, `glassStroke`, `glassAccent` (lot 2) ; `GlassInitProvider` (lot 2).
- Produces : `GlassTheme.applyDefaults(Resources.Theme)`, attribut booléen `glassAmbient`.

- [ ] **Step 1: Écrire les tests qui échouent** (`GlassThemeTest`, contexte thémé puis `GlassTheme.applyDefaults(context.getTheme())`) :
  - `headersDrawablesInflateInEveryScheme` : pour `App.Theme.Glass.Noir.Browse`, `App.Theme.Glass.Rose.Browse` et `App.Theme.DarkGrey.Browse`, `ContextCompat.getDrawable(ctx, R.drawable.glass_headers_panel)` et `R.drawable.glass_header_pill` ne lèvent pas d'exception et ne sont pas null ;
  - `defaultsDoNotOverrideGlassThemes` : avec Glass Rose, `glassAccent` reste `#FF5A5F` et `glassAmbient` vaut `true` ;
  - `classicSchemeGetsNeutralDefaults` : avec Dark Grey, `glassAccent` vaut `#FF0033` et `glassAmbient` vaut `false`.
- [ ] **Step 2: Lancer** `./gradlew :smarttubetv:testStglassDebugUnitTest --tests '*GlassThemeTest'` → FAIL (drawables, style et classe absents).
- [ ] **Step 3: Écrire les ressources et le code** listés ci-dessus.
- [ ] **Step 4: Lancer** la même commande → PASS ; puis toute la suite `testStglassDebugUnitTest` → PASS.
- [ ] **Step 5: Inscrire les surcharges** — `bash glass/scripts/check-overrides.sh --add leanback-1.0.0/src/main/res/layout/lb_headers_fragment.xml` et `… --add smarttubetv/src/main/res/layout/icon_header_item.xml` ; `bash glass/scripts/check-overrides.sh` → exit 0.
- [ ] **Step 6: Build release** — `./gradlew :smarttubetv:assembleStglassRelease` (lint compris) → BUILD SUCCESSFUL.
- [ ] **Step 7: Commit** — `feat(glass): glass side panel with pill entries`.

### Task 3: Publication et vérification sur la box

- [ ] **Step 1: Relecture indépendante** de la branche depuis le début du lot (avant push).
- [ ] **Step 2: Push** (par l'utilisateur) → release `32.56-glass.3`, mise à jour sur la box.
- [ ] **Step 3: Check-list** :
  - menu déplié : panneau verre flottant arrondi, pills de 44 dp bien espacées, pill focalisée claire avec barre accent ;
  - Droite vers les vidéos : le menu se réduit en colonne d'icônes (animation fluide), aucun libellé ne dépasse, l'icône de la section active est à pleine opacité ;
  - Gauche ou Retour : le menu se redéploie ; Haut/Bas dans les rangées ne sautent jamais dans la colonne (Focus 2) ;
  - changement de section puis Droite (Focus 3) ; langue allemande ou libellés longs (Focus 4) ;
  - thème Dark Grey après redémarrage : colonne d'icônes neutre, aucun plantage (Focus 5) ;
  - réglages : plus de carrés sur les tuiles (correctif lot 2) ;
  - lecture 4K : inchangée.
- [ ] **Step 4: Consigner** dans le ledger.
