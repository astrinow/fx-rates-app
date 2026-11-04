# Code conventions

## Kotlin

- **No `it` in lambdas.** Name the parameter (`{ rate -> rate.code }`) or use a reference
  (`map(CurrencyRate::code)`). Names are full words: `rate`, not `r`.
- **Lambda properties and `invoke` operators are called explicitly:** `onClick.invoke()`.
- **`String.Empty`** (`core/utils`) instead of `""` in State, UiState and converters.
- **`sealed class`**, not `sealed interface`, for closed hierarchies.
- **Braces in a `when` are uniform:** if one branch is a block, all are.
- **Expression body** only when the body fits on one line; a trailing lambda
  (`= Column { … }`, `= when (x) { … }`) is fine.
- Constants: a top-level `const val` is `UpperCamelCase`; inside an `object` or a
  `companion object` it is `SCREAMING_SNAKE_CASE`.
- Lines are at most 120 characters. An empty line after the opening `{` and before the closing
  `}` of a class body.
- KDoc only where it says something the signature does not: a unit, why a value is nullable, an
  invariant. Comments are in English.

## Money and numbers

- **Rates and everything derived from them are `BigDecimal`** (`com.ionspin.kotlin.bignum`).
  `Double` and `Float` are not used for rates, amounts or their changes: binary floating point
  cannot represent most decimal fractions.
- Numbers become strings only through `core/format/NumberFormatter`. No `toString()` of a
  number in UI code, no hand-made rounding.
- A percent is shown with two decimals and an explicit sign (`+1.23%`, `-0.50%`); a value that
  rounds to zero has no sign (`0.00%`).

## Compose

- Spacing comes from `AppTheme.spacing`, semantic colours (up / down / neutral) from
  `AppTheme.colors`, text styles from `MaterialTheme.typography`. No literal `dp` and no literal
  `Color` in a screen.
- User-facing strings are in `ui/Strings.kt`, never inline in a composable.
- The public composable takes the component; it collects the state and delegates to a private
  stateless overload that takes `UiState` and lambdas. Previews render the stateless overload
  with states from the `PreviewParameterProvider`.
- Callbacks are named after the event: `onCurrencyClick`, `onRefresh`, `onBack`.

## Tests

- kotest, `FunSpec`, in `shared/src/commonTest`. Matchers from `io.kotest.matchers`.
- Fakes, not mocks: a hand-written `Fake…Repository` in `commonTest/.../testing`.
- Code that runs on `Dispatchers.Main` gets `Dispatchers.setMain(...)` in `beforeSpec` and
  `resetMain()` in `afterSpec`.
- A test name says the behaviour, not the method: `"a failed load ends in an error"`.
