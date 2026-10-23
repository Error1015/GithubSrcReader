package com.error1015.githubsrcreader.data.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.error1015.githubsrcreader.data.TextReader
import com.error1015.githubsrcreader.data.model.CodeSource
import com.error1015.githubsrcreader.data.model.FileContent
import com.error1015.githubsrcreader.data.model.FileEntry
import com.error1015.githubsrcreader.data.model.Language
import com.error1015.githubsrcreader.data.model.SourceException
import com.error1015.githubsrcreader.data.model.UsageHit
import com.error1015.githubsrcreader.data.model.sortedForDisplay
import com.error1015.githubsrcreader.data.search.ReferenceSearch
import com.error1015.githubsrcreader.data.stripBom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * 通过 SAF（Storage Access Framework）访问用户选择的本地文件夹。
 * 不需要任何存储权限，权限随 Uri 持久化。
 */
class LocalCodeSource(
    private val context: Context,
    private val treeUri: Uri,
    override val title: String,
) : CodeSource {

    override val subtitle: String = "本地文件夹"

    override val supportsRepositorySearch: Boolean = true

    override val repositorySearchHint: String? = null

    private val root: DocumentFile? by lazy { DocumentFile.fromTreeUri(context, treeUri) }

    override suspend fun list(path: String): List<FileEntry> = withContext(Dispatchers.IO) {
        val directory = resolve(path) ?: throw SourceException("无法访问该文件夹，可能已失去读取权限")
        if (!directory.isDirectory) throw SourceException("该路径不是文件夹")
        directory.listFiles()
            .map { file ->
                val name = file.name ?: "(未命名)"
                FileEntry(
                    name = name,
                    path = joinPath(path, name),
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) -1L else file.length(),
                )
            }
            .sortedForDisplay()
    }

    override suspend fun readText(path: String): FileContent = withContext(Dispatchers.IO) {
        val file = resolve(path) ?: throw SourceException("文件不存在或已被移动")
        if (file.isDirectory) throw SourceException("这是一个文件夹")
        val text = readFileText(file, TextReader.MAX_BYTES)
            ?: throw SourceException("无法读取该文件，或它是二进制文件")
        val trimmed = TextReader.trimLines(text)
        FileContent(
            path = path,
            text = trimmed.text,
            language = Language.fromPath(path),
            truncated = trimmed.truncated || file.length() > TextReader.MAX_BYTES,
            sizeBytes = file.length(),
        )
    }

    /**
     * 整个文件夹范围内搜索引用：深度优先遍历，跳过常见构建产物与隐藏目录，
     * 只扫描文本类文件，文件总数与单文件大小都有上限。
     */
    override suspend fun searchReferences(
        name: String,
        onProgress: (scanned: Int, total: Int) -> Unit,
    ): List<UsageHit> = withContext(Dispatchers.IO) {
        val files = ArrayList<FileEntry>()
        collectFiles("", files)
        val hits = ArrayList<UsageHit>()
        files.forEachIndexed { index, entry ->
            coroutineContext.ensureActive()
            onProgress(index + 1, files.size)
            val file = resolve(entry.path) ?: return@forEachIndexed
            val text = runCatching { readFileText(file, MAX_SEARCH_BYTES) }.getOrNull() ?: return@forEachIndexed
            if (text.isNotEmpty()) {
                hits += ReferenceSearch.searchInText(
                    text = text,
                    name = name,
                    language = Language.fromPath(entry.path),
                    path = entry.path,
                )
            }
        }
        hits.sortedWith(compareBy({ it.path }, { it.line }))
    }

    private fun readFileText(file: DocumentFile, maxBytes: Int): String? {
        val stream = context.contentResolver.openInputStream(file.uri) ?: return null
        return stream.use { input ->
            val result = TextReader.read(input, maxBytes)
            if (result.binary) null else result.text.stripBom()
        }
    }

    private fun collectFiles(path: String, out: MutableList<FileEntry>) {
        if (out.size >= MAX_SEARCH_FILES) return
        val directory = resolve(path) ?: return
        for (file in directory.listFiles()) {
            if (out.size >= MAX_SEARCH_FILES) return
            val name = file.name ?: continue
            if (file.isDirectory) {
                if (name.startsWith(".") || name in IGNORED_DIRS) continue
                collectFiles(joinPath(path, name), out)
            } else {
                if (name.startsWith(".")) continue
                if (Language.fromPath(name) == Language.PLAIN) continue
                if (file.length() > MAX_SEARCH_BYTES) continue
                out += FileEntry(name, joinPath(path, name), false, file.length())
            }
        }
    }

    private fun resolve(path: String): DocumentFile? {
        var current = root ?: return null
        val segments = path.split('/').filter { it.isNotBlank() }
        for (segment in segments) {
            current = current.findFile(segment) ?: return null
        }
        return current
    }

    private fun joinPath(parent: String, name: String): String =
        if (parent.isBlank()) name else "$parent/$name"

    companion object {
        private const val MAX_SEARCH_FILES = 600
        private const val MAX_SEARCH_BYTES = 300_000

        private val IGNORED_DIRS = setOf(
            "node_modules", "build", "dist", "out", "target", "vendor",
            "Pods", "DerivedData", "__pycache__", ".gradle", ".idea", ".git",
        )

        /** 读取文件夹显示名，失败时回退到路径末段。 */
        fun displayName(context: Context, uri: Uri): String {
            val fromDocument = runCatching {
                DocumentsContract.getTreeDocumentId(uri)
                    .substringAfterLast(':')
                    .substringAfterLast('/')
            }.getOrNull()
            if (!fromDocument.isNullOrBlank()) return fromDocument
            val fromFile = DocumentFile.fromTreeUri(context, uri)?.name
            if (!fromFile.isNullOrBlank()) return fromFile
            return uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "本地文件夹" }
        }
    }
}
