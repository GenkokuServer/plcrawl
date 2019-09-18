package city.genkoku.plcrawl.collector.maven

import java.util.regex.Pattern

val KEY_PATTERN: Pattern = Pattern.compile("\\\$\\{.+}");
val KEY_PATTERN_STRICT: Pattern = Pattern.compile("\\\$\\{[^\${]+}");

fun String.tokenize(): Iterator<String> {
    val input = this
    return iterator {
        val matcher = KEY_PATTERN_STRICT.matcher(input)
        var lastEnd = 0
        var first = true
        while (matcher.find()) {
            if (!first) {
                yield(input.substring(lastEnd, matcher.start()))
            }
            yield(matcher.group())
            lastEnd = matcher.end()
            first = false
        }
        yield(input.substring(lastEnd));
    }
}

fun String.isKeyComplex(): Boolean {
    return KEY_PATTERN.matcher(this).matches()
}

fun String.isKeyStrict(): Boolean {
    return KEY_PATTERN_STRICT.matcher(this).matches()
}

fun String.flatten(): String {
    assert(this.isKeyStrict())
    return this.trim().substring(this.indexOf("\${") + 2, this.lastIndexOf('}')).also {
        System.err.println("[DEBUG] Flatten property key: $it")
    };
}

fun String.resolve(properties: Map<String, String>): String? {
    assert(this.isKeyStrict())
    for (entry in properties) {
        if (this == entry.key) {
            val result = entry.value

            return if (result.isKeyComplex()) {
                val sb = StringBuilder(result.length)
                for (token in result.tokenize()) {
                    sb.append(
                        if (token.isKeyStrict()) {
                            token.flatten().resolve(properties)
                        } else {
                            token
                        }
                    )
                }
                sb.toString()
            } else result
        }
    }
    return null
}

