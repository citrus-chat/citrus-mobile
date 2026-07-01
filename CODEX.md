# Citrus Mobile Project Guide

This file is the operating guide for Codex and future agents working in this
repository. Keep it current when architecture, API contracts, verification
commands, or project conventions change.

## Project Summary

Citrus Mobile is the Android client for Citrus Chat, an enterprise chat system.
It is written in Kotlin and connects to a remote Spring Boot REST and WebSocket
API. The app currently targets Android 10+ (`minSdk = 29`) and uses Jetpack
Compose, Material 3, Hilt, Room, OkHttp, coroutines, and Flow.

Important build details:

- Package/application id: `com.citruschat.citrusmobile`
- Main module: `app`
- Java/Kotlin toolchain: 21
- Compile/target SDK: 36
- Dependency catalog: `gradle/libs.versions.toml`
- App build config: `app/build.gradle.kts`
- API base URL: `BuildConfig.API_BASE_URL`
- WebSocket base URL: `BuildConfig.WS_BASE_URL`

Do not assume remote contracts from memory. When an API payload shape matters,
verify against the Spring Boot contract, Swagger/OpenAPI, or a real sample
response before changing parsers.

## Repository Shape

Main source root:

`app/src/main/java/com/citruschat/citrusmobile`

Primary packages:

- `ui/`: Compose screens, screen state, view models, and reusable UI components.
- `navigation/`: route constants and the Compose navigation graph.
- `domain/`: domain models, repository interfaces, auth result/state types, and
  realtime contracts.
- `data/`: repository implementations, HTTP clients, WebSocket client, Room
  entities/DAOs/database, mappers, secure storage, and Hilt modules.
- `core/`: cross-cutting infrastructure, currently logging.

The dependency direction should stay simple:

- UI depends on domain models and repository interfaces.
- Data implements domain repository interfaces.
- Data maps between remote/local representations and domain models.
- Domain should not depend on Compose, Room, OkHttp, Android framework classes,
  or Hilt.
- DI wiring belongs in `data/di` or the closest existing module.

## Architecture Rules

Preserve the existing lightweight clean-architecture split.

Use domain interfaces as the boundary between UI and data:

- Add repository methods to `domain/repository` only when a UI or domain
  consumer actually needs them.
- Implement repository behavior in `data/repository`.
- Keep transport details in remote data sources or API clients.
- Keep persistence details in DAOs/entities/mappers.

Prefer small explicit seams over framework-heavy tests:

- Pure parser objects are good JVM test targets.
- Small remote-data-source interfaces are good repository test seams.
- ViewModels should depend on repository interfaces and expose immutable state.

Avoid broad refactors during feature work. Match the nearby style and keep edits
scoped to the user request.

## Compose And UI

Compose screens live under `ui/<feature>`. Feature packages usually contain:

- `<Feature>Screen.kt`
- `<Feature>ViewModel.kt`
- `<Feature>UiState.kt`
- Optional `component/` package for reusable pieces.

UI conventions:

- Screen state is an immutable `UiState` data class exposed as `StateFlow`.
- ViewModels use `@HiltViewModel` and `viewModelScope`.
- Compose screens collect state lifecycle-aware where possible.
- Keep transient UI-only state in Compose unless it participates in business
  logic or must be shared.
- Keep network, database, auth, and long-lived persistence out of composables.
- Use `MaterialTheme`, `strings.xml`, colors, and dimensions instead of
  hardcoded UI values when practical.
- Reuse existing components before introducing new ones.

Current top-level flows:

- `SplashAuthGateScreen` decides authenticated vs unauthenticated navigation.
- `LoginScreen` logs in and navigates to `Routes.Main`.
- `MainScreen` owns the main app area.
- `HomeScreen` lists/searches chats and user search results.
- `ChatScreen` renders a chat by `chatId`.
- `ProfileScreen` shows current-user/profile and theme/logout actions.
- `ConnectedDevicesScreen` and `DeviceQrScannerScreen` handle device flows.

## Auth And Session State

Auth behavior is split across:

- `data/auth/AuthApiClient.kt`
- `data/auth/AuthApiResponseParser.kt`
- `data/auth/TokenStore.kt`
- `data/auth/EncryptedPrefsTokenStore.kt`
- `data/repository/AuthRepositoryImpl.kt`
- `domain/auth/*`

