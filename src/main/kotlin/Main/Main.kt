package Main

import Parser.*

fun main(args: Array<String>) {
    var p = PegParser()

    p.setString("d+2;")
    val lol = p.parse()
    if (lol.nodes.isEmpty())
        println("ERROR")
    else
        println(lol.dump())
}