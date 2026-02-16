package dev.httpmarco.polocloud.database.test

import dev.httpmarco.polocloud.database.EntryIdentifier

data class TestObject(@EntryIdentifier val name: String, val age: Int) {
}