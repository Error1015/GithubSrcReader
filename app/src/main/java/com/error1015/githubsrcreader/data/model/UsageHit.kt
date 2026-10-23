package com.error1015.githubsrcreader.data.model

/**
 * 一条引用 / 声明命中记录。
 *
 * @param path 文件路径（相对来源根目录）
 * @param line 行号，从 1 开始
 * @param column 列号，从 1 开始
 * @param lineText 该行的完整文本（用于列表预览）
 * @param isDeclaration 是否命中在声明处
 */
data class UsageHit(
    val path: String,
    val line: Int,
    val column: Int,
    val lineText: String,
    val isDeclaration: Boolean = false,
)
