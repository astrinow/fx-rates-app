# Anatomy of a screen

Reference implementation: `feature/rates/list/`.

## Files

```text
feature/<feature>/<screen>/
├── mvi/
│   ├── XxxStore.kt          ← Intent / Action / Message / Label / State
│   ├── XxxReducer.kt        ← pure State.reduce(Message)
│   ├── XxxExecutor.kt       ← side effects
│   └── XxxStoreFactory.kt   ← assembles the Store, starts loading via a Bootstrapper
├── model/                   ← @Immutable UI models (rows, sections)
├── XxxUiState.kt            ← @Immutable state the composables draw
├── XxxUiStateConverter.kt   ← Store.State → UiState
├── XxxUiComponent.kt        ← abstract contract + Default (Factory, Callbacks)
├── XxxUiContent.kt          ← composables
└── XxxUiStatePreviewProvider.kt ← preview states
```

## Data flow

```text
Compose ─click─► UiComponent ─accept(Intent)─► Executor ─forward(Action)─► Executor ─► Repository
                                                   │                          │
                                              publish(Label)           dispatch(Message)
                                                   ▼                          ▼
                                     UiComponent → Callbacks        Reducer → State
                                                                              ▼
                                                     UiStateConverter → UiState → Compose
```

## MVI rules

- `Intent`, `Action`, `Message`, `Label` are nested `sealed class`es of the Store.
- **`State` holds domain models**, wrapped in `Resource` when they are loaded. No strings for
  display, no Compose types.
- **The Reducer is pure**: an expression-body `when` over every `Message`, only `copy(...)`.
- **`executeIntent` only does `dispatch`, `publish` and `forward`.** It may read `state()`.
- **Every side effect starts in `executeAction`**, and `executeAction` never reads `state()`: the
  Action carries everything it needs.
- A repository call is always `resourceFlow { repository.x() }.onEach { dispatch(...) }.launchIn(scope)`
  — never a bare `scope.launch { }` and never `runCatching`.
- A handler is named after its Intent/Action: `Action.LoadRates` → `loadRates(action)`. No
  `handle…`, `process…`, `do…`.
- Initial loading goes through `SimpleBootstrapper(Action.Load…)` in the factory.

## UiState and the converter

- `UiState` contains only what the screen draws, already formatted: strings, flags, UI models.
- All formatting and derived values are computed in `XxxUiStateConverter` through
  `NumberFormatter` and `core/money`. A composable never formats or calculates.
- A loading / error / content alternative is a sealed `Content` inside the `UiState`.

## UiComponent

- `abstract class XxxUiComponent` exposes `state: StateFlow<XxxUiState>` and one function per
  user action. The composable only knows this contract.
- `XxxUiComponentDefault` owns the Store, maps `states` through the converter and turns `Label`s
  into `Callbacks` calls.

## Navigation

- The stack lives in `root/RootUiComponent.kt`: a `@Serializable sealed class Config` and a
  `Child` per screen.
- **A screen does not navigate.** It publishes a `Label`, its component calls a `Callbacks`
  method, and the root pushes a `Config`.
- **A `Config` carries identifiers, not models** (`code`, `base`), so it survives process death.
  The destination screen loads what it needs itself.
- Back is handled by the stack (`handleBackButton = true`); a screen's own back button calls a
  `Callbacks` method, and the root pops.

## Tests

Every screen has at least:

- a **Store test** — a real Store from `XxxStoreFactory` with `DefaultStoreFactory()` and a fake
  repository; asserts `store.state` and the published labels after intents;
- a **UiStateConverter test** — State in, UiState out; this is where the formatted strings and
  the edge cases (empty data, nulls, zero) are checked.

See `RatesListStoreTest` and `RatesListUiStateConverterTest`.
