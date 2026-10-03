# SmartTube Glass — Lot 2 (thèmes glass & fond d'ambiance) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rendre sélectionnables les thèmes **Glass Noir** (par défaut) et **Glass Rose**, et faire suivre au fond d'écran la miniature floutée de la vidéo sélectionnée, sans modifier d'autre fichier SmartTube que `MainUIData.java`.

**Architecture:** Les styles `App.Theme.Glass.*` (ressources du flavor `stglass`) héritent des styles Dark Grey éprouvés et n'en changent que les couleurs. `MainUIData` les insère en position 1 et 2 de la liste des thèmes, uniquement s'ils existent (donc jamais dans les flavors upstream). Un `ContentProvider` déclaré par le flavor enregistre des callbacks d'activité qui écoutent le focus : quand une carte vidéo prend le focus, sa miniature déjà chargée est réduite, floutée (algorithme Java pur, testé) et posée avec un voile dans le `BackgroundManager` Leanback.

**Tech Stack:** Android resources (styles, attrs, colors, drawables), Java 8, androidx.leanback `BackgroundManager` / `ImageCardView`, JUnit 4.12 (`src/testStglass`), Gradle 7.5 / JDK 17.

**Spec:** `docs/superpowers/specs/2026-10-03-smarttube-glass-design.md` (§2, §5.1, §5.3, §7, §8, §9 lot 2).

## Global Constraints

- Couleurs exactes de la spec §5.3 (Glass Noir / Glass Rose) : voile du fond `#B3000000` / `#A62A0612`, teinte panneau `#14FFFFFF` / `#1AFF5A7A`, teinte focus `#29FFFFFF` / `#33FF5A7A`, bord `#1FFFFFFF` / `#2EFFB3C1`, accent `#FF0033` / `#FF5A5F`, texte secondaire `#99FFFFFF` / `#A6FFFFFF`, teinte lecteur `#8C0A0A0C` / `#8C1A0710`.
- Tout le code Java du fork vit dans `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/` ; ses tests dans `smarttubetv/src/testStglass/java/com/liskovsoft/smartyoutubetv2/glass/` ; commande de test : `./gradlew :smarttubetv:testStglassDebugUnitTest`.
- Seul fichier Java upstream modifié : `common/src/main/java/com/liskovsoft/smartyoutubetv2/common/prefs/MainUIData.java` (1 appel dans le constructeur + 1 méthode privée en fin de classe). Le noter dans `glass/README.md` (section *Principe*).
- `android:windowBackground` des thèmes reste **opaque** (commentaire upstream : le `BackgroundManager` ne marche pas sinon).
- Aucun appel réseau ajouté : le fond d'ambiance réutilise le bitmap déjà affiché par la carte.
- Le fond d'ambiance ne s'applique qu'avec un thème Glass, jamais dans `PlaybackActivity`.
- **Précisions par rapport à la spec §9** (à reporter dans la spec en Task 1) : `GlassPanelView`, `GlassBlurPolicy` et la police Figtree passent au lot 3, où ils sont utilisés pour la première fois ; Glass Noir devient le thème par défaut par sa position (index 1 = défaut upstream) et non par `GlassInitProvider`.
- Carte sélectionnée : le fond reste quasi blanc (`#EBFFFFFF`) parce que la couleur de texte sélectionné est fixée par le code upstream (`card_selected_text_grey`) ; le vrai style de carte arrive au lot 4.

## Review Focus

