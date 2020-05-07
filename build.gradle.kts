import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    java
    kotlin("jvm") version "1.4-M1"
}
group = "parser4k"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven ("https://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation(kotlin("test-annotations-common"))
}

sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("src/main")
}
sourceSets["test"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("src/test")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}
tasks.withType<Test>().configureEach {
    // Larger stack for performance tests on large inputs
    jvmArgs("-Xss10M")
}
