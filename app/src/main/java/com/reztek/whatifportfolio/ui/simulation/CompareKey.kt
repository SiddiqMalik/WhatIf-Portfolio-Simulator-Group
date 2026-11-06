package com.reztek.whatifportfolio.ui.simulation

sealed interface CompareKey {
    data class Remote(val id: String) : CompareKey
    data class Local(val id: Long) : CompareKey

    companion object {
        private const val REMOTE_PREFIX = "remote-"
        private const val LOCAL_PREFIX = "local-"

        fun parse(key: String): CompareKey? = when {
            key.startsWith(REMOTE_PREFIX) ->
                key.removePrefix(REMOTE_PREFIX).takeIf { it.isNotBlank() }?.let { Remote(it) }
            key.startsWith(LOCAL_PREFIX) ->
                key.removePrefix(LOCAL_PREFIX).toLongOrNull()?.let { Local(it) }
/**
 * A parsed Saved Simulations key.
 *
 * [SavedListItem.key] uses "local-<Long>" for Room rows and "remote-<id>" for
 * cloud rows. Parsing is kept in one pure place so that "local-7" and
 * "remote-7" can never be confused and malformed keys are rejected before any
 * repository call is made.
 */
sealed interface CompareKey {
    /** Short label that is safe to log (never the id itself). */
    val typeLabel: String

    data class Local(val id: Long) : CompareKey {
        override val typeLabel: String = "local"
    }

    data class Remote(val id: String) : CompareKey {
        override val typeLabel: String = "remote"
    }

    companion object {
        private const val LOCAL_PREFIX = "local-"
        private const val REMOTE_PREFIX = "remote-"

        /** Returns null when [raw] is not a well-formed local or remote key. */
        fun parse(raw: String): CompareKey? = when {
            raw.startsWith(LOCAL_PREFIX) -> {
                val digits = raw.removePrefix(LOCAL_PREFIX)
                val id = if (digits.isNotEmpty() && digits.all { it in '0'..'9' }) {
                    digits.toLongOrNull()
                } else {
                    null
                }
                // Room auto-generated ids start at 1, so 0 is never a saved row.
                id?.takeIf { it > 0 }?.let { Local(it) }
            }
            raw.startsWith(REMOTE_PREFIX) -> {
                val id = raw.removePrefix(REMOTE_PREFIX)
                if (id.isNotBlank() && id.none { it.isWhitespace() || it == '/' }) Remote(id) else null
            }
            else -> null
        }
    }
}
