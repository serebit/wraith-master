package com.serebit.wraith.core

import kotlinx.cinterop.*
import platform.posix.*

private val executablePath = memScoped {
    val pathBuffer = allocArray<ByteVar>(PATH_MAX)
    val nchar = readlink("/proc/self/exe", pathBuffer, PATH_MAX)

    check(nchar >= 0) { "Failed to get path to running executable" }
    pathBuffer.toKString().replaceAfterLast('/', "").replaceAfterLast('/', "")
}

@OptIn(ExperimentalUnsignedTypes::class)
val programVersion = "${executablePath}share/wraith-master/version.txt"
    .takeIf { access(it, R_OK) != -1 }
    ?.let { path ->
        val file = fopen(path, "r")!!
        memScoped {
            val fileLength = alloc<stat>().apply { fstat(fileno(file), ptr) }.st_size
            allocArray<ByteVar>(fileLength + 1).apply {
                fread(this, fileLength.toULong(), 1u, file)
                fclose(file)
            }.toKString().trim()
        }
    }