Login request:

- Endpoint: `POST /api/v1/auth/login`
- JSON body fields:
  - `email`
  - `password`
  - `deviceRequest`
- `deviceRequest` includes `deviceId`, `deviceName`, `deviceType`, and
  `publicKey`.

Login response parsing is strict and contract-driven:

- Root object must contain `data`.
- `data` must contain `accessToken`, `email`, `userId`, and `username`.
- Missing required fields are parser failures, not silent defaults.

Auth repository responsibilities:

- Create/load device identity before login.
- Persist access tokens on successful login.
- Persist the current user through `UserRepository`.
- Clear token, device identity, and current-user flag on logout.
- Expose `AuthState` from token state.

Token storage uses Android Keystore-backed AES/GCM encryption in
`EncryptedPrefsTokenStore`. Do not log raw tokens, passwords, private keys, or
authorization header values.

## Device Identity

Device identity is owned by `data/device`.

Current behavior:

- `EncryptedPrefsDeviceIdentityProvider` creates a UUID device id.
- Device type is `"MOBILE"`.
- Device name comes from Android manufacturer/model.
- P-256 ECDH key generation is handled by `P256IdentityKeyGenerator`.
- Public key is sent to the backend.
- Private key material is stored encrypted with Android Keystore AES/GCM.
- Logout clears local device identity.

Avoid changing identity lifecycle casually. Backend pairing and connected-device
features may depend on stable device ids and public keys.

## Remote API Conventions

Remote clients use OkHttp directly, not Retrofit.

Networking setup:

- Shared `OkHttpClient` is provided in `data/di/NetworkModule.kt`.
- Connect timeout: `CONNECT_TIMEOUT`.
- Read timeout: `READ_TIMEOUT = 0L` for long-lived connections.
- Write timeout: `WRITE_TIMEOUT`.
- HTTP logging is controlled by `BuildConfig.ENABLE_APP_LOGGING`.

Remote data-source interfaces:

- `AuthRemoteDataSource`
- `UserRemoteDataSource`

Keep clients small:

- Build request URLs with `HttpUrl` when query parameters are involved.
- Read response bodies once.
- Map HTTP failures to domain results or empty results based on current local
  convention.
- Catch specific exceptions such as `IOException`, `JSONException`, and
  `IllegalArgumentException`.
- Avoid broad `Exception` catches unless there is a clear boundary reason.

## User Search Contract

User search is owned by:

- `data/user/UserApiClient.kt`
- `data/user/UserApiResponseParser.kt`
- `data/repository/UserRepositoryImpl.kt`
- `ui/home/HomeViewModel.kt`

Current endpoint:

- `GET /api/v1/users?search=<query>`
- Adds `Authorization: Bearer <accessToken>` when a nonblank token exists.

Current response parser is intentionally strict:

- Blank body returns an empty list.
- Nonblank body must be a JSON object with a `data` array.
- Each user requires `id`, `email`, and `username`.
- `avatar_url` maps to `User.profilePictureUrl`.

Do not add random fallback field names or wrapper names just to make a broken
payload render. If the backend contract changes, update the parser and parser
tests to match that specific contract.

Home search behavior:

- Search input is debounced by `350ms`.
- Remote user search runs only when trimmed query length is greater than `2`.
- Blank query observes local chats only.
- Short nonblank queries show no results rather than calling the API.
- Selecting a user saves that user locally, finds or creates the direct chat,
  and emits an open-chat event.

Preserve this debounce and minimum-length gate unless the user explicitly asks
to change the product behavior.

## Realtime Chat

Realtime contract:

- `domain/realtime/ChatRealtimeClient.kt`
- `domain/realtime/ChatRealtimeEvent`

OkHttp implementation:

- `data/remote/ws/OkHttpChatRealtimeClient.kt`

Current behavior:

- One socket is kept in an `AtomicReference`.
- Duplicate `connect` calls are ignored while a socket exists.
- A bearer token is attached when provided.
- Events are exposed as a `Flow<ChatRealtimeEvent>`.
- Shared-flow buffer capacity is 64 and drops oldest on overflow.
- Normal disconnect uses close code `1000`.

When adding realtime message handling, keep parsing and persistence separated:
the WebSocket client should manage connection lifecycle and raw events, while a
repository or use-case layer should translate events into domain messages and
database writes.

