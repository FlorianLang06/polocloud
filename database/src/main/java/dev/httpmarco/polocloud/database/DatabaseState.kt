package dev.httpmarco.polocloud.database

enum class DatabaseState {

    UNKNOWN,
    CONNECTING,
    CONNECTED,
    FAILED,
    CLOSED

}