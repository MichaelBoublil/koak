package Main

import Parser.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert.*

import Parser.PegParser

class ParserTest: Spek({
    given("A parser") {
        val parser = PegParser("def fun(i : int) : int ;")
        on("Parse String") {
            val tree = parser.parse()
            val ref = AST(LocalDef(Prototype(Identifier("fun"), PrototypeArgs(Params(Identifier("i"), VarType("int")), FunType("int")))));
            it("should return the following value") {
                println(tree.dump());
                assertEquals(tree.dump(), ref.dump())
            }
        }
    }
})