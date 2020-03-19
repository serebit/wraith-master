package com.serebit.wraith.core

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.*

@OptIn(ExperimentalUnsignedTypes::class)
val programVersion = listOf("/local/", "/")
    .map { "/usr${it}share/wraith-master/version.txt" }
    .firstOrNull { access(it, R_OK) != -1 }
    ?.let { path ->
        val file = fopen(path, "r")!!
        fseek(file, 0, SEEK_END)

        val fileLength = ftell(file)
        fseek(file, 0, SEEK_SET)

        memScoped {
            val buffer = allocArray<ByteVar>(fileLength + 1)
            fread(buffer, fileLength.toULong(), 1u, file)
            fclose(file)
            buffer.toKString()
        }
    }?.trim()
