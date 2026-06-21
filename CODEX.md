# Citrus Mobile Architecture

CODEX.md is a living document. Whenever you encounter something new or something that is important to know in the future, leave it here so future work starts with the right project context.

## Application Shape

Citrus Mobile is an Android app built with Kotlin, Jetpack Compose, Hilt, Room, OkHttp, and coroutines/Flow. The code is organized around a lightweight clean-architecture split:

- `ui/`: Compose screens, screen state, view models, and reusable UI components.
- `navigation/`: App route constants and the Compose navigation host.
- `domain/`: Business-facing models, repository interfaces, auth result types, and realtime contracts.
- `data/`: Repository implementations, local Room entities/DAOs/database, remote HTTP/WebSocket clients, auth token storage, mappers, and dependency injection modules.
- `core/`: Cross-cutting infrastructure such as logging.

The dependency direction should stay mostly one-way:

- UI depends on domain repository interfaces and domain models.
- Data implements domain repository interfaces and maps between data entities and domain models.
- Domain should stay independent of Android framework and data-layer details where practical.
- Dependency wiring belongs in Hilt modules under `data/di` or the closest existing DI module.

## UI Layer

Compose screens live under `app/src/main/java/com/citruschat/citrusmobile/ui`. Screen-level state is modeled with immutable `UiState` data classes and exposed from view models as `StateFlow`. Screens collect state using lifecycle-aware Compose collection.

Keep UI-only interaction state, such as password visibility toggles, in the composable unless it needs to survive process death or participate in business logic. Keep network, database, auth, and persistence behavior in repositories or dedicated data clients.

## Domain Layer

Domain models live under `domain/model`. Repository contracts live under `domain/repository`. Use this layer as the stable interface between UI and data. Prefer adding behavior to interfaces only when the UI or another domain consumer actually needs it.

Auth outcomes are represented with `AuthResult` and `AuthError`; map those errors to UI resources at the boundary where UI messages are needed.

## Data Layer

Room entities and DAOs live under `data/local`. Repository implementations live under `data/repository`. Mapping extensions live under `data/mapper` and should preserve all relevant fields in both directions.

Chats relate to users through the `chat_participants` cross-reference table. Chat list summaries should come from Room relations that include the chat, last message, and participants so search and future chat creation can reason about 1:1 and group membership consistently.

Remote auth uses `AuthApiClient`; realtime chat uses the `ChatRealtimeClient` contract with an OkHttp implementation. Token persistence is handled by `TokenStore` implementations.

Remote user search is handled by `UserApiClient`, which calls `GET /api/v1/users?search=<query>` with bearer auth when a token is available. The response parser accepts common array wrappers such as `data`, `users`, `results`, and `items`.

When parsing remote responses, keep parsing helpers small and deterministic. Prefer explicit exception handling over broad `Exception` catches so detekt stays meaningful.

## Best practices

Instead of using hardcoded strings, colors and sizes. Use resources.

You have resources from strings.xml or color.xml and also MaterialTheme resources. This app uses a custom MaterialTheme with custom sizes, colors and more.