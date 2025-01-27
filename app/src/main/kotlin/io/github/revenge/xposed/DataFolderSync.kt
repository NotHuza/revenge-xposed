package io.github.revenge.xposed

import android.os.Environment
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class DataFolderSync(private val packageName: String) {

    private val privateDir = "/data/data/$packageName" // App's private data directory.
    private val publicDir = "${Environment.getExternalStorageDirectory()}/Android/data/$packageName" // User-accessible directory.

    init {
        // Ensure the public directory exists
        val publicDataDir = File(publicDir).apply {
            if (!exists()) mkdirs()
        }

        // Sync public to private folder initially
        syncFolders(File(publicDir), File(privateDir))
    }

    /**
     * Synchronizes changes between the public and private directories.
     */
    fun syncFolders(source: File, destination: File) {
        if (!source.exists()) return

        source.walk().forEach { srcFile ->
            val destFile = File(destination, srcFile.relativeTo(source).path)
            if (srcFile.isDirectory) {
                destFile.mkdirs()
            } else {
                try {
                    Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } catch (e: Exception) {
                    XposedBridge.log("Error copying file ${srcFile.path}: ${e.message}")
                }
            }
        }
    }

    /**
     * Mirrors changes in a specific file between public and private directories.
     */
    fun mirrorFileChange(file: File, isDelete: Boolean) {
        val relativePath = file.path.replaceFirst("^$publicDir|^$privateDir".toRegex(), "")
        val otherFile = if (file.path.startsWith(publicDir)) {
            File(privateDir, relativePath)
        } else {
            File(publicDir, relativePath)
        }

        try {
            if (isDelete) {
                otherFile.delete()
            } else {
                if (!file.exists()) return
                Files.copy(file.toPath(), otherFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            XposedBridge.log("Error mirroring file change: ${file.path} -> ${otherFile.path}: ${e.message}")
        }
    }
}
