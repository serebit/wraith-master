plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64().compilations["main"].cinterops {
        create("libusb") { includeDirs("/usr/include") }
    }

    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
}
