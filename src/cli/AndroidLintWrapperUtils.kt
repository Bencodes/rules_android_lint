package com.rules.android.lint.cli

import java.io.File
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.inputStream

internal fun unzip(
  src: Path,
  dst: Path,
) {
  val dstFile = dst.toFile()
  val bufferedZipInputStream = src.inputStream().buffered()
  ZipInputStream(bufferedZipInputStream).use { zipStream ->
    var zipEntry = zipStream.nextEntry
    while (zipEntry != null) {
      if (zipEntry.isDirectory) {
        File(dstFile, zipEntry.name).mkdirs()
      } else {
        File(dstFile, zipEntry.name)
          .also { it.parentFile.mkdirs() }
          .outputStream()
          .use { fileOutputStream -> zipStream.copyTo(fileOutputStream) }
      }

      zipStream.closeEntry()
      zipEntry = zipStream.nextEntry
    }
  }
}
