# Architecture

## Modules

| Module       | What it is                                                                  |
|--------------|-----------------------------------------------------------------------------|
| `:shared`    | Kotlin Multiplatform library (Android + iOS targets). All the app's code.  |
| `:androidApp`| The Android application: `MainActivity` builds the root component and shows it. |

The iOS targets exist so that `commonMain` cannot use `java.*` or `android.*`. Building an iOS
app is not needed.

## Packages of `:shared`

```text
com.fxrates
├── core/        ← helpers with no knowledge of features
│   ├── format/  ← NumberFormatter: the only place numbers become strings
│   ├── money/   ← arithmetic on rates (RateChangeCalculator)
│   └── utils/   ← Resource, resourceFlow, String.Empty
├── domain/      ← pure models and repository interfaces; no Ktor, no Compose, no DTOs
│   ├── model/
│   └── repository/
├── data/        ← everything that talks to the outside world
│   ├── remote/  ← FrankfurterApi (Ktor), DTOs, the HTTP client
│   └── repository/ ← repository implementations and DTO → domain converters
├── feature/<feature>/<screen>/ ← screens, see screen.md
├── root/        ← the navigation stack of the app
├── ui/          ← AppTheme, Strings
└── di/          ← AppComponent: the kotlin-inject graph
```

Dependencies point one way: `feature → domain ← data`. A screen sees `domain` interfaces and
models only — never a DTO, never `FrankfurterApi`, never `HttpClient`.

## Data

- The API is [Frankfurter](https://frankfurter.dev) (`https://api.frankfurter.dev/v1/`), ECB
  reference rates, no key. Rates exist for business days only: weekends and holidays have no
  entry, so "the last 30 days" is fewer than 30 points.
- An endpoint is a method of `FrankfurterApi` returning a DTO from `data/remote/dto`.
- A DTO is converted into a domain model in `data/repository/*Converter`, never in the screen.
- Numbers from the API become `BigDecimal` (`com.ionspin.kotlin.bignum`) in the converter,
  through the string form, so the decimal digits the API sent are kept.
- A repository is an interface in `domain/repository` and an `*Impl` in `data/repository`, bound
  in `AppComponent`. A new read for an existing data source is a new method on the existing
  repository, not a new repository.

## Dependency injection

kotlin-inject. A class with an `@Inject` constructor needs no registration. An interface is
bound in `di/AppComponent.kt` with a `@Provides fun Impl.bindX(): X = this`. Long-lived objects
(the HTTP client, repositories) are `@AppScope`.

A Decompose component is created through an `@AssistedFactory`: the `ComponentContext`, the
screen arguments and the `Callbacks` are `@Assisted`, everything else is injected.
