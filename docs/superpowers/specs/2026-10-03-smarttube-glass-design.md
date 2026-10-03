# SmartTube Glass — Design

Date : 2026-10-03
Statut : en revue

## 1. Objectif

Fork de [SmartTube](https://github.com/yuliskov/SmartTube) (client YouTube Android TV) avec une interface
« frosted glass » moderne, inspirée de maquettes de menus latéraux en verre dépoli (Airbnb rose, Netflix noir).

Critères de succès :

1. Le fork s'installe sur une Xiaomi TV Box S (3e gén., Google TV / Android 14, 2 Go RAM) à côté de SmartTube officiel.
2. Il se met à jour **tout seul** via le système de mise à jour intégré, en conservant le redesign.
3. Chaque nouvelle version **stable** de SmartTube est intégrée automatiquement, sans intervention humaine
   tant qu'il n'y a pas de conflit.
4. Navigation latérale, grille de vidéos, lecteur et dialogues/réglages adoptent le style glass.
5. La lecture reste fluide (4K, 10 min, pas de saccade) sur la box cible.

Hors périmètre : nouvelles fonctionnalités, modification du comportement, nouvelles données (ex. compteurs
inexistants), modification des sous-modules `SharedModules` / `MediaServiceCore`.

## 2. Principe directeur : couche isolée

Toute modification vit dans des emplacements propres au fork. Les fichiers upstream modifiés sont limités à :

| Fichier upstream | Modification | Taille |
|---|---|---|
| `smarttubetv/build.gradle` | déclaration du flavor `stglass` | ~10 lignes |
| `common/.../prefs/MainUIData.java` | 2 entrées `ColorScheme` (Glass Noir, Glass Rose) ajoutées **en fin** de `initColorSchemes()` | ~10 lignes |

Tout le reste passe par :

- **Surcharge de ressources** : `smarttubetv/src/stglass/res/` (le module app prime sur `main` et sur les
  bibliothèques `common`, `leanback-1.0.0`).
- **Code propre au flavor** : `smarttubetv/src/stglass/java/com/liskovsoft/smartyoutubetv2/glass/`.
- **Point d'entrée sans hook Java upstream** : `GlassInitProvider` (ContentProvider déclaré dans
  `src/stglass/AndroidManifest.xml`, fusionné par le manifest merger). Il enregistre des
  `ActivityLifecycleCallbacks` + `FragmentLifecycleCallbacks` qui « décorent » les vues après leur création.

## 3. Repo, branches, versions

- Repo public `<compte>/SmartTube-Glass`, fork GitHub de `yuliskov/SmartTube`.
- Remotes locaux : `upstream` = yuliskov/SmartTube, `origin` = fork.
- Branche par défaut `glass`, partie du tag stable `32.56s`.
- Sous-modules inchangés (URLs upstream).
- Versions upstream : tags `X.YY` = beta (prerelease), `X.YYs` = stable. **On suit les stables uniquement.**
- Numéro de version du fork : `versionCode = upstreamVersionCode * 100 + glassRevision`
  (`glassRevision` ∈ [0, 99], dans `glass/glass.properties`, remis à 0 à chaque sync upstream).
  `versionName = "<upstreamVersionName>-glass.<rev>"` (ex. upstream 32.56s : versionCode 2446 → 244600, `32.56-glass.0`).
- `applicationId = org.smarttube.glass`.
- Les modules bibliothèques sans flavor `stglass` utilisent `matchingFallbacks = ['ststable']`
  (aucune modification de `common/build.gradle`).
