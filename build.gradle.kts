import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    java
    kotlin("jvm") version "1.3.72"
    `maven-publish`
}
group = "parser4k"
version = "0.01"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("stdlib"))
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

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("parser4k") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = URI("https://api.bintray.com/maven/dkandalov/maven/parser4k/;publish=1")
            credentials {
                username = System.getenv("BINTRAY_USER")
                password = System.getenv("BINTRAY_API_KEY")
            }
        }
    }
}
