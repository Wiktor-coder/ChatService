plugins {
    id("jacoco")
    kotlin("jvm") version "2.1.21"
}

group = "ru.netology"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation ("org.jetbrains.kotlin:kotlin-test")
//    testImplementation(kotlin("test"))
    testImplementation ("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
kotlin {
    jvmToolchain(13)
}