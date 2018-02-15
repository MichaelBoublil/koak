package Main

import Parser.*

fun main(args: Array<String>) {
    var p = PegParser()

    p.setString("putchar(48);")
    p.parse()
}