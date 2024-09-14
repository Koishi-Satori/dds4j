plugins {
    kotlin("jvm") version "2.0.0"
}

group = "top.kkoishi.dds"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("ar.com.hjg", "pngj", "2.1.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}