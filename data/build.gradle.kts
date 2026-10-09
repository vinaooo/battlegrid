plugins {
    alias(libs.plugins.vinkit.android.library)
    alias(libs.plugins.vinkit.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.vinaooo.battlegrid.data"
}

val vinkit = providers.gradleProperty("vinkit.tag").get()

dependencies {
    implementation(project(":domain"))
    api("com.github.vinaooo.vinkit:settings:$vinkit")
    api("com.github.vinaooo.vinkit:scores:$vinkit")
    api("com.github.vinaooo.vinkit:achievements:$vinkit")
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
