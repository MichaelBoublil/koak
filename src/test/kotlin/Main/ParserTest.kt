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
        val parser = PegParser("def fun(x : int) : int 2 + x;")
        on("Function with int parameter and int return value. Correct syntax.") {
            val tree = parser.parse()
            val ref = AST(
                    LocalDef(
                            Prototype(
                                    Identifier("fun"),
                                    PrototypeArgs(
                                            Params(
                                                    Identifier("x"),
                                                    VarType("int")
                                            ),
                                            FunType("int"))
                            ),
                            Expressions(
                                    Expression(
                                            Unary(PostFix(Primary(Literal(DecimalConst("2"))))),
                                            BinOp("+", true),
                                            Unary(PostFix(Primary(Identifier("x"))))
                                    )
                            )
                    )
            );
            it("should return the following value") {
                println("REFERENCE: " + ref.dump());
                assertEquals(tree.dump(), ref.dump())
            }
        }
    }
})