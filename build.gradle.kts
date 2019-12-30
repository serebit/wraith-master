plugins {
    kotlin("multiplatform") version "1.3.61"
}

group = "com.serebit.wraith"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

kotlin {
    linuxX64 {
        compilations["main"].cinterops {
            create("libusb") { includeDirs("/usr/include") }
        }
        binaries.executable {
            entryPoint = "com.serebit.wraith.main"
        }
    }

    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
}
