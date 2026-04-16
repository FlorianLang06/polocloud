package de.polocloud.services.sdk

fun main() {

    println("Started service")

    while(true) {
        println("tick")
        Thread.sleep(1000)
    }

    Thread.currentThread().join()
}