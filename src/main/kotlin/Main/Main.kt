package Main

import Parser.*

fun main(args: Array<String>) {
    var p = PegParser()

    p.setString("456(48);")
    p.parse()
}