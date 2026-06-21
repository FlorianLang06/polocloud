package de.polocloud.api.group

import java.util.function.Consumer

class GroupService {

    fun findAll(): List<Group> {
        TODO()
    }

    fun find(name: String): Group? {
        TODO()
    }

    fun find(type: GroupFilterType) : List<Group> {
        TODO()
    }

    fun count() : Int {
        TODO()
    }

    fun delete(name: String) {
        TODO()
    }

    fun delete(group: Group) {
        TODO()
    }

    fun edit(editor: Consumer<GroupBuilder>) {
        TODO()
    }

    fun create(name: String) : GroupBuilder {
        TODO()
    }

}