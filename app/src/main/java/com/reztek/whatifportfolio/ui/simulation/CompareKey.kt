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
            else -> null
        }
    }
}
