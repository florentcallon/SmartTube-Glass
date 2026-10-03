# SmartTube Glass — Lot 0 + Lot 1 (maquette & socle) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Valider le rendu glass sur une maquette HTML, puis livrer une variante `stglass` installable qui se compile, se publie et se met à jour toute seule depuis `florentcallon/SmartTube-Glass`.

**Architecture:** Le flavor `stglass` est déclaré dans `glass/glass.gradle`, appliqué par **une seule ligne** ajoutée à `smarttubetv/build.gradle`. La logique CI (choix du tag stable, révision, JSON de mise à jour, contrôle de dérive) vit dans de petits scripts bash testés, appelés par deux workflows GitHub Actions (`sync-upstream` appelle `build-release` en `workflow_call`, car un push fait avec `GITHUB_TOKEN` ne déclenche pas d'autre workflow).

**Tech Stack:** Gradle 7.5 / AGP 7.4.2, JDK 17, bash + coreutils, node (validation JSON dans les tests), GitHub Actions, GitHub Releases.

**Spec:** `docs/superpowers/specs/2026-10-03-smarttube-glass-design.md` (sections 1–4, 9 lots 0–1, 10).

Les lots 2–6 (système glass, écrans) feront l'objet de plans séparés, rédigés après validation de la maquette (Task 1), car elle fixe les détails visuels.

## Global Constraints

- `applicationId` = `org.smarttube.glass` ; nom affiché `SmartTube Glass`.
- `versionCode = upstreamVersionCode * 100 + glassRevision`, `glassRevision` ∈ [0, 99] ; `versionName = <upstreamVersionName>-glass.<rev>` (32.56 / 2446 → `244600`, `32.56-glass.0`).
- **Révision automatique (précision de la spec §3)** : pas de `glass/glass.properties`. La CI calcule `glassRevision` = nombre de tags `v<upstreamVersionName>-glass.*` déjà existants ; le build local vaut 0. Le tag de release est `v<versionName>`.
- On suit uniquement les tags upstream `^[0-9]+\.[0-9]+s$`.
- Fichiers upstream modifiés dans ce plan : **uniquement** `smarttubetv/build.gradle` (1 ligne en fin de fichier).
- Sous-modules jamais modifiés.
- Modules à flavors (`common`, `leanbackassistant`, `youtubeapi`, `appupdatechecker2`) : résolus via `matchingFallbacks = ['ststable']`.
- URL de mise à jour : `https://github.com/florentcallon/SmartTube-Glass/releases/download/latest/smarttube_glass.json`.
- Assets de release : `smarttube_glass.apk` (universel), `smarttube_glass_arm64-v8a.apk`, `smarttube_glass_armeabi-v7a.apk`, `smarttube_glass.json`.
- Secrets : `GLASS_KEYSTORE_BASE64`, `GLASS_KEYSTORE_PASSWORD`, `GLASS_KEY_ALIAS`, `GLASS_KEY_PASSWORD`.
- Toute action vers GitHub (push, création de repo, secrets) est faite par l'utilisateur ou après son accord explicite.

## Review Focus

1. **Plus de 99 révisions sur une même version upstream** → la CI doit échouer proprement (pas de versionCode qui déborde sur la version upstream suivante). Test : `next-revision` avec 100 tags existants → exit 1.
2. **Re-run du build sur un commit déjà publié** (push doc, relance manuelle) → aucune nouvelle release, aucune révision consommée. Test : `build-release` vérifie qu'aucun tag `v*-glass.*` ne pointe déjà sur `HEAD` (Task 4, step de garde).
3. **Liste de tags upstream contenant des tags non numériques** (`beta`, `latest`, `notification2`) ou des versions à 3 chiffres (`32.100s`) → sélection correcte par tri de version, pas lexical. Test dans Task 3.
4. **Fichier surchargé supprimé ou renommé upstream** → `check-overrides` le signale comme changé (pas de crash du script). Test dans Task 3.
5. **Lint `abortOnError true` en release** sur le nouveau flavor → le build release doit passer localement avant la CI (Task 2, step de vérification `assembleStglassRelease`).

---

### Task 1: Maquette HTML interactive (Lot 0)

**Files:**
- Create: `glass/mockup/index.html` (page unique, CSS/JS inline, publiée en Artifact)

**Interfaces:**
- Produces : les valeurs visuelles validées (couleurs §5.3, rayons §5.4) que les plans des lots 2–6 reprendront ; tout écart demandé par l'utilisateur est reporté dans la spec.

- [ ] **Step 1: Construire la maquette** — écran 1920×1080 mis à l'échelle de la fenêtre. Sélecteurs : thème (Glass Noir / Glass Rose), écran (Navigation+grille / Lecteur / Dialogue / Réglages). Navigation au clavier (flèches, Entrée, Échap) qui simule la télécommande et montre les états de focus (§5.4). Fond d'ambiance = image floutée de la carte sélectionnée (dégradés générés, aucune image tierce). Valeurs exactes des tableaux §5.3 et rayons §5.4. Police Figtree via Google Fonts.
- [ ] **Step 2: Vérifier** dans le navigateur intégré : les 4 écrans × 2 thèmes s'affichent, le focus se déplace aux flèches, aucune erreur console.
- [ ] **Step 3: Publier en Artifact** et faire valider par l'utilisateur. **Porte : ne pas commencer Task 2 sans retour explicite** (les retours visuels sont reportés dans la spec, commit `docs:`).
- [ ] **Step 4: Commit**

```bash
git add glass/mockup/index.html docs/superpowers/specs
git commit -m "docs(glass): interactive mockup of glass screens"
```

### Task 2: Flavor `stglass` et calcul de version

**Files:**
- Create: `glass/glass.gradle`
- Modify: `smarttubetv/build.gradle` (ajout en dernière ligne : `apply from: rootProject.file('glass/glass.gradle')`)
- Create: `smarttubetv/src/stglass/res/values/strings.xml` (`app_name` = `SmartTube Glass`, `translatable="false"`, CDATA comme upstream)
- Create: `smarttubetv/src/stglass/res/values/update_urls.xml` (`string-array name="update_urls"`, une seule URL, cf. Global Constraints)
- Create: `smarttubetv/src/stglass/res/mipmap-nodpi/` : copies de `app_banner.png`, `app_icon.png`, `app_logo.png`, `app_logo_semi_grey.png`, `app_logo_semi_red.png` depuis `src/ststable/res/mipmap-nodpi/`, et `mipmap-nodpi-v30/app_icon.png` (les icônes glass dédiées viendront au lot 2)
- Test: `glass/tests/test-version.sh`

**Interfaces:**
- Produces : tâche Gradle `:smarttubetv:printGlassVersion` qui imprime exactement une ligne `upstreamVersionName=<n> versionCode=<c> versionName=<v>` ; propriété Gradle `-PglassRevision=<int>` (défaut 0) ; APK de sortie `smarttubetv/build/outputs/apk/stglass/release/SmartTube_glass_<versionName>_<abi|universal>.apk` (nommage upstream inchangé).

- [ ] **Step 0: Prérequis local** — JDK 17 (celui d'Android Studio est un JDK 25, incompatible avec Gradle 7.5). Demander l'accord de l'utilisateur, puis `winget install EclipseAdoptium.Temurin.17.JDK`. Exporter `JAVA_HOME` vers ce JDK pour les commandes Gradle. Les composants SDK manquants (platform 34, build-tools 30.0.3) sont téléchargés par Gradle (licences déjà acceptées : `Sdk/licenses` présent).

- [ ] **Step 1: Écrire le test qui échoue** — `glass/tests/test-version.sh` :

```bash
out=$(./gradlew -q :smarttubetv:printGlassVersion)
[ "$out" = "upstreamVersionName=32.56 versionCode=244600 versionName=32.56-glass.0" ] || fail "rev0: $out"
out=$(./gradlew -q :smarttubetv:printGlassVersion -PglassRevision=7)
[ "$out" = "upstreamVersionName=32.56 versionCode=244607 versionName=32.56-glass.7" ] || fail "rev7: $out"
./gradlew -q :smarttubetv:printGlassVersion -PglassRevision=100 && fail "rev100 must fail"
```

(Les valeurs 32.56/2446 sont celles du tag de base ; le test lit `defaultConfig` pour rester juste après une sync : en tête du script, extraire `versionCode`/`versionName` de `smarttubetv/build.gradle` par `grep` et construire les attendus à partir d'eux.)

- [ ] **Step 2: Lancer** `bash glass/tests/test-version.sh` → FAIL (`Task 'printGlassVersion' not found`).

- [ ] **Step 3: Implémenter `glass/glass.gradle`** — dans `android.productFlavors`, un flavor `stglass` avec `dimension "default"`, `applicationId "org.smarttube.glass"`, `targetSdkVersion project.properties.compileSdkVersion` (comme ststable), `matchingFallbacks = ['ststable']`, `versionCode = android.defaultConfig.versionCode * 100 + rev`, `versionNameSuffix "-glass.${rev}"`. `rev` = `project.findProperty('glassRevision') ?: 0` converti en int ; `rev` hors [0,99] → `throw new GradleException("glassRevision must be in [0, 99]")`. Tâche `printGlassVersion` qui imprime la ligne de l'interface (`println`, rien d'autre en sortie `-q`).

- [ ] **Step 4: Ajouter la ligne `apply from`** en fin de `smarttubetv/build.gradle` et les ressources du flavor listées dans *Files*.

- [ ] **Step 5: Lancer** `bash glass/tests/test-version.sh` → PASS.

- [ ] **Step 6: Vérifier le build** — `./gradlew :smarttubetv:assembleStglassDebug :smarttubetv:assembleStglassRelease` → BUILD SUCCESSFUL (release non signée en local sans `keystore.properties`, c'est attendu). Puis `"$ANDROID_HOME/build-tools/36.0.0/aapt2" dump badging <apk universel debug> | head -3` → `package: name='org.smarttube.glass' versionCode='244600' versionName='32.56-glass.0'` et `application-label:'SmartTube Glass'`.

- [ ] **Step 7: Vérifier l'URL de mise à jour** — `aapt2 dump resources <apk> | grep -A2 update_urls` → contient l'URL `florentcallon/SmartTube-Glass`.

- [ ] **Step 8: Vérifier la contrainte « 1 ligne upstream »** — `git diff 32.56s --stat -- . ':!glass' ':!docs' ':!smarttubetv/src/stglass' ':!.github'` → seulement `smarttubetv/build.gradle | 1 +`.

- [ ] **Step 9: Commit**

```bash
git add glass/glass.gradle glass/tests/test-version.sh smarttubetv/build.gradle smarttubetv/src/stglass
git commit -m "feat(glass): add stglass flavor with derived version code"
```

### Task 3: Scripts CI testés

**Files:**
- Create: `glass/scripts/latest-stable-tag.sh`, `glass/scripts/next-revision.sh`, `glass/scripts/make-update-json.sh`, `glass/scripts/check-overrides.sh`
- Create: `glass/overrides.lock` (vide + commentaire d'en-tête `# sha1  upstream-path` ; les lignes `#` sont ignorées)
- Test: `glass/tests/test-scripts.sh` (helpers `fail`, `assert_eq` ; un compteur ; exit ≠ 0 si un échec)

**Interfaces:**
- `latest-stable-tag.sh` : lit des noms de tags sur stdin, imprime le plus grand qui matche `^[0-9]+\.[0-9]+s$` (tri `sort -V`) ; aucun → rien sur stdout, exit 1.
- `next-revision.sh <upstreamVersionName>` : lit des noms de tags sur stdin, compte ceux qui matchent exactement `^v<upstreamVersionName>-glass\.[0-9]+$`, imprime ce nombre ; si ≥ 100 → message sur stderr, exit 1.
- `make-update-json.sh <versionName> <versionCode> <repo> [changelog...]` : imprime le JSON de la spec §4.2 ; base d'URL `https://github.com/<repo>/releases/download/latest/` (release au tag fixe `latest`) ; clés `package.downloadUrl`, `package.downloadUrlList_arm64-v8a`, `package.downloadUrlList_armeabi-v7a`, et `<versionName>.versionCode` (nombre), `<versionName>.changelog` (tableau, vide si aucun argument). Échappe `"` et `\` dans le changelog.
- `check-overrides.sh [lockfile]` (défaut `glass/overrides.lock`) : pour chaque ligne `<sha1>  <path>`, compare à `sha1sum <path>` dans l'arbre de travail ; imprime chaque chemin changé ou absent ; exit 0 si aucun, 2 si au moins un, 1 si le lockfile est illisible.

- [ ] **Step 1: Écrire les tests qui échouent** dans `glass/tests/test-scripts.sh` :
  - `latest_stable_picks_highest` : entrée `32.10\n32.56s\n32.47s\n32.59\nbeta\nlatest\nnotification2\n32.100s` → `32.100s`.
  - `latest_stable_none` : entrée `32.59\nbeta` → sortie vide, exit 1.
  - `next_revision_counts_exact_prefix` : `next-revision.sh 32.56` avec `v32.56-glass.0\nv32.56-glass.1\nv32.5-glass.0\nv32.56-glass.x\nv132.56-glass.0` → `2`.
  - `next_revision_overflow` : 100 lignes `v32.56-glass.0..99` → exit 1.
  - `update_json_shape` : `make-update-json.sh 32.56-glass.0 244600 florentcallon/SmartTube-Glass "Upstream 32.56s" 'Say "hi"'` → JSON valide (`node -e "JSON.parse(require('fs').readFileSync(0,'utf8'))"`), et via node : `j['32.56-glass.0'].versionCode === 244600`, `j['32.56-glass.0'].changelog[1] === 'Say "hi"'`, `j.package['downloadUrlList_arm64-v8a'][0] === 'https://github.com/florentcallon/SmartTube-Glass/releases/download/latest/smarttube_glass_arm64-v8a.apk'`, `j.package.downloadUrl` se termine par `/smarttube_glass.apk`.
  - `update_json_no_changelog` : sans changelog → `changelog` est `[]`.
  - `overrides_clean` / `overrides_changed` / `overrides_missing` : dans un `mktemp -d`, fichier `a.xml`, lockfile généré par `sha1sum a.xml` → exit 0 ; après modification de `a.xml` → exit 2 et sortie `a.xml` ; après `rm a.xml` → exit 2 et sortie `a.xml`.
  - `overrides_comments_ignored` : lockfile avec seulement `# sha1  upstream-path` → exit 0.

- [ ] **Step 2: Lancer** `bash glass/tests/test-scripts.sh` → FAIL (scripts absents).
- [ ] **Step 3: Implémenter les 4 scripts** (`#!/usr/bin/env bash`, `set -euo pipefail`, aucun outil hors coreutils/grep/sed/sort), `git update-index --chmod=+x` sur chacun.
- [ ] **Step 4: Lancer** `bash glass/tests/test-scripts.sh` → tous PASS.
- [ ] **Step 5: Commit**

```bash
git add glass/scripts glass/tests/test-scripts.sh glass/overrides.lock
git commit -m "feat(glass): tested CI helper scripts"
```

### Task 4: Workflow `build-release`

**Files:**
- Create: `.github/workflows/build-release.yml`

**Interfaces:**
- Consumes : scripts de Task 3, `printGlassVersion` et `-PglassRevision` de Task 2.
- Produces : workflow déclenché par `push` sur `glass`, `workflow_dispatch`, et `workflow_call` (utilisé par Task 5) ; en sortie une release `v<versionName>` et la release `latest` mises à jour.

- [ ] **Step 1: Écrire le workflow** — job unique `ubuntu-latest`, `permissions: contents: write, issues: write`, `concurrency: glass-release` (pas d'annulation). Étapes :
  1. `actions/checkout@v4` avec `fetch-depth: 0`, `submodules: recursive`, puis `git fetch --tags --force`.
  2. **Garde** : si `git tag --points-at HEAD | grep -q -- '-glass\.'` → sortie succès sans build (Review Focus 2).
  3. `actions/setup-java@v4` (temurin 17, cache gradle).
  4. `UP=$(./gradlew -q :smarttubetv:printGlassVersion | sed -E 's/.*upstreamVersionName=([^ ]+).*/\1/')` ; `REV=$(git tag -l | glass/scripts/next-revision.sh "$UP")`.
  5. Décoder `GLASS_KEYSTORE_BASE64` vers `$RUNNER_TEMP/glass.jks` et écrire `keystore.properties` à la racine (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) ; secrets absents → échec explicite « signing secrets missing ».
  6. `bash glass/tests/test-scripts.sh`, puis `./gradlew -PglassRevision=$REV :smarttubetv:testStglassDebugUnitTest :smarttubetv:assembleStglassRelease`.
  7. Lire `versionCode`/`versionName` via `printGlassVersion -PglassRevision=$REV` ; copier les APK de `smarttubetv/build/outputs/apk/stglass/release/` vers les noms d'assets des Global Constraints ; générer `smarttube_glass.json` (changelog : `Upstream <dernier tag stable ancêtre de HEAD>` + sujets des commits non-merge depuis le tag `v*-glass.*` précédent, 10 max).
  8. `gh release create "v$VERSION_NAME" <apks> smarttube_glass.json --target "$GITHUB_SHA" --notes-file <changelog>` puis mise à jour de `latest` : `gh release view latest || gh release create latest --title "Latest" --notes "Dernière version de SmartTube Glass"` puis `gh release upload latest <assets> --clobber`.
  9. `if: failure()` → `gh issue create --title "Build/release échoué : <sha court>" --body "<lien du run>"`.
- [ ] **Step 2: Valider la syntaxe** — `npx --yes @action-validator/cli .github/workflows/build-release.yml` → aucune erreur.
- [ ] **Step 3: Commit**

```bash
git add .github/workflows/build-release.yml
git commit -m "ci(glass): build, sign and publish stglass releases"
```

### Task 5: Workflow `sync-upstream`

**Files:**
- Create: `.github/workflows/sync-upstream.yml`

**Interfaces:**
- Consumes : `latest-stable-tag.sh`, `check-overrides.sh` (Task 3) ; `build-release.yml` en `workflow_call` (Task 4).

- [ ] **Step 1: Écrire le workflow** — déclencheurs `schedule: cron '17 4 * * *'` et `workflow_dispatch` ; `permissions: contents: write, issues: write` ; job `sync` (sortie `merged: true|false`) :
  1. Checkout `glass` (`fetch-depth: 0`, `submodules: recursive`), identité git `github-actions[bot]`.
  2. `git remote add upstream https://github.com/yuliskov/SmartTube.git && git fetch upstream --tags --force`.
  3. `TAG=$(git tag -l | glass/scripts/latest-stable-tag.sh)` ; `git merge-base --is-ancestor "$TAG" HEAD` → `merged=false`, fin.
  4. `git merge --no-edit "$TAG"` ; en cas d'échec : liste `git diff --name-only --diff-filter=U`, `git merge --abort`, issue « Sync upstream $TAG : conflit » avec la liste, job en échec.
  5. `git submodule update --init --recursive` ; `glass/scripts/check-overrides.sh` → exit 2 : issue « Surcharges à revoir après $TAG » avec la liste (non bloquant) ; exit 1 : échec.
  6. `git push origin glass`, `merged=true`.
  - Job `release` : `needs: sync`, `if: needs.sync.outputs.merged == 'true'`, `uses: ./.github/workflows/build-release.yml`, `secrets: inherit`.
- [ ] **Step 2: Valider la syntaxe** — `npx --yes @action-validator/cli .github/workflows/sync-upstream.yml` → aucune erreur.
- [ ] **Step 3: Simuler la logique de merge en local** — `git fetch upstream --tags` puis `git tag -l | glass/scripts/latest-stable-tag.sh` → `32.56s` aujourd'hui, et `git merge-base --is-ancestor 32.56s HEAD` → exit 0 (rien à faire).
- [ ] **Step 4: Commit**

```bash
git add .github/workflows/sync-upstream.yml
git commit -m "ci(glass): daily sync with upstream stable tags"
```

### Task 6: Documentation, mise en ligne et première release sur la box

**Files:**
- Create: `glass/README.md` — sections : *Principe* (couche isolée, fichiers upstream touchés), *Build local* (JDK 17, `assembleStglassDebug`), *Signature* (génération du keystore, 4 secrets, sauvegarde hors GitHub), *Synchronisation* (fonctionnement, résoudre un conflit à la main : `git fetch upstream --tags && git merge <tag>`, puis push), *Surcharges* (règle de conservation des `android:id`, mise à jour de `overrides.lock` avec `sha1sum <upstream-path> >> glass/overrides.lock`), *Check-list de vérification sur la box* (spec §8).

- [ ] **Step 1: Écrire `glass/README.md`** et commit `docs(glass): fork README`.
- [ ] **Step 2: Clé de signature (utilisateur, guidé)** — `keytool -genkeypair -v -keystore smarttube-glass.jks -alias glass -keyalg RSA -keysize 4096 -validity 36500` exécuté **par l'utilisateur** hors du repo (le `.jks` ne doit jamais être commité ; vérifier que `*.jks` et `keystore.properties` sont ignorés par `.gitignore`, sinon les ajouter). L'utilisateur encode en base64 et saisit lui-même les 4 secrets dans GitHub → Settings → Secrets → Actions.
- [ ] **Step 3: Repo GitHub (utilisateur)** — créer le fork `florentcallon/SmartTube-Glass` de `yuliskov/SmartTube`, activer Actions (onglet Actions → « I understand… »), et les workflows planifiés.
- [ ] **Step 4: Push (avec accord explicite de l'utilisateur)** — `git push -u origin glass`, puis l'utilisateur définit `glass` comme branche par défaut. Le push déclenche `build-release`.
- [ ] **Step 5: Vérifier la release** — page Releases : `v32.56-glass.0` et `latest` contiennent les 4 assets ; `curl -sL https://github.com/florentcallon/SmartTube-Glass/releases/download/latest/smarttube_glass.json` → JSON avec `versionCode` 244600.
- [ ] **Step 6: Installer sur la box** — `adb connect <ip-box>` (débogage réseau activé par l'utilisateur), `adb install smarttube_glass.apk` → app « SmartTube Glass » lancée à côté de SmartTube officiel.
- [ ] **Step 7: Vérifier la mise à jour intégrée** — pousser un commit trivial (ex. README) → release `v32.56-glass.1` (244601) ; sur la box, Réglages → À propos → Vérifier les mises à jour → proposition de `32.56-glass.1`, installation OK, réglages conservés.
- [ ] **Step 8: Lancer `sync-upstream` à la main** (onglet Actions) → succès, sortie « rien à fusionner » (`merged=false`), job `release` ignoré.
