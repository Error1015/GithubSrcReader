package com.error1015.githubsrcreader.data.model

/**
 * 支持的语法高亮语言。仅用于挑选词法规则与展示标签，不做编译级解析。
 */
enum class Language(val displayName: String) {
    KOTLIN("Kotlin"),
    JAVA("Java"),
    GROOVY("Gradle"),
    PYTHON("Python"),
    JAVASCRIPT("JavaScript"),
    TYPESCRIPT("TypeScript"),
    C("C"),
    CPP("C++"),
    CSHARP("C#"),
    GO("Go"),
    RUST("Rust"),
    RUBY("Ruby"),
    PHP("PHP"),
    SWIFT("Swift"),
    DART("Dart"),
    LUA("Lua"),
    SHELL("Shell"),
    SQL("SQL"),
    JSON("JSON"),
    YAML("YAML"),
    TOML("TOML"),
    XML("XML"),
    HTML("HTML"),
    CSS("CSS"),
    MARKDOWN("Markdown"),
    PLAIN("Text"),
    ;

    val isMarkup: Boolean get() = this == XML || this == HTML

    companion object {
        private val byFileName = mapOf(
            "dockerfile" to SHELL,
            "makefile" to SHELL,
            "gradlew" to SHELL,
            "cmakelists.txt" to PLAIN,
            "gemfile" to RUBY,
            "podfile" to RUBY,
            ".gitignore" to PLAIN,
            ".gitattributes" to PLAIN,
            ".editorconfig" to PLAIN,
        )

        private val byExtension = mapOf(
            "kt" to KOTLIN, "kts" to KOTLIN,
            "java" to JAVA,
            "gradle" to GROOVY,
            "py" to PYTHON, "pyi" to PYTHON,
            "js" to JAVASCRIPT, "jsx" to JAVASCRIPT, "mjs" to JAVASCRIPT, "cjs" to JAVASCRIPT,
            "ts" to TYPESCRIPT, "tsx" to TYPESCRIPT, "mts" to TYPESCRIPT,
            "c" to C, "h" to C,
            "cpp" to CPP, "cc" to CPP, "cxx" to CPP, "hpp" to CPP, "hh" to CPP, "hxx" to CPP,
            "cs" to CSHARP,
            "go" to GO,
            "rs" to RUST,
            "rb" to RUBY,
            "php" to PHP,
            "swift" to SWIFT,
            "dart" to DART,
            "lua" to LUA,
            "sh" to SHELL, "bash" to SHELL, "zsh" to SHELL, "fish" to SHELL, "ps1" to SHELL,
            "sql" to SQL,
            "json" to JSON, "json5" to JSON,
            "yml" to YAML, "yaml" to YAML,
            "toml" to TOML,
            "xml" to XML, "svg" to XML, "plist" to XML,
            "html" to HTML, "htm" to HTML, "vue" to HTML, "svelte" to HTML,
            "css" to CSS, "scss" to CSS, "less" to CSS,
            "md" to MARKDOWN, "markdown" to MARKDOWN, "mdx" to MARKDOWN,
        )

        fun fromPath(path: String): Language {
            val fileName = path.substringAfterLast('/').lowercase()
            byFileName[fileName]?.let { return it }
            val ext = fileName.substringAfterLast('.', "")
            if (ext.isEmpty()) return PLAIN
            return byExtension[ext] ?: PLAIN
        }
    }
}
