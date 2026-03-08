package de.polocloud.database.test

import de.polocloud.database.EntryIdentifier

data class TestObject(@EntryIdentifier val name: String, val age: Int) {
}