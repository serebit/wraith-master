plugins {
    kotlin("multiplatform") version "1.3.50"
}

group = "com.serebit.wraith"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

kotlin {
    linuxX64 {
        compilations["main"].cinterops.create("libusb") {
            if (preset == presets["linuxX64"]) includeDirs.headerFilterOnly("/usr/include")
        }

        binaries.executable {
            entryPoint = "com.serebit.wraith.main"
        }
    }
}