## Models And Mapping

Domain models live under `domain/model`. Data mappers live under `data/mapper`.

Mapping rules:

- Preserve all meaningful fields in both directions.
- Keep transport naming differences at the parser/client boundary.
- Keep Room naming differences in entity/mapper code.
- Do not make UI depend on Room entities or JSON objects.

Current model areas:

- `User`
- `Chat`
- `ChatListItemSummary`
- `Message`
- `MessageDeliveryStatus`

For profile-related work, keep the user fields aligned with the backend and
local entity: profile picture, email, username, createdAt, status/current-user
state where applicable.

## Logging

Use `core/logging/Logger` instead of direct Android logging in app code.

Guidelines:

- Include stable tags, usually one per class/file.
- Log lifecycle and important state transitions at `i`.
- Log noisy flow/database observations at `v` or `d`.
- Log recoverable failures with useful context.
- Never log secrets: passwords, access tokens, private keys, raw auth headers,
  or full sensitive payloads.

HTTP logging is configured through OkHttp's logging interceptor and the
`ENABLE_APP_LOGGING` build config field.

## Dependency Injection

Hilt modules:

- `data/di/AuthModule.kt`: auth API, token store, device identity, user API,
  auth repository.
- `data/di/DatabaseModule.kt`: Room database, DAOs, repositories, theme
  repository.
- `data/di/NetworkModule.kt`: OkHttp and realtime client binding.
- `core/logging/LoggingModule.kt`: logger bindings/configuration.

Prefer constructor injection for concrete classes. Use provider/bind methods
only at module boundaries or when construction requires build config, Android
context, or third-party builders.

## Testing

Tests live under `app/src/test/java`.

Existing useful tests:

- Parser tests: `UserApiResponseParserTest`, mapper tests.
- Repository tests: `UserRepositoryImplTest`.
- ViewModel tests: `HomeViewModelTest`.
- Utility tests: device key generation and logging.

Testing guidance:

- Add parser tests whenever API response parsing changes.
- Add mapper tests when entity/domain conversion changes.
- Add DAO or repository tests when persistence relationships change.
- Add ViewModel tests for state-flow behavior, debouncing, events, and error
  paths.
- Use `kotlinx-coroutines-test` for coroutine/ViewModel behavior.
- Prefer fake repository/data-source implementations over network mocking when
  the behavior can be tested through a small interface.

Recommended verification commands:

```bash
./gradlew :app:ktlintCheck
./gradlew :app:detekt
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
```

If ktlint or detekt fails because of unrelated pre-existing code, report the
exact failure and still run the most relevant compile/test command for the files
you changed.

## Code Style

Follow the current Kotlin style:

- Keep files formatted for ktlint.
- Prefer immutable data classes for UI/domain state.
- Prefer `StateFlow` for observable state and `SharedFlow` for one-shot events.
- Keep constants close to the code that owns the behavior.
- Use explicit exception types at boundaries.
- Keep comments rare and useful.
- Use ASCII in source and docs unless a file already uses another character set
  or the user explicitly asks for localized text.

When adding dependencies:

- Prefer the version catalog for shared dependency coordinates.
- Avoid new libraries when an existing project dependency already solves the
  problem.
- Keep core chat/auth/data behavior testable without Android UI tests when
  practical.

## Security And Privacy

This app handles enterprise chat data, auth tokens, and device keys. Treat those
as sensitive.

Rules:

- Do not print credentials or tokens.
- Do not persist secrets outside encrypted storage.
- Do not add sample real credentials to source files.
- Keep token access behind `TokenStore`.
- Keep device private-key material behind `DeviceIdentityProvider`.
- Be careful with screenshots, logs, fixtures, and test resources that may
  contain production data.

## Working With Backend Contracts

The backend is a remote Spring Boot service. Contracts can drift independently
from this client.

Before changing request/response behavior:

- Check the relevant client and parser first.
- Verify the backend endpoint path, method, headers, request body, and response
  body shape.
- Keep parsing strict and close to the contract.
- Update tests with contract-shaped fixtures.
- Avoid temporary client-side normalization if the backend is expected to be
  fixed. Remove temporary workarounds once the API is corrected.

If network access or backend docs are unavailable, state the assumption clearly
in the final response and keep changes isolated behind parsers or data-source
interfaces.