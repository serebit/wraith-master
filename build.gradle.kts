plugins {
    kotlin("multiplatform") version "1.3.61" apply false
    base // to add clean task, for cleaning up package output
}

subprojects {
    group = "com.serebit.wraith"
    version = "0.4.1"

    repositories { jcenter() }
}
