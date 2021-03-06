plugins {
    kotlin("multiplatform") version "1.5.10" apply false
    id("com.github.ben-manes.versions") version "0.39.0"
    base // to add clean task, for cleaning up package output
}

allprojects {
    group = "com.serebit.wraith"
    version = "1.2.0"

    repositories {
        mavenCentral()
    }
}
