import io.gitlab.arturbosch.detekt.Detekt
import org.cqfn.save.buildutils.configurePublishing
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    val hostTargetName = listOf("linuxX64", "mingwX64", "macosX64")

    linuxX64()
    mingwX64()
    macosX64()

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("okio.ExperimentalFileSystem")
        }
        val commonMain by getting {
            dependencies {
                api("com.squareup.okio:okio-multiplatform:${Versions.okio}")
                implementation( "org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.Kotlinx.serialization}")
                implementation("org.jetbrains.kotlinx:kotlinx-cli:${Versions.Kotlinx.cli}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.Kotlinx.datetime}")
                implementation("com.akuleshov7:ktoml-core:${Versions.ktoml}")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        hostTargetName.forEach {
            getByName("${it}Main").dependsOn(nativeMain)
        }

        val nativeTest by creating {
            dependsOn(commonTest)
        }
        hostTargetName.forEach {
            getByName("${it}Test").dependsOn(nativeTest)
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
            }
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}

// TODO: Remove SaveProperties file and this rule in future commits
diktat {
    excludes = files("src/commonMain/kotlin/org/cqfn/save/core/config/SaveProperties.kt")
}

tasks.withType<Detekt>().configureEach {
    exclude("**/SaveProperties.kt")
}
