package Main

import Parser.*

fun main(args: Array<String>) {
//    var p = PegParser()
//
//    p.setString("putchar(0x34);")
//    val lol = p.parse()
//    if (lol.nodes.isEmpty())
//        println("ERROR")
//    else
//        println(lol.dump())
    try {
        FrontEnd(args)
    }
    catch (e : Exception) {
        e.printStackTrace()
    }
}