1. **Préférence de thème déjà enregistrée** (la box a l'index 1 = ancien Dark Grey) → après mise à jour, index 1 = Glass Noir ; aucun index hors bornes. Vérifié en Task 4 sur la box.
2. **Flavors upstream (stbeta, ststable)** : les styles Glass n'y existent pas → `MainUIData` ne doit rien ajouter, pas de thème « vide ». Test : build `assembleStstableDebug` + vérification `aapt2` qu'aucune chaîne `color_scheme_glass_*` n'y figure, en Task 1.
3. **Miniature pas encore chargée, drawable non bitmap ou de taille 0** (carte qui vient d'apparaître, placeholder, carte « Charger plus ») → pas de crash, fond inchangé. Test unitaire `GlassAmbientTest` + garde dans le contrôleur (Task 3).
4. **Défilement rapide dans une rangée** → un seul calcul de fond après 300 ms d'arrêt, pas un par carte traversée. Test unitaire du debounce (Task 3).
5. **Thème classique choisi** (Teal, Dark Grey…) → aucun fond d'ambiance, rendu identique à SmartTube. Vérifié en Task 4.

---

### Task 1: Thèmes Glass Noir / Glass Rose et enregistrement

**Files:**
- Create: `smarttubetv/src/stglass/res/values/glass_attrs.xml` — `<attr format="color">` : `glassScrim`, `glassTint`, `glassTintFocus`, `glassStroke`, `glassAccent`, `glassTextSecondary`, `glassPlayerTint`.
- Create: `smarttubetv/src/stglass/res/values/glass_colors.xml` — `glass_noir_*` et `glass_rose_*` pour les 7 valeurs des Global Constraints, plus `glass_card_selected` = `#EBFFFFFF`, `glass_noir_header` = `#B30E0E12`, `glass_rose_header` = `#B3240812`, `glass_noir_shelf` = `#0B0B0E`, `glass_rose_shelf` = `#14060B`.
- Create: `smarttubetv/src/stglass/res/drawable/glass_noir_shelf.xml`, `glass_rose_shelf.xml` — dégradé radial (centre 30 %/20 %, couleur accent à 18 % d'alpha → couleur `*_shelf`), fond par défaut tant qu'aucune carte n'a le focus.
- Create: `smarttubetv/src/stglass/res/values/glass_themes.xml` — pour X ∈ {Noir, Rose} :
  - `App.Theme.Glass.X.Browse` (parent `App.Theme.DarkGrey.Browse`) : `android:windowBackground` = `@color/glass_x_shelf`, `shelfBackground` = `@drawable/glass_x_shelf`, `brandColor` = `@color/glass_x_header`, `brandAccentColor` = accent, `cardDefaultBackground` = teinte panneau, `cardSelectedBackground` = `@color/glass_card_selected`, `android:colorAccent` = accent, et les 7 attributs `glass*`.
  - `App.Theme.Glass.X.Player` (parent `App.Theme.DarkGrey.Player`) : `playbackProgressPrimaryColor` et `playbackControlsIconHighlightColor` = accent, `android:colorAccent` = accent, et les 7 attributs `glass*`.
  - `App.Theme.Glass.X.Preferences` (parent `App.Theme.DarkGrey.Preferences`) : `android:colorAccent` = accent.
- Modify: `smarttubetv/src/stglass/res/values/strings.xml` — `color_scheme_glass_noir` = `Glass Noir`, `color_scheme_glass_rose` = `Glass Rose` (`translatable="false"`).
- Modify: `common/src/main/java/com/liskovsoft/smartyoutubetv2/common/prefs/MainUIData.java` — dans le constructeur, ligne suivant `initColorSchemes();` : `initGlassColorSchemes();` ; en fin de classe, `private void initGlassColorSchemes()` qui, pour chaque thème `(nom de chaîne, préfixe de style)` dans l'ordre Noir puis Rose, résout `Helpers.getResourceId(<préfixe>.Browse, "style", mContext)` et la chaîne ; si l'un vaut ≤ 0 → ne rien ajouter ; sinon `mColorSchemes.add(1 + i, new ColorScheme(nameResId, <préfixe>.Player, <préfixe>.Browse, <préfixe>.Preferences, mContext))`. Commentaire d'une ligne : thèmes du fork SmartTube Glass, absents des autres flavors, insérés en tête pour que les thèmes ajoutés plus tard en fin de liste ne décalent pas les préférences.
- Modify: `glass/README.md` (liste des fichiers upstream modifiés), `docs/superpowers/specs/2026-10-03-smarttube-glass-design.md` (§9 : précisions des Global Constraints).

**Interfaces:**
- Produces : attributs de thème `R.attr.glassScrim`, `glassTint`, `glassTintFocus`, `glassStroke`, `glassAccent`, `glassTextSecondary`, `glassPlayerTint` (lus par Task 3 et les lots suivants) ; styles `App.Theme.Glass.{Noir,Rose}.{Browse,Player,Preferences}` ; liste de thèmes `[Teal, Glass Noir, Glass Rose, Dark Grey, Red, …]` en stglass.

- [ ] **Step 1: Écrire les ressources** listées ci-dessus.
- [ ] **Step 2: Modifier `MainUIData`** comme décrit.
- [ ] **Step 3: Build** — `./gradlew :smarttubetv:assembleStglassDebug :smarttubetv:assembleStstableDebug` → BUILD SUCCESSFUL.
- [ ] **Step 4: Vérifier stglass** — `aapt2 dump resources <apk stglass>` contient `style/App.Theme.Glass.Noir.Browse`, `…Rose.Player`, `…Rose.Preferences` et `string/color_scheme_glass_noir`.
- [ ] **Step 5: Vérifier ststable (Review Focus 2)** — `aapt2 dump resources <apk ststable> | grep -c "Glass\|color_scheme_glass"` → `0`.
- [ ] **Step 6: Vérifier l'empreinte upstream** — `git diff 32.56s --stat -- . ':!glass' ':!docs' ':!smarttubetv/src/stglass' ':!.github'` → seulement `.gitignore`, `smarttubetv/build.gradle`, `MainUIData.java`.
- [ ] **Step 7: Commit** — `feat(glass): Glass Noir and Glass Rose color schemes`.

### Task 2: Flou et voile (algorithme pur)

**Files:**
- Create: `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassBlur.java`
- Test: `smarttubetv/src/testStglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassBlurTest.java`

**Interfaces:**
- Produces : `public final class GlassBlur` avec
  - `static int[] boxBlur(int[] argb, int width, int height, int radius, int passes)` — flou boîte séparable (horizontal puis vertical) répété `passes` fois, bords répliqués (pas d'assombrissement), canaux R/G/B/A, nouveau tableau ; `radius == 0` ou `passes == 0` → copie identique ; `IllegalArgumentException` si `argb.length != width * height` ou si `width`/`height` ≤ 0.
  - `static int[] applyScrim(int[] argb, int scrimArgb)` — compose le voile par-dessus chaque pixel : canal = `round((src * (255 - a) + scrim * a) / 255)` pour R, G, B, alpha résultat 255.

- [ ] **Step 1: Écrire les tests qui échouent** (`GlassBlurTest`) :
  - `uniformImageStaysUniform` : 8×4 rempli de `0xFF336699`, `boxBlur(…, 2, 3)` → tous les pixels `0xFF336699` (couvre les bords).
  - `zeroRadiusIsIdentity` : image quelconque 3×3, `radius 0` → `assertArrayEquals` avec l'entrée, et le résultat n'est pas la même instance.
  - `singleBrightPixelSpreadsSymmetrically` : 5×5 noir opaque avec centre blanc, `radius 1, passes 1` → pixels (1,2), (3,2), (2,1), (2,3) égaux entre eux, plus sombres que le centre et plus clairs que (0,0).
  - `rejectsMismatchedSize` : `boxBlur(new int[5], 2, 2, 1, 1)` → `IllegalArgumentException`.
  - `scrimBlackOverWhite` : `applyScrim({0xFFFFFFFF}, 0xB3000000)` → `{0xFF4C4C4C}` (255 × 76 / 255 = 76 = 0x4C).
  - `scrimRoseOverBlack` : `applyScrim({0xFF000000}, 0xA62A0612)` → R = round(42 × 166 / 255) = 27, G = round(6 × 166 / 255) = 4, B = round(18 × 166 / 255) = 12 → `0xFF1B040C`.
- [ ] **Step 2: Lancer** `./gradlew :smarttubetv:testStglassDebugUnitTest --tests '*GlassBlurTest'` → FAIL (classe absente).
- [ ] **Step 3: Implémenter `GlassBlur`.**
- [ ] **Step 4: Lancer** la même commande → 6 tests PASS.
- [ ] **Step 5: Commit** — `feat(glass): pure Java box blur and scrim`.

### Task 3: Fond d'ambiance qui suit la carte sélectionnée

**Files:**
- Create: `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassAmbient.java` — logique testable sans Android.
- Create: `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassAmbientController.java` — branchement Android, un par activité.
- Create: `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassInitProvider.java` — `ContentProvider` dont `onCreate()` enregistre un `Application.ActivityLifecycleCallbacks` ; toutes les autres méthodes renvoient `null` / `0`.
- Create: `smarttubetv/src/stglass/AndroidManifest.xml` — `<provider android:name="com.liskovsoft.smartyoutubetv2.glass.GlassInitProvider" android:authorities="${applicationId}.glass-init" android:exported="false" />`.
- Test: `smarttubetv/src/testStglass/java/com/liskovsoft/smartyoutubetv2/glass/GlassAmbientTest.java`

**Interfaces:**
- Consumes : `GlassBlur.boxBlur`, `GlassBlur.applyScrim` (Task 2) ; `R.attr.glassScrim` (Task 1).
- Produces :
  - `GlassAmbient` : constantes `SAMPLE_WIDTH = 96`, `SAMPLE_HEIGHT = 54`, `BLUR_RADIUS = 4`, `BLUR_PASSES = 3`, `DEBOUNCE_MS = 300` ; `static boolean isUsableSize(int width, int height)` (vrai si les deux > 0) ; `static int[] render(int[] samplePixels, int scrimArgb)` = `applyScrim(boxBlur(samplePixels, 96, 54, 4, 3), scrim)` ; classe imbriquée `Debouncer` avec `interface Clock { long now(); }` (pas de `java.util.function` : minSdk 17 sans desugaring) et `Debouncer(Clock clock, long delayMs)`, `void request(Object key)`, `Object poll()` qui renvoie la dernière clé demandée si `delayMs` s'est écoulé depuis la dernière `request`, sinon `null` (et ne la renvoie qu'une fois).
  - `GlassAmbientController(Activity activity)` : `attach()` / `detach()` ; ne fait rien si le thème de l'activité ne définit pas `glassScrim` (`Theme.resolveAttribute` faux) ou si l'activité est une `PlaybackActivity`.

- [ ] **Step 1: Écrire les tests qui échouent** (`GlassAmbientTest`) :
  - `zeroSizeIsUnusable` : `isUsableSize(0, 54)`, `isUsableSize(96, 0)` faux ; `isUsableSize(96, 54)` vrai.
  - `renderAppliesBlurThenScrim` : 96×54 uniforme `0xFFFFFFFF`, scrim `0xB3000000` → tous les pixels `0xFF4C4C4C`.
  - `debouncerWaitsForQuiet` : horloge factice ; `request("a")` à t=0, `request("b")` à t=200, `poll()` à t=450 → `null` ; à t=500 → `"b"` ; `poll()` à t=600 → `null`.
  - `debouncerSingleRequest` : `request("x")` à t=0, `poll()` à t=300 → `"x"`.
- [ ] **Step 2: Lancer** `./gradlew :smarttubetv:testStglassDebugUnitTest --tests '*GlassAmbientTest'` → FAIL.
- [ ] **Step 3: Implémenter `GlassAmbient`.**
- [ ] **Step 4: Lancer** → 4 tests PASS.
- [ ] **Step 5: Implémenter `GlassAmbientController`** :
  - `attach()` ajoute un `ViewTreeObserver.OnGlobalFocusChangeListener` sur la décor view. Quand la vue focalisée est une `ImageCardView` (ou a un parent `ImageCardView`), `Debouncer.request(card)` et `postDelayed` d'un contrôle à `DEBOUNCE_MS`.
  - Au contrôle, si `poll()` renvoie la carte et que son `getMainImageView().getDrawable()` a une taille utilisable : dessiner ce drawable dans un `Bitmap` ARGB 96×54 (sauvegarder puis restaurer ses `bounds`), `getPixels`, `GlassAmbient.render`, `setPixels` dans un nouveau bitmap, `BitmapDrawable` avec `setFilterBitmap(true)`, puis `BackgroundManager.getInstance(activity).setDrawable(d)`.
  - Toute exception est attrapée et loguée sous le tag `Glass` (spec §7) ; le fond reste alors inchangé.
  - `detach()` retire le listener et les callbacks.
- [ ] **Step 6: Implémenter `GlassInitProvider`** — callbacks : `onActivityResumed` → `attach()` d'un contrôleur mémorisé par activité (`WeakHashMap`) pour toute `LeanbackActivity` ; `onActivityPaused` → `detach()`.
- [ ] **Step 7: Build + tests** — `./gradlew :smarttubetv:testStglassDebugUnitTest :smarttubetv:assembleStglassDebug` → BUILD SUCCESSFUL, 10 tests PASS ; `aapt2 dump xmltree --file AndroidManifest.xml` contient `org.smarttube.glass.glass-init`.
- [ ] **Step 8: Commit** — `feat(glass): ambient background follows the focused card`.

### Task 4: Publication et vérification sur la box

- [ ] **Step 1: Push** (par l'utilisateur ou avec son accord) → le workflow publie `32.56-glass.2` (244602) ; vérifier le JSON `latest`.
- [ ] **Step 2: Mise à jour sur la box** via la mise à jour intégrée.
- [ ] **Step 3: Check-list** (spec §8 + Review Focus) :
  - Réglages → Interface → thème : « Glass Noir » sélectionné après la mise à jour (Focus 1), « Glass Rose » disponible ;
  - le fond suit la vidéo sélectionnée après un court arrêt, sans à-coups pendant un défilement rapide (Focus 4) ;
  - Glass Rose : voile bordeaux et accent corail ;
  - thème Dark Grey : aucun fond d'ambiance (Focus 5) ;
  - lecture 4K 10 min fluide, barre de progression à la couleur accent.
- [ ] **Step 4: Consigner** le résultat dans le ledger.
