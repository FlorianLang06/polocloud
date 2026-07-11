package de.polocloud.addons.sign.system

import java.util.LinkedList

class SignPool {

    private val signs = LinkedList<SignData>()

    fun attach(data: SignData) {
        this.signs.add(data)
    }
}