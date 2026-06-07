# Android & Kotlin Development Rules

## Kotlin Naming Conventions
- **Classes/Interfaces**: PascalCase (e.g., `MainViewModel`, `ExpenseRepository`).
- **Functions/Properties**: camelCase (e.g., `loadExpenses()`, `totalCents`).
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_EXPORT_LIMIT`).

## Package Structure
- `data`: Entities, DAOs, Databases, Repositories (implementation).
- `domain`: Use Cases, Repositories (interfaces), Domain models.
- `ui`: ViewModels, UI Screens (Compose), Components.
- `di`: Dependency Injection modules.

## Jetpack Compose Best Practices
- **State Hoisting**: Keep state as high as possible in the composition.
- **Preview**: Every reusable Composable should have a `@Preview`.
- **ViewModels**: Use `viewModel()` from `androidx.lifecycle.viewmodel.compose`.
- **Unidirectional Data Flow**: UI should emit events, ViewModel should emit state.

## Room Database
- Use `Flow` or `Suspend` for DAO methods.
- Keep entities simple and focused on persistence.
- Data transformations should happen in the Repository or ViewModel.

## Logging
- Use `Log.d`, `Log.e`, etc.
- Always gate debug logs with `BuildConfig.DEBUG`.
- Use descriptive tags (e.g., `ClassName:FunctionName`).

## General
- Prefer English for code elements (classes, variables, functions).
- Comments can be in the project's primary language but aim for clarity.
- Follow Clean Architecture principles where it makes sense for project size.
