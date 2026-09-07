plugins {
    id("com.android.application")
}

android {
    namespace = "de.moebelschroeder.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.moebelschroeder.extra"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0-mobile-design"
    }
}
