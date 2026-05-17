# Project Plan

Restructure the AI Wardrobe app from an Android-native project to a Compose Multiplatform project. The core logic, SQLDelight database, and UI (Jetpack Compose) should be moved to a shared `composeApp/commonMain` module to support Android and iOS. Use KMP-compatible versions of Coil, SQLDelight, and Navigation. Handle platform-specifics like Camera and MLKit via abstractions.

## Project Brief

# AI Wardrobe - Compose Multiplatform Project Brief

## Features
*   **AI-Powered Wardrobe Digitization:** Capture or upload clothing photos. The app uses MLKit (via platform-specific implementations) to automatically categorize and tag items (e.g., "Tops," "Outerwear").
*   **Shared Digital Closet:** A unified, cross-platform gallery UI that displays your clothing collection, synchronized and managed through a shared SQLDelight database.
*   **Smart Outfit Generator:** A shared logic layer using TensorFlow Lite to analyze the digital closet and suggest stylish outfit combinations for both Android and iOS users.
*   **Cross-Platform Style Library:** Save and manage favorite outfit combinations with a consistent UI/UX experience across mobile platforms.

## High-Level Technical Stack
*   **Framework:** Compose Multiplatform (CMP) for shared UI and business logic across Android and iOS.
*   **Language:** Kotlin Multiplatform (KMP).
*   **Architecture:** MVVM Clean Architecture (Shared in `commonMain`).
*   **Navigation:** Jetpack Navigation 3 (State-driven, multiplatform-compatible).
*   **Adaptive Strategy:** Compose Material Adaptive library for responsive layouts on diverse screen sizes (phones, tablets, and foldables).
*   **Persistence:** SQLDelight for a multiplatform, type-safe local database.
*   **Concurrency:** Kotlin Coroutines & Flow (Multiplatform).
*   **Image Loading:** Coil 3 (KMP-ready version).
*   **AI/ML:** Platform-specific wrappers for MLKit (Android) and TensorFlow Lite (Shared/Native) to handle image processing and outfit inference.

## Implementation Steps

### Task_1_Foundation: Implement the core data layer and AI infrastructure. This includes setting up the Room database for wardrobe items and outfits, and integrating MLKit Image Labeling and TensorFlow Lite dependencies.
- **Status:** COMPLETED
- **Updates:** Implemented SQLDelight database with ClothingItem and Outfit tables. Implemented Repository pattern with WardrobeRepository and WardrobeRepositoryImpl. Integrated MLKit Image Labeling and LiteRT (TFLite) dependencies. Configured Hilt for DI. Verified successful build with updated SDK and KSP settings.
- **Acceptance Criteria:**
  - Room database with ClothingItem and Outfit entities is functional
  - Repository pattern implemented for data operations
  - MLKit and TFLite dependencies integrated
  - Project builds successfully

### Task_2_DigitalCloset: Develop the Digital Closet Gallery and clothing entry feature. Create an adaptive grid UI to display items, and integrate CameraX/Media Picker for image capture. Implement MLKit to automatically tag clothing items during entry.
- **Status:** COMPLETED
- **Updates:** Implemented WardrobeGalleryScreen with an adaptive grid using LazyVerticalGrid. Integrated ImageLabeler using MLKit for automatic clothing tagging. Integrated Media Picker and Camera for image capture. Implemented WardrobeViewModel for state management. Added FileProvider for secure image storage. UI uses Material 3 cards and Coil for image loading.
- **Acceptance Criteria:**
  - Adaptive grid gallery displays clothing items from database
  - Camera and Media Picker integration works correctly
  - MLKit successfully generates tags for captured images
  - Clothing items are correctly persisted with AI-generated tags

### Task_3_SmartStyling: Implement the Smart Outfit Generator and Style Management. Integrate a TFLite model to suggest outfit combinations and create the UI for the generator and favorites list.
- **Status:** COMPLETED
- **Updates:** Implemented StylingInference engine for outfit suggestions based on item categories (Tops, Bottoms, Shoes). Created StylingScreen with tabs for AI Suggestions and Favorites. Integrated StylingViewModel for managing outfit generation and persistence to SQLDelight. Implemented MainScreen with Bottom Navigation for seamless transition between Closet and Styling screens. App is now fully functional with AI-driven styling and favorite management.
- **Acceptance Criteria:**
  - Outfit generator suggests combinations based on available items
  - Users can save outfits to a 'Favorites' list
  - Favorite outfits are correctly displayed and manageable
  - Styling logic follows MVVM Clean Architecture

### Task_4_CMP_Restructuring: Restructure the AI Wardrobe app into a Compose Multiplatform project. Create the 'composeApp' module and move the core logic, ViewModels, and UI screens to 'commonMain'. Implement abstractions (expect/actual) for platform-specific features like Camera and MLKit Image Labeling.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Project structure follows CMP standards (composeApp/commonMain)
  - SQLDelight database and ViewModels moved to shared module
  - Camera and MLKit abstracted for platform independence
  - Project builds successfully for Android target
- **StartTime:** 2026-05-11 17:26:19 IST

### Task_5_Final_Refinement_Verification: Finalize the shared UI with Material 3, Navigation 3, and Edge-to-Edge display. Integrate KMP-compatible versions of Coil and Navigation. Perform a final verification of the application on Android.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Navigation 3 implemented for state-driven transitions in shared code
  - Material 3 vibrant theme and Edge-to-Edge applied to CMP UI
  - App does not crash, all features work as expected
  - Build pass and stability verified

