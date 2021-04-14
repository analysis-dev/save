plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.cqfn.diktat:diktat-gradle-plugin:0.4.2")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.15.0")
}