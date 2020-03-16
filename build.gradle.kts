plugins {
    kotlin("multiplatform") version "1.3.70" apply false
    base // to add clean task, for cleaning up package output
}

allprojects {
    group = "com.serebit.wraith"
    version = "0.5.2"

    repositories { 
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap") 
    }
}

tasks.register("distTar") {
    dependsOn(":cli:package")
    dependsOn(":gtk:package")

    doLast {
        val packageDir = buildDir.resolve("package")
        val tarballName = "wraith-master-$version.tar.xz"

        temporaryDir.resolve("wraith-master").also {
            packageDir.copyRecursively(it, true)
            exec {
                workingDir = temporaryDir
                commandLine("tar", "cfJ", tarballName, it.name)
            }
        }

        temporaryDir.resolve(tarballName).apply {
            copyTo(buildDir.resolve("dist/$name"))
            deleteOnExit()
        }
    }
}
