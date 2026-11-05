---
name: android-architecture
description: Expert guidance on setting up and maintaining a modern Android application architecture using Clean Architecture and Hilt. Use this when asked about project structure, module setup, or dependency injection.
---

# Android Modern Architecture & Modularization

## Instructions

When designing or refactoring an Android application, adhere to the **Guide to App Architecture** and **Clean Architecture** principles.

### 1. High-Level Layers
Structure the application into three primary layers. Dependencies must strictly flow **inwards** (or downwards) to the core logic.

*   **UI Layer (Presentation)**:
    *   **Responsibility**: Displaying data and handling user interactions.
    *   **Components**: Activities, Fragments, Composables, ViewModels.
    *   **Dependencies**: Depends on the Domain Layer (or Data Layer if simple). **Never** depends on the Data Layer implementation details directly.
*   **Domain Layer (Business Logic) [Optional but Recommended]**:
    *   **Responsibility**: Encapsulating complex business rules and reuse.
    *   **Components**: Use Cases (e.g., `GetLatestNewsUseCase`), Domain Models (pure Kotlin data classes).
    *   **Pure Kotlin**: Must NOT contain any Android framework dependencies (no `android.*` imports).
    *   **Dependencies**: Depends on Repository Interfaces.
*   **Data Layer**:
    *   **Responsibility**: Managing application data (fetching, caching, saving).
    *   **Components**: Repositories (implementations), Data Sources (Ktor HTTP APIs, the Apollo GraphQL client, Room DAOs). Generated Apollo models stay in the data layer and are mapped to domain models there.
    *   **Dependencies**: Depends only on external sources and libraries.
    *   **Local persistence**: Room 3 (`androidx.room3`). Persisted data uses Room as the single source of truth — DAOs expose `Flow` for reads and suspend functions for writes, and a check-then-write runs in one `@Transaction`. Export schemas (`shared/schemas/`), commit them, and add a `Migration` per version bump instead of a destructive fallback. Per-account data is keyed by the signed-in user's id.

### 2. Dependency Injection with Hilt
Use **Hilt** for all dependency injection.

*   **@HiltAndroidApp**: Annotate the `Application` class.
*   **@AndroidEntryPoint**: Annotate Activities and Fragments.
*   **@HiltViewModel**: Annotate ViewModels; use standard `constructor` injection.
*   **Modules**:
    *   Use `@Module` and `@InstallIn(SingletonComponent::class)` for app-wide singletons (e.g., Network, Database).
    *   Use `@Binds` in an abstract class to bind interface implementations (cleaner than `@Provides`).
    *   **Classes in `:shared` are the exception**: multiplatform common code can't use `javax.inject`, so they carry no annotations and `:app`'s `di/SharedModule.kt` builds them with `@Provides` (`@Singleton` for repositories). This lasts until DI moves to a multiplatform library.

### 3. Modularization Strategy
For production apps, use a multi-module strategy to improve build times and separation of concerns.

**This project today** is moving to Kotlin Multiplatform for an iOS app:

*   **:shared** (KMP library: Android, `iosArm64`, `iosSimulatorArm64`): the domain layer, plus the data code that needs only multiplatform libraries (DTOs, mappers, Room, Apollo, the Ktor clients and APIs, and the repositories built on them; `:app` passes in the engine and anything from `BuildConfig`). `commonMain` has no `android.*`, `java.*` or `javax.*` imports; platform differences go through `expect`/`actual` in `androidMain`/`iosMain`.
*   **:app** (Android application): UI, Hilt modules, and data code still tied to Android or the JVM (Firebase Auth and AI, and the OkHttp base client the Ktor engine, Coil and Apollo share). It moves into `:shared` as multiplatform replacements land.

The target layout below is the direction for splitting further, not the current state:

*   **:app**: The main entry point, connects features.
*   **:core:model**: Shared domain models (Pure Kotlin).
*   **:core:data**: Repositories, Data Sources, Database, Network.
*   **:core:domain**: Use Cases and Repository Interfaces.
*   **:core:ui**: Shared Composables, Theme, Resources.
*   **:feature:[name]**: Standalone feature modules containing their own UI and ViewModels. Depends on `:core:domain` and `:core:ui`.

### 4. Checklist for implementation
- [ ] Ensure `Domain` layer has no Android dependencies.
- [ ] Repositories should default to main-safe suspend functions (use `Dispatchers.IO` internally if needed).
- [ ] ViewModels should interact with the UI layer via `StateFlow` (see `android-viewmodel` skill).
