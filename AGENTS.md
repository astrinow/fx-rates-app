# AGENTS.md

Rules for anyone — human or AI agent — changing this repository. They are not suggestions: a
change that ignores them is reviewed as wrong even if it works.

## Read before writing code

| Document                                       | Open it when                                                  |
|------------------------------------------------|---------------------------------------------------------------|
| [`docs/architecture.md`](docs/architecture.md) | Deciding where code goes; adding a model, an endpoint, a repository method |
| [`docs/screen.md`](docs/screen.md)             | Touching any layer of a screen: Store, Executor, UiState, UiComponent, navigation |
| [`docs/conventions.md`](docs/conventions.md)   | Writing any Kotlin or Compose code                            |
| [`docs/commands.md`](docs/commands.md)         | Building, testing, checking the result                        |

`RatesList` (`shared/src/commonMain/kotlin/com/fxrates/feature/rates/list/`) is the reference
screen. A new screen has the same files, the same naming and the same layering.

## Ground rules

- **The stack is fixed:** Kotlin Multiplatform, Compose Multiplatform, Decompose, MVIKotlin,
  kotlin-inject, Ktor, kotlinx.serialization, kotest. No Hilt/Koin, no AndroidX ViewModel, no
  Retrofit, no mocking library. A new dependency needs a reason, and the reason is written in the
  PR / solution notes.
- **All code lives in `shared/src/commonMain`.** `androidMain` and `iosMain` hold only platform
  glue (the HTTP engine). `androidApp` only hosts the root component.
- **Reuse before writing.** Number formatting is `core/format/NumberFormatter`, arithmetic on
  rates is `core/money`. A second formatter or a second "percent change" function is a defect.
- **Every change is verified** with the commands in [`docs/commands.md`](docs/commands.md) before
  it is called done. A change with red tests or a red detekt is not done.
- **Commits are small and meaningful.** One logical step per commit, an English message saying
  what changed and why.

If a rule seems to contradict the existing code, the rule wins — the code may be debt. Say so in
the notes instead of copying the deviation.
