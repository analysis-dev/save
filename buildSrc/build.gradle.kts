plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.cqfn.diktat:diktat-gradle-plugin:1.0.0-rc.3")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.17.1")
    implementation(kotlin("gradle-plugin", "1.5.31"))
    implementation(kotlin("serialization", "1.5.31"))
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.0")
    implementation("org.ajoberstar.grgit:grgit-core:4.1.0")
    implementation("com.squareup:kotlinpoet:1.8.0")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.gradle:test-retry-gradle-plugin:1.3.1")
}
