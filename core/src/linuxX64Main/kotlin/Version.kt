package com.serebit.wraith.core

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalUnsignedTypes::class)
val programVersion = listOf("/local/", "/")
    .map { "/usr${it}share/wraith-master/version.txt" }
    .firstOrNull { access(it, R_OK) != -1 }
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
