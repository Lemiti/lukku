## Here is expeceted folder structur for Lukku

```text
lukku/
│
├── .github/                  # CI/CD workflows (if any)
├── .gitignore                # Ignore build files, local.properties, IDE files
├── README.md                 # Project overview and build instructions
│
├── landing-page/             # The output you get from Claude goes here
│   ├── index.html
│   └── assets/               # Images, mockups, favicon
│
└── android-app/              # The native Kotlin Android project
    ├── build.gradle.kts      # Project-level Gradle
    ├── settings.gradle.kts
    │
    └── app/
        ├── build.gradle.kts  # App-level Gradle (add Compose, Room, Retrofit, Coroutines)
        └── src/
            └── main/
                ├── AndroidManifest.xml  # AccessibilityService declarations
                ├── res/                 # accessibility_config.xml, icons, strings
                └── java/com/startup/lukku/
                    │
                    ├── LukkuApplication.kt  # Hilt/Koin init or basic app setup
                    │
                    ├── accessibility/       # The OS Intercept Layer
                    │   ├── AccountabilityService.kt
                    │   └── overlay/
                    │       └── ComposeOverlayManager.kt
                    │
                    ├── data/                # The Local Data Layer
                    │   ├── local/
                    │   │   ├── AppDatabase.kt
                    │   │   ├── ScheduleDao.kt
                    │   │   └── SummaryDao.kt
                    │   └── models/
                    │       └── ScheduleBlock.kt
                    │
                    ├── network/             # The AI Network Layer
                    │   ├── GroqApiClient.kt
                    │   ├── GroqApiService.kt
                    │   └── prompts/
                    │       └── SystemPrompts.kt # Store the "Guilt-Inducing" personas here
                    │
                    └── ui/                  # The Presentation Layer
                        ├── theme/           # Compose Material 3 Theme setup
                        ├── main/
                        │   ├── MainActivity.kt
                        │   └── MainViewModel.kt
                        └── overlay/
                            └── BlockScreenUI.kt # The screen that blocks Instagram
```

Copyright © 2026 Lukku. All rights reserved. This is proprietary software. No license is granted to copy, distribute, or modify this code.
