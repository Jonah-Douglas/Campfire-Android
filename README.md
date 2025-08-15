# Campfire - Android Client

## Spark Connections Through Shared Experiences (Android)

This is the Android client application for **Campfire**, an event aggregation and exploration
platform. It allows users to discover events, connect with others based on shared event interests,
and communicate around those events by interacting with
the [Campfire Backend API](https://github.com/Jonah-Douglas/Campfire).

**Core MVP Vision (Client Perspective):**

* Users can register and authenticate via OTP with their phone number.
* Users can complete their profiles after initial registration.
* Users can create new events.
* (Future) Users can discover and view events sourced by the system.
* Users can express interest or "match" with events.
* Upon matching with an event, users will be able to participate in a group messaging channel
  specific to that event.
* Users can manage their profiles and application settings (e.g., dark mode).
* Users can manage a friends list to facilitate event invitations and joint event creation.

**Project Lead:** Jonah Douglas

---

## Table of Contents

* [Features](#features)
* [Tech Stack & Architecture](#tech-stack--architecture)
* [Project Structure](#project-structure)
* [Getting Started](#getting-started)
    * [Prerequisites](#prerequisites)
    * [Installation & Setup](#installation--setup)
    * [Configuration](#configuration)
* [Logging](#logging)
* [Roadmap & Future Enhancements](#roadmap--future-enhancements)
* [Contributing](#contributing) (Optional)

---

## Features

*(This section should align with the backend features but from the client's implementation
perspective)*

### Current & In-Progress (Android):

* **User Authentication:**
    * OTP-based registration and login using phone numbers.
    * Secure local storage of JWT access and refresh tokens using Android Keystore for encryption.
    * Automatic token refresh handling.
* **User Profile Management:**
    * Initial profile completion after registration.
    * (Foundation for viewing/editing full profile and settings).
* **Core UI & Navigation:**
    * Splash screen and initial data readiness checks.
    * Global state management for authentication and application readiness.
    * Basic navigation structure using Jetpack Compose Navigation.
* **(Foundation for other features like Event Management, Friends, etc.)**

### Planned for MVP (Android):

* **Event Creation:** UI and logic for users to create new events.
* **Event Discovery & Details:** Displaying event lists and individual event details.
* **Event Matching:** UI for users to express interest in events.
* **Event-Based Group Messaging:** UI integration for event-specific group chats.
* **Friendship Management:** UI for sending, accepting, and managing friend requests; viewing
  friends lists.
* **Event Invitations & Joint Creation:** UI flows to support these features.
* **User Settings:** UI for managing app preferences (e.g., theme).
* **Comprehensive Error Handling and User Feedback.**

---

## Tech Stack & Architecture

This Android application is built using modern Android development practices and libraries.

* **Programming Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (for declarative UI)
* **Architecture:** Clean Architecture principles with feature-based packaging.
    * **Layers per Feature:** `presentation` (ViewModel/UI State/Events, Composables), `domain` (Use
      Cases, Models), `data` (Repositories, Data Sources, DTOs, Mappers).
    * **Core Module:** Contains shared utilities, base classes, DI modules for networking, security,
      preferences, etc.
* **State Management:**
    * `ViewModel` with `StateFlow` and `SharedFlow` for UI state and events (MVI-like).
    * `GlobalStateViewModel` for managing app-wide UI states (auth, readiness).
* **Dependency Injection:** Hilt
* **Networking:** Retrofit (for API communication), OkHttp (for HTTP client customization,
  interceptors).
* **Asynchronous Programming:** Kotlin Coroutines & Flow
* **Navigation:** Jetpack Navigation for Compose
* **Data Persistence (Local):**
    * Jetpack DataStore (for user preferences and settings).
    * Encrypted storage for sensitive data like auth tokens (using Android Keystore via
      `EncryptionManager`).
* **Logging:** Timber (wrapped in a custom `Firelog` utility)
* **Build System:** Gradle (with Kotlin DSL potentially)

---

## Project Structure

The project follows a feature-based organization for modularity and clarity:

project_root/<br>
├── com.example.campfire/<br>
│&emsp;├── auth/ # Feature: Authentication & User Onboarding<br>
│&emsp;│&emsp;├── data/<br>
│&emsp;│&emsp;│&emsp;├── local/ # Local data sources (e.g., AuthTokenStorage)<br>
│&emsp;│&emsp;│&emsp;├── mapper/ # DTO to Domain model mappers<br>
│&emsp;│&emsp;│&emsp;├── remote/ # Remote API services & DTOs<br>
│&emsp;│&emsp;│&emsp;└── repository/ # Repository implementations<br>
│&emsp;│&emsp;├── di/ # Hilt DI modules for auth feature<br>
│&emsp;│&emsp;├── domain/<br>
│&emsp;│&emsp;│&emsp;├── model/ # Domain models specific to auth<br>
│&emsp;│&emsp;│&emsp;├── repository/ # Repository interfaces<br>
│&emsp;│&emsp;│&emsp;└── usecase/ # Business logic / use cases<br>
│&emsp;│&emsp;└── presentation/ # ViewModels, Composables, Navigation<br>
│&emsp;│<br>
│&emsp;│&emsp;# ... other feature folders (e.g., events, friends, messaging) ...<br>
│&emsp;│<br>
│&emsp;├── core/ # SHARED: Global configuration, utilities, base classes<br>
│&emsp;│&emsp;├── common/ # Common utilities (logging, exceptions, validation)<br>
│&emsp;│&emsp;├── data/ # Shared data components (network setup, preferences)<br>
│&emsp;│&emsp;├── di/ # Core DI modules (Network, Storage, Security, etc.)<br>
│&emsp;│&emsp;├── domain/ # Core domain models/logic (e.g., SessionInvalidator)<br>
│&emsp;│&emsp;├── presentation/ # Global UI components, navigation, GlobalStateViewModel<br>
│&emsp;│&emsp;└── security/ # Encryption utilities<br>
│&emsp;├── tests/ # Unit and Integration tests, mirroring app structure<br>
│&emsp;├── ...<br>
│&emsp;└── README.md<br>

---

## Getting Started

### Prerequisites

* Android Studio (Latest stable version recommended - Iguana or newer for full AI Assistant
  compatibility if used)
* Android SDK (Target SDK Version - e.g., 34)
* JDK 17 or newer
* An Android device or emulator (API level - e.g., 26+)
* The [Campfire Backend API](https://github.com/Jonah-Douglas/Campfire) must be running and
  accessible.

### Installation & Setup

1. **Clone the repository:**
    * bash git clone [git_url] cd [Campfire]
2. **Open in Android Studio:**
    * Select "Open an existing Android Studio project".
    * Navigate to and select the cloned project directory.
3. **Build the project:**
    * Android Studio should automatically sync Gradle and download dependencies. If not, trigger a
      manual Gradle sync.
    * Build the project (Build > Make Project).

### Configuration

* **API Base URL:**
    * The base URL for the backend API is configured in `core/data/network/...` (e.g., within a
      `NetworkModule` or constants file).
    * Update this URL if your backend is running on a non-default host or port (especially when
      testing with a local backend from an emulator - use `10.0.2.2` for the host machine's
      localhost).
    * Example: `const val BASE_URL = "http://10.0.2.2:8000/api/v1/"`
* **(Add any other specific configuration steps needed, e.g., API keys if you were using third-party
  services directly in the client, specific build variant configurations, etc.)**

---

## Logging

This project uses Timber for logging, wrapped by a custom `Firelog` utility found in
`core.common.logging`.

* Debug builds will log extensively to Logcat.
* Release builds are configured to log less or only critical errors (e.g., to a crash reporting
  service - not yet implemented).
* Use `Firelog.d("message")`, `Firelog.e("error", exception)`, etc. for application logging.

---

## Roadmap & Future Enhancements

* **MVP Completion:**
    *   [X] User Authentication (OTP, Token Management)
    *   [X] Initial Profile Completion
    *   [ ] Full Event Creation UI & Logic
    *   [ ] Event Discovery & Details Screens
    *   [ ] Event Matching UI & Logic
    *   [ ] Event-Based Group Messaging UI Shell & Integration Points
    *   [ ] Friends List Management UI
    *   [ ] Event Invitations & Joint Creation UI
    *   [ ] User Settings Screen (Theme, etc.)
* **Post-MVP / Future:**
    *   [ ] Real-time updates in UI (e.g., using WebSockets or other mechanisms for new messages,
        friend requests, event feed)
    *   [ ] Advanced event filtering and search UI
    *   [ ] Integration for displaying system-sourced events
    *   [ ] User reviews and ratings UI
    *   [ ] Calendar integration
    *   [ ] Offline support for critical data (e.g., caching events, user profile)
    *   [ ] Comprehensive UI testing (Espresso, Compose UI Tests)
    *   [ ] CI/CD pipeline setup for automated builds and releases

---

## Contributing

Currently, this is a solo project. However, if you are interested in contributing in the future,
feel free to reach out.

---

This `README.md` should be a living document. Please update it as the project evolves, new features
are added, or setup instructions change.