- Pas de Firebase/Crashlytics pour `stglass` (le plugin n'est appliqué que pour stbeta/ststable).

### Arborescence ajoutée

```
glass/
  README.md                 doc du fork (build, signature, sync, résolution de conflits)
  glass.properties          glassRevision
  overrides.lock            fichier upstream remplacé -> SHA-1 de la version copiée
  scripts/
    check-overrides.sh      compare overrides.lock aux fichiers upstream actuels
    make-update-json.sh     génère smarttube_glass.json
smarttubetv/src/stglass/
  AndroidManifest.xml
  java/.../glass/           composants Java du fork
  res/                      surcharges + ressources glass
.github/workflows/
  sync-upstream.yml
  build-release.yml
```

## 4. Chaîne CI (GitHub Actions)

### 4.1 `sync-upstream.yml` — quotidien + manuel

1. Checkout `glass` (historique complet, sous-modules récursifs).
2. Récupère les tags upstream, sélectionne le plus récent correspondant à `^\d+\.\d+s$` (tri par version).
3. Si ce tag est déjà ancêtre de `glass` → fin.
4. `git merge <tag>` (+ `git submodule update --recursive`).
   - Conflit → abort, ouvre une issue « Sync upstream <tag> : conflit » listant les fichiers, fin en échec.
5. Remet `glassRevision` à 0, commit, `glass/scripts/check-overrides.sh` :
   - un fichier upstream surchargé a changé → ouvre une issue « Surcharge à revoir » (non bloquant :
     la surcharge reste valable tant que le build passe, mais elle masque le changement upstream).
6. Push `glass` → déclenche `build-release`.

### 4.2 `build-release.yml` — sur push `glass` + manuel

1. JDK 17 (AGP 7.4.2 / Gradle 7.5), SDK Android, cache Gradle.
2. Reconstitue `keystore.properties` + keystore depuis les secrets
   `GLASS_KEYSTORE_BASE64`, `GLASS_KEYSTORE_PASSWORD`, `GLASS_KEY_ALIAS`, `GLASS_KEY_PASSWORD`.
3. `./gradlew :smarttubetv:testStglassDebugUnitTest :smarttubetv:assembleStglassRelease`.
4. Si `versionCode` déjà publié → fin (évite les doublons sur un push doc).
   Sinon : release GitHub `v<versionName>` + mise à jour de la release `latest` avec :
   - `smarttube_glass_arm64-v8a.apk`, `smarttube_glass_armeabi-v7a.apk`, `smarttube_glass.apk` (universel)
   - `smarttube_glass.json` (format `AppVersionChecker`) :
     ```json
     {
       "package": {
         "downloadUrl": ".../latest/download/smarttube_glass.apk",
         "downloadUrlList_arm64-v8a": [".../smarttube_glass_arm64-v8a.apk"],
         "downloadUrlList_armeabi-v7a": [".../smarttube_glass_armeabi-v7a.apk"]
       },
       "32.56-glass.0": { "versionCode": 244600, "changelog": ["Upstream 32.56s", "..."] }
     }
     ```
5. Échec → issue ouverte, rien n'est publié ; la TV reste sur la dernière version valide.

### 4.3 Mise à jour côté TV

`src/stglass/res/values/update_urls.xml` surcharge `update_urls` →
`https://github.com/<compte>/SmartTube-Glass/releases/download/latest/smarttube_glass.json`.
Les liens upstream vers les APK officiels (`StableRestorePresenter`, bridges) sont laissés tels quels.

## 5. Système visuel glass

### 5.1 Fond d'ambiance

La miniature de l'élément sélectionné, floutée et voilée, sert de fond. On s'appuie sur le
`UriBackgroundManager` existant : `GlassBackgroundController` (installé par les callbacks) remplace le
drawable posé dans le `BackgroundManager` par sa version floutée + voile, avec fondu enchaîné de 300 ms.
Le flou du fond est calculé une fois par image (transformation Glide, image réduite à 1/4), donc gratuit à l'affichage.

### 5.2 Panneaux verre — `GlassPanelView`

Conteneur `FrameLayout` qui dessine : fond flouté (si disponible) → teinte → trait de bord 1 dp
(dégradé vertical, plus clair en haut) → contenu. Arrondi via `ViewOutlineProvider` + `clipToOutline`.

Stratégie de flou (`GlassBlurPolicy`, testable unitairement) :

| Condition | Rendu |
|---|---|
| API ≥ 31, pas `isLowRamDevice`, écran ≠ lecteur | flou temps réel de l'arrière-plan (`RenderEffect`, échantillon ½, rayon 24 px), recalculé uniquement quand le fond change |
| sinon | teinte translucide sur le fond d'ambiance (déjà flouté) |
| lecteur vidéo | toujours teinte (jamais de flou temps réel sur la vidéo) |

Pas de réglage utilisateur dans la v1 (choix automatique). *Écart assumé par rapport à la discussion :
un interrupteur demanderait de modifier les écrans de réglages upstream ; on l'ajoutera si la détection
automatique s'avère insuffisante.*

### 5.3 Thèmes

Styles `App.Theme.Glass.{Noir,Rose}.{Browse,Player,Preferences}` héritant des styles `App.Theme.Leanback.*`
upstream, définis dans `src/stglass/res/values/glass_themes.xml`. Attributs personnalisés
(`glassTint`, `glassStroke`, `glassAccent`, `glassScrim`, `glassTextSecondary`) déclarés dans
`glass_attrs.xml` ; `GlassTheme` les lit avec des valeurs par défaut neutres, ce qui garde les anciens
thèmes (Teal, Dark Grey…) fonctionnels avec un rendu glass neutre.

| Élément | Glass Noir | Glass Rose |
|---|---|---|
| Voile du fond | `#B3000000` | `#A62A0612` |
| Teinte panneau | `#14FFFFFF` | `#1AFF5A7A` |
| Teinte panneau focus | `#29FFFFFF` | `#33FF5A7A` |
| Bord | `#1FFFFFFF` | `#2EFFB3C1` |
| Accent | `#FF0033` | `#FF5A5F` |
| Texte secondaire | `#99FFFFFF` | `#A6FFFFFF` |
| Teinte lecteur | `#8C0A0A0C` | `#8C1A0710` |

Premier lancement : si aucune préférence UI n'existe, `GlassInitProvider` sélectionne Glass Noir
(aucune modification de la valeur par défaut upstream).

### 5.4 Formes, typo, focus

- Rayons : panneaux 24 dp, cartes 16 dp, pills/boutons 14 dp, badges pastille. Grille de 4 dp.
- Police Figtree (OFL) embarquée en `res/font`, ≈150 Ko, repli système.
- Focus : teinte focus, `scale 1.06` (cartes), halo accent (ombre colorée API 28+, sinon trait accent 2 dp),
  barre accent 3 dp à gauche dans le menu. Animations 150–200 ms, `FastOutSlowIn`, sans rebond.

## 6. Écrans

### 6.1 Navigation latérale
Surcharge de `icon_header_item.xml` et des layouts d'en-têtes de `leanback-1.0.0` ; décoration du
conteneur d'en-têtes de `BrowseFragment` par `GlassPanelView` (marge 16 dp, rayon 24 dp).
Contenu : logo + avatar/nom du compte en tête (si connecté), entrées en pills icône+texte, entrée active
teintée accent + barre gauche, carte verre « Réglages » en bas. Repliée : colonne d'icônes verre ;
dépliage animé 180 ms. Badges uniquement si une donnée existe déjà.

### 6.2 Grille des vidéos
Surcharge de `lb_image_card_view*.xml`, `lb_video_card_view.xml`, `channel_card.xml`, `settings_card.xml`,
`text_badge_image_view*.xml`. Miniature arrondie 16 dp, bande d'infos verre en bas, badges en pastilles
verre, progression fine accent. Focus : zoom + halo + fondu du fond d'ambiance vers la miniature.
Titres de rangées blanc semi-gras, espacement accru.

### 6.3 Lecteur
Surcharge de `lb_playback_transport_controls_row.xml`, `lb_control_bar.xml`, `lb_playback_fragment.xml`
(+ drawables de seekbar). Barre de contrôles verre flottante (rayon 24 dp), capsule titre/chaîne en haut
à gauche, barre de progression épaisse arrondie avec curseur accent, aperçu dans un cadre verre, boutons
ronds. Teinte uniquement (cf. 5.2).

### 6.4 Dialogues et réglages
Surcharge des layouts de préférences (`leanback_preference_fragment.xml`,
`leanback_list_preference_fragment.xml`, `dialog_list_preference_item_multi.xml`) et styles `Preferences`.
Panneau verre latéral droit flottant, options en pills, interrupteurs/coches accent.

Règle commune aux surcharges : **conserver tous les `android:id` du layout upstream** (le code Java
upstream les référence) ; chaque surcharge est inscrite dans `glass/overrides.lock`.

## 7. Gestion des erreurs

- Échec de détection/flou → repli teinte, jamais de crash (try/catch + log `Glass`).
- Vue attendue introuvable lors d'une décoration (upstream a changé un layout) → décoration ignorée
  + log ; l'écran reste fonctionnel avec le style d'origine.
- CI : tout échec bloque la publication et ouvre une issue.

## 8. Tests

- Unitaires (Robolectric, déjà utilisé par le projet) : `GlassBlurPolicy`, `GlassTheme` (lecture des
  attributs + valeurs par défaut), calcul de version, `make-update-json.sh` (JSON valide, URLs par ABI).
- CI : build `stglass` + tests unitaires existants + `check-overrides.sh`.
- Manuel sur la box, après chaque lot : check-list dans `glass/README.md` (navigation D-pad complète,
  focus visible partout, fluidité du menu et des rangées, lecture 4K 10 min, mise à jour in-app de N vers N+1).

## 9. Lots de livraison

0. **Maquette HTML interactive** des 4 écrans × 2 thèmes, à valider avant le code Android.
1. **Socle** : flavor, versions, update_urls, CI sync + build + release, signature, première release installable.
2. **Système glass** : thèmes, attributs, `GlassPanelView`, `GlassBlurPolicy`, fond d'ambiance, police, `GlassInitProvider`.
3. Navigation latérale. 4. Grille. 5. Lecteur. 6. Dialogues & réglages.

Chaque lot se termine par une release installée et vérifiée sur la box.

## 10. Prérequis côté utilisateur

- Compte GitHub, fork créé, Actions activées.
- Keystore de signature généré (guidé) et ajouté en secrets ; sauvegarde hors GitHub obligatoire.
- Sur la box : autoriser l'installation depuis la source de SmartTube Glass.
- En local (optionnel, pour itérer vite) : JDK 17 (celui d'Android Studio convient) + `adb` (présent).
