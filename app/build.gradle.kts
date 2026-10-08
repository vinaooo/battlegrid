plugins {
    alias(libs.plugins.vinkit.android.application)
    alias(libs.plugins.vinkit.android.compose)
    alias(libs.plugins.vinkit.hilt)
}

android {
    namespace = "io.github.vinaooo.battlegrid"

    defaultConfig {
        applicationId = "io.github.vinaooo.battlegrid"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidResources {
        localeFilters += listOf("en", "pt-rBR")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":feature:game"))
    implementation("com.github.vinaooo.vinkit:shell:${providers.gradleProperty("vinkit.tag").get()}")
    implementation("com.github.vinaooo.vinkit:designsystem:${providers.gradleProperty("vinkit.tag").get()}")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.androidx.test.core)
}
