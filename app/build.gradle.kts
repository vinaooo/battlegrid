plugins {
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.vinkit.android.application)
    alias(libs.plugins.vinkit.android.compose)
    alias(libs.plugins.vinkit.hilt)
    alias(libs.plugins.kotlin.serialization)
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

    // FakeAdsModule keeps the real ads SDKs out of both Robolectric and on-device tests.
    sourceSets {
        getByName("test").kotlin.directories += "src/sharedTest/kotlin"
    }

    androidResources {
        localeFilters += listOf("en", "pt-rBR")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":feature:game"))
    implementation("com.github.vinaooo.vinkit:designsystem:${providers.gradleProperty("vinkit.tag").get()}")
    implementation("com.github.vinaooo.vinkit:shell:${providers.gradleProperty("vinkit.tag").get()}")
    implementation("com.github.vinaooo.vinkit:ads:${providers.gradleProperty("vinkit.tag").get()}")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
