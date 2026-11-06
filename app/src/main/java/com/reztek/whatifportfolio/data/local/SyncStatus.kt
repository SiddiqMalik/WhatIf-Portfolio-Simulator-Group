package com.reztek.whatifportfolio.data.local

/**
 * Where a cached cloud simulation stands relative to the REST API.
 * Room stores the enum as text.
 *
 * PART3-ASH: offline mode with sync.
 */
enum class SyncStatus {
    /** Matches the server copy. */
    SYNCED,

    /** Renamed on the device; waiting to be PUT to the API. */
    PENDING_UPDATE,

    /** Deleted on the device (hidden from the list); waiting to be DELETEd on the API. */
    PENDING_DELETE,

    /** The server permanently rejected the rename; kept locally so no user data is lost. */
    FAILED_UPDATE,

    /** The server permanently rejected the delete; kept locally so no user data is lost. */
    FAILED_DELETE
}
