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
        val parser = PegParser()
        on("Function with int parameter and int return value. Correct syntax.") {
            parser.setString("def fun(x : int) : int 2 + x;")
            val tree = parser.parse()
            it("should return the following value") {
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
                                                BinOp("+", false),
                                                Unary(PostFix(Primary(Identifier("x"))))
                                        )
                                )
                        )
                );
                println("REFERENCE: " + ref.dump());
                assertEquals(tree.dump(), ref.dump())
            }
        }
        on("Simple Command Hello World. Correct Syntax") {
            parser.setString("putchar(48);")
            val tree = parser.parse()

            it("should return the following value") {
                val ref = AST(
                        TopExpr(
                                Expressions(
                                        Expression(
                                                Unary(PostFix(
                                                        Primary(Identifier("putchar")),
                                                        CallExpr(Expression(Unary(PostFix(Primary(Literal(DecimalConst("48")))))))
                                                ))
                                        )
                                )
                        )
                );
                println("REFERENCE: " + ref.dump());
                assertEquals(tree.dump(), ref.dump())
            }
        }

        // Ce test suggererait qu'on fait en effet l'inférence de type.
        on("Simple Variable affectation WITH inference of type") {
            parser.setString("x = 10;")
            val tree = parser.parse()

            it("should return the following value") {
                val ref = AST(
                        TopExpr(
                                Expressions(
                                        Expression(
                                                Unary(PostFix(Primary(Identifier("x")))),
                                                BinOp("=", true),
                                                Unary(PostFix(Primary(Literal(DecimalConst("10")))))
                                        )
                                )
                        )
                );
                println("REFERENCE: " + ref.dump());
                assertEquals(tree.dump(), ref.dump())
            }
        }
    }
})