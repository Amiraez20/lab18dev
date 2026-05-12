# ScorePersistanceDemo — TP Android ViewModel & LiveData

> **Objectif :** Comprendre pourquoi les variables sont perdues à la rotation d'écran, maîtriser `ViewModel` + `LiveData`, et appliquer l'architecture MVVM recommandée par Google (Jetpack 2.10.0).

---

## Sommaire

1. [Structure du projet](#structure-du-projet)
2. [Dépendances](#dépendances)
3. [Partie 1 — Sans ViewModel (le problème)](#partie-1--sans-viewmodel-le-problème)
4. [Partie 2 — Avec ViewModel + LiveData (la solution)](#partie-2--avec-viewmodel--livedata-la-solution)
5. [Bonus — Thread background & SavedStateHandle](#bonus--thread-background--savedstatehandle)
6. [Tableau comparatif](#tableau-comparatif)
7. [Concepts clés](#concepts-clés)

---

## Structure du projet

```
ScorePersistanceDemo/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/scorepersistancedemo/
│   │   │   ├── MainActivity.java
│   │   │   └── ScoreViewModel.java
│   │   └── res/layout/
│   │       └── activity_main.xml
│   └── build.gradle.kts
└── README.md
```

---

## Dépendances

Dans `build.gradle.kts` (module app) :

```kotlin
val lifecycleVersion = "2.10.0"

implementation("androidx.lifecycle:lifecycle-viewmodel:$lifecycleVersion")
implementation("androidx.lifecycle:lifecycle-livedata:$lifecycleVersion")
```

> Ces dépendances Jetpack fournissent `ViewModel` et `LiveData`. La version 2.10.0 est la version stable officielle 2026, optimisée pour Android 15/16.

---

## Partie 1 — Sans ViewModel (le problème)

### Ce qui se passe lors d'une rotation

Quand l'écran tourne, Android **détruit** l'Activity puis en **recrée** une nouvelle. Toutes les variables d'instance sont réinitialisées à leur valeur par défaut.

```
Rotation détectée
      ↓
onSaveInstanceState()   ← sauvegarde manuelle (limitée)
      ↓
onDestroy()             ← Activity détruite, variables perdues
      ↓
onCreate()              ← nouvelle Activity créée
      ↓
Restauration manuelle   ← seulement types primitifs (int, String…)
```

### Démonstration vidéo

**Sans `onSaveInstanceState` — perte totale du compteur à la rotation :**

[sans ViewModel.webm](https://github.com/user-attachments/assets/67509f29-d23f-4099-afbd-b081159151a4)


> **Scénario :** Lancer l'app → cliquer "AJOUTER" 10 fois → faire `Ctrl+F11` (rotation) → le score revient à **0**.

### Limitation de `onSaveInstanceState`

Même en ajoutant la sauvegarde manuelle, cette approche reste limitée :

| Limitation | Détail |
|---|---|
| Types supportés | Seulement primitifs (`int`, `String`, `boolean`…) |
| Objets complexes | Non supportés (Room, Retrofit, listes…) |
| Threads actifs | Non gérés |
| Process death | Comportement imprévisible |

---

## Partie 2 — Avec ViewModel + LiveData (la solution)

### Architecture MVVM appliquée

```
┌─────────────────────────────────────────┐
│              MainActivity               │  ← View
│  observe(scoreViewModel.obtenirScore()) │
│  btnPlus → scoreViewModel.augmenter()   │
└──────────────────┬──────────────────────┘
                   │ observe / appelle
┌──────────────────▼──────────────────────┐
│             ScoreViewModel              │  ← ViewModel
│  valeurScore : MutableLiveData<Integer> │
│  augmenter() / diminuer() / reinit()    │
│  Survit à la rotation (ViewModelStore)  │
└─────────────────────────────────────────┘
```

### Pourquoi ViewModel survit à la rotation

Android conserve le `ViewModel` dans un `ViewModelStore` lié au `LifecycleOwner` (l'Activity). Lors d'une rotation :

- L'Activity est détruite puis recréée → variables perdues
- Le `ViewModel` reste en mémoire dans le `ViewModelStore` → données intactes
- La nouvelle Activity récupère le même `ViewModel` via `ViewModelProvider`

### Pourquoi LiveData ne cause pas de memory leak

`LiveData` est **lifecycle-aware** : il n'émet des mises à jour que si l'Activity est en état `STARTED` ou `RESUMED`. Quand l'Activity est détruite, l'`Observer` est automatiquement supprimé → zéro crash, zéro fuite mémoire.

### Démonstration vidéo

**Avec ViewModel + LiveData — le score survit à la rotation :**

[AVEC ViewModel.webm](https://github.com/user-attachments/assets/a28a5eab-9b58-4539-b695-75b7b41d0269)


> **Scénario :** Lancer l'app → cliquer "AJOUTER" 15 fois → faire `Ctrl+F11` (rotation) → le score reste à **15**.

---

## Bonus — Thread background & SavedStateHandle

### `postValue` depuis un thread secondaire

```java
public void augmenterDepuisThread() {
    new Thread(() -> {
        Integer val = valeurScore.getValue();
        if (val != null) {
            valeurScore.postValue(val + 1); // thread-safe, pas besoin de runOnUiThread()
        }
    }).start();
}
```

| Méthode | Thread | Usage |
|---|---|---|
| `setValue()` | Thread principal uniquement | Mise à jour synchrone |
| `postValue()` | N'importe quel thread | Mise à jour depuis background |

### `SavedStateHandle` — persistance après kill processus

Quand Android tue le processus (manque de mémoire), même le `ViewModel` est perdu. `SavedStateHandle` résout ce cas extrême :

```java
public ScoreViewModel(SavedStateHandle handle) {
    this.etatSauvegarde = handle;
    Integer valeurSauvee = etatSauvegarde.get(CLE_PERSISTANCE);
    valeurScore.setValue(valeurSauvee != null ? valeurSauvee : 0);
}
```

> **Test process death :** `adb shell am kill com.example.scorepersistancedemo` → relancer l'app → le score est restauré.

---

## Tableau comparatif

| Critère | Partie 1 — Sans ViewModel | Partie 2 — Avec ViewModel |
|---|---|---|
| Survie à la rotation | Non (sauf `onSaveInstanceState`) | Oui (`ViewModelStore`) |
| Mise à jour UI automatique | Manuelle (`rafraichirAffichage()`) | Oui (`LiveData` + `Observer`) |
| Support objets complexes | Non | Oui |
| Thread background sécurisé | Non (risque de crash) | Oui (`postValue`) |
| Code propre (MVVM) | Mélangé (logique dans l'Activity) | Séparé (logique dans le ViewModel) |
| Memory leak | Possible | Impossible (lifecycle-aware) |
| Survie au kill processus | Non | Oui (avec `SavedStateHandle`) |

---

## Concepts clés

### `MutableLiveData` vs `LiveData`

```
MutableLiveData<Integer>   →   modifiable (setValue / postValue)
        ↓ exposé via getter comme
LiveData<Integer>          →   lecture seule pour l'Activity
```

Cette séparation empêche l'Activity de modifier directement les données — seul le ViewModel en est responsable.

### `LifecycleOwner` et `Observer`

```java
// "this" = l'Activity = LifecycleOwner
// L'observer est automatiquement retiré quand l'Activity est détruite
scoreViewModel.obtenirScore().observe(this, nouvelleValeur -> {
    scoreDisplay.setText(String.valueOf(nouvelleValeur));
});
```

### `ViewModelProvider`

```java
// Si le ViewModel existe déjà (après rotation) → retourne la même instance
// Si c'est la première fois → en crée une nouvelle
scoreViewModel = new ViewModelProvider(this).get(ScoreViewModel.class);
```

---

## Résumé

Ce TP démontre le fondement de l'architecture **MVVM** (Model-View-ViewModel) recommandée par Google depuis 2018 et utilisée dans toutes les applications professionnelles Android modernes.

```
Avant Jetpack   →   rotation = perte de données + code mélangé
Avec Jetpack    →   données persistantes + UI réactive + code propre + zéro leak
```
