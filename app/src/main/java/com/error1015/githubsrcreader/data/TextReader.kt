package com.error1015.githubsrcreader.data

import java.io.InputStream

/**
 * 带长度上限的文本读取，并做最基础的二进制探测（出现 NUL 字节即判定为二进制）。
 */
object TextReader {

    const val MAX_BYTES = 800_000

    data class Result(val text: String, val truncated: Boolean, val binary: Boolean)

    fun read(input: InputStream, maxBytes: Int = MAX_BYTES): Result {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        var total = 0
        var binary = false
        var first = true
        while (true) {
            val read = input.read(chunk)
            if (read <= 0) break
            if (first) {
                first = false
                binary = (0 until read).any { chunk[it] == 0.toByte() }
            }
            val allowed = minOf(read, maxBytes - total)
            if (allowed <= 0) return Result(buffer.toString(Charsets.UTF_8.name()), true, binary)
            buffer.write(chunk, 0, allowed)
            total += allowed
            if (total >= maxBytes) return Result(buffer.toString(Charsets.UTF_8.name()), true, binary)
        }
        return Result(buffer.toString(Charsets.UTF_8.name()), false, binary)
    }

    /** 行数过多时截断，避免单次渲染压力过大。 */
    data class Trimmed(val text: String, val lineCount: Int, val truncated: Boolean)

    fun trimLines(text: String, maxLines: Int = 20_000): Trimmed {
        var count = 0
        var index = 0
        while (index < text.length) {
            val newline = text.indexOf('\n', index)
            count++
            if (count > maxLines) {
                return Trimmed(text.substring(0, index).trimEnd('\n'), maxLines, true)
            }
            if (newline < 0) break
            index = newline + 1
        }
        return Trimmed(text, count.coerceAtLeast(1), false)
    }
}

/** 去掉 UTF-8 BOM。 */
fun String.stripBom(): String = if (startsWith('\uFEFF')) substring(1) else this
