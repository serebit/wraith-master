package com.serebit.wraith.core

import kotlinx.cinterop.*
import platform.posix.*

val programVersion = memScoped {
    val pathBuffer = allocArray<ByteVar>(PATH_MAX)
    val nchar = readlink("/proc/self/exe", pathBuffer, PATH_MAX)
    check(nchar >= 0) { "Failed to get path to running executable" }

    val installRoot = pathBuffer.toKString().replaceAfterLast('/', "").removeSuffix("/bin/")

    "${installRoot}/share/wraith-master/version.txt"
        .takeIf { access(it, R_OK) != -1 }
        ?.let { path ->
            val file = fopen(path, "r")!!
            val fileLength = alloc<stat>().apply { fstat(fileno(file), ptr) }.st_size
            allocArray<ByteVar>(fileLength + 1).apply {
                fread(this, fileLength.toULong(), 1u, file)
                fclose(file)
            }.toKString().trim()
        }
}
