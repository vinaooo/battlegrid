plugins {
    alias(libs.plugins.vinkit.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.vinaooo.battlegrid.feature.game"
}

val vinkit = providers.gradleProperty("vinkit.tag").get()

dependencies {
    implementation("com.github.vinaooo.vinkit:shell:$vinkit")
    implementation("com.github.vinaooo.vinkit:bugreport:$vinkit")
    implementation("com.github.vinaooo.vinkit:designsystem:$vinkit")
    implementation("com.github.vinaooo.vinkit:settings:$vinkit")
    implementation("com.github.vinaooo.vinkit:scores:$vinkit")
    implementation("com.github.vinaooo.vinkit:achievements:$vinkit")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
