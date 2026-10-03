# SmartTube Glass

Fork de [SmartTube](https://github.com/yuliskov/SmartTube) avec une interface « verre dépoli ».
Il s'installe à côté de SmartTube officiel (`org.smarttube.glass`) et se met à jour tout seul.

- Spec : [`docs/superpowers/specs/2026-10-03-smarttube-glass-design.md`](../docs/superpowers/specs/2026-10-03-smarttube-glass-design.md)
- Maquette : [`glass/mockup/index.html`](mockup/index.html)

## Principe

Tout le redesign vit dans des emplacements propres au fork :

| Emplacement | Contenu |
|---|---|
| `glass/glass.gradle` | déclaration de la variante `stglass` (id, version, fallbacks) |
| `smarttubetv/src/stglass/` | ressources et code Java du fork |
| `glass/scripts/`, `glass/tests/` | scripts de la CI et leurs tests |
| `.github/workflows/build-release.yml`, `sync-upstream.yml` | chaîne automatique |

Fichiers de SmartTube modifiés (à garder minimes) :

- `smarttubetv/build.gradle` : une ligne en fin de fichier, `apply from: rootProject.file('glass/glass.gradle')`.
- `.gitignore` : 4 lignes en fin de fichier pour ne jamais commiter la clé de signature.

Les sous-modules `SharedModules` et `MediaServiceCore` ne sont jamais modifiés.

## Versions

`versionCode = versionCode SmartTube × 100 + révision`, `versionName = <version SmartTube>-glass.<révision>`.
Exemple : SmartTube 32.56 (2446) → `32.56-glass.0` (244600), puis `32.56-glass.1` (244601)…
La CI calcule la révision en comptant les tags `v32.56-glass.*` déjà publiés. Elle repart à 0 à chaque
nouvelle version SmartTube. Au-delà de 99 révisions sur une même version, la CI refuse de publier.

## Build local

Il faut un **JDK 17** (Gradle 7.5 ne supporte pas les JDK plus récents, dont celui d'Android Studio).

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17<version>-hotspot"
./gradlew :smarttubetv:assembleStglassDebug
./gradlew -q :smarttubetv:printGlassVersion
bash glass/tests/test-scripts.sh
bash glass/tests/test-version.sh
```

L'APK debug est dans `smarttubetv/build/outputs/apk/stglass/debug/`.

## Signature

Toutes les versions doivent être signées avec **la même clé**, sinon Android refuse la mise à jour.
Garde une copie du fichier `.jks` et des mots de passe hors de GitHub (gestionnaire de mots de passe).

1. Générer la clé, une seule fois, hors du repo :
   ```bash
   keytool -genkeypair -v -keystore smarttube-glass.jks -alias glass -keyalg RSA -keysize 4096 -validity 36500
   ```
2. Encoder la clé : `base64 -w0 smarttube-glass.jks > smarttube-glass.jks.b64`
3. Dans GitHub → *Settings* → *Secrets and variables* → *Actions*, créer :
   `GLASS_KEYSTORE_BASE64` (contenu du `.b64`), `GLASS_KEYSTORE_PASSWORD`, `GLASS_KEY_ALIAS` (`glass`),
   `GLASS_KEY_PASSWORD`. Puis supprimer le `.b64`.

Pour signer en local, créer `keystore.properties` à la racine (ignoré par git) :
`storeFile=`, `storePassword=`, `keyAlias=`, `keyPassword=`.

## Mise en ligne (une fois)

1. Forker `yuliskov/SmartTube` en `florentcallon/SmartTube-Glass`.
2. Onglet *Actions* : activer les workflows.
3. **Désactiver les workflows hérités de SmartTube** : *Actions* → choisir le workflow → *⋯* → *Disable workflow*,
   pour `Build Debug APK` (CI.yml), `cleanup`, `stale` et `virustotal_scan`. Sinon `stale` fermerait les
   issues ouvertes par la synchro et `cleanup` effacerait l'historique des exécutions. On ne les supprime pas
   du repo, pour ne pas créer de conflits avec SmartTube.
4. Ajouter les 4 secrets de signature.
5. Pousser la branche : `git push -u origin glass`, puis *Settings* → *General* → *Default branch* = `glass`.

## Synchronisation avec SmartTube

`sync-upstream` tourne chaque jour à 04:17 UTC (et à la demande depuis l'onglet *Actions*) :

1. Il cherche la dernière version **stable** de SmartTube (tag `X.YYs`) ; les betas sont ignorées.
2. Il la fusionne dans `glass`, puis lance `build-release`, qui compile, signe et publie.
3. **Conflit** : rien n'est publié, une issue « Sync upstream … : conflit » liste les fichiers.
4. **Surcharge masquant un changement** : une issue « Surcharges à revoir » liste les fichiers SmartTube
   modifiés que le fork remplace. La publication continue.

Résoudre un conflit à la main :

```bash
git fetch upstream --tags
git merge 32.60s            # le tag indiqué dans l'issue
# corriger les fichiers en conflit, puis
git add -A && git commit
git push origin glass refs/tags/32.60s
```

## Surcharges de fichiers SmartTube

Quand le fork remplace un fichier de SmartTube (mise en page, style…) par sa propre version dans
`smarttubetv/src/stglass/res/` :

- **Garder tous les `android:id` du fichier d'origine** : le code Java de SmartTube les utilise.
- Inscrire le fichier d'origine dans `glass/overrides.lock` :
  ```bash
  bash glass/scripts/check-overrides.sh --add smarttubetv/src/main/res/layout/icon_header_item.xml
  ```
- Après une issue « Surcharges à revoir » : comparer l'ancienne et la nouvelle version du fichier
  d'origine, reporter le changement dans la surcharge, retirer la ligne de `overrides.lock` et la réinscrire.

## Vérification sur la box (Xiaomi TV Box S 3e gén.)

Activer le débogage réseau sur la box (Paramètres → Système → À propos → appuyer 7 fois sur la version,
puis Options pour les développeurs → Débogage USB / réseau).

```bash
adb connect <ip-de-la-box>
adb install -r smarttube_glass.apk
```

Après chaque lot :

- [ ] Navigation complète à la télécommande : menu, rangées, lecteur, réglages, retour.
- [ ] Le focus est visible partout.
- [ ] Le menu et les rangées défilent sans saccade.
- [ ] Lecture 4K pendant 10 minutes sans saccade ni image perdue.
- [ ] Réglages → À propos → mise à jour : la version N propose N+1, l'installe, et les réglages sont conservés.
