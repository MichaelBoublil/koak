package Main

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert.*

import Parser.PegParser

//class ParserTest: Spek({
//    given("A parser") {
//        val parser = PegParser("def fun(i : int) : int { putchar(i) }")
//        on("Parse String") {
//            val tree = parser.Parse()
//            it("should return the following value") {
//                assertEquals("salut", ir)
//            }
//        }
//    }
//})