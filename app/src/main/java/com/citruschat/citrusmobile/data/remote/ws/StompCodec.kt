package com.citruschat.citrusmobile.data.remote.ws

internal object StompCodec {
    fun buildFrame(
        command: String,
        headers: LinkedHashMap<String, String>,
        body: String,
    ): String =
        buildString {
            append(command).append('\n')
            headers.forEach { (name, value) ->
                append(name).append(':').append(value).append('\n')
            }
            append('\n')
            append(body)
            append('\u0000')
        }

    fun parseFrames(raw: String): List<StompFrame> =
        raw
            .split('\u0000')
            .mapNotNull { frameText ->
                val normalized = frameText.trim('\r', '\n')
                if (normalized.isBlank()) return@mapNotNull null
                val headerEnd = normalized.indexOf("\n\n")
                val head = if (headerEnd >= 0) normalized.substring(0, headerEnd) else normalized
                val body = if (headerEnd >= 0) normalized.substring(headerEnd + 2) else ""
                val lines = head.lines()
                val command = lines.firstOrNull()?.trim().orEmpty()
                if (command.isBlank()) return@mapNotNull null
                val headers =
                    lines
                        .drop(1)
                        .mapNotNull { line ->
                            val separator = line.indexOf(':')
                            if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
                        }.toMap()
                StompFrame(command = command, headers = headers, body = body)
            }
}

internal data class StompFrame(
    val command: String,
    val headers: Map<String, String>,
    val body: String,
)
