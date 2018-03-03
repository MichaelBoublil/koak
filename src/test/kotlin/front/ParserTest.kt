package front

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
        on("Function Definition. Function with int parameter and int return value. Correct syntax.") {
            parser.setString("def fun(x : int) : int 2 + x;")
            val tree = parser.parse()
            it("should return the following value") {
                val ref = AST(
                        KDefs(
                                LocalDef(
                                        Defs(
                                                Prototype(
                                                        Identifier("fun"),
                                                        PrototypeArgs(
                                                                Args(
                                                                        Identifier("x"),
                                                                        VarType("int")
                                                                ),
                                                                FunType("int"))
                                                ),
                                                Expressions(
                                                        Expression(
                                                                BinOp("+", false,
                                                                        Unary(PostFix(Primary(Literal(DecimalConst("2"))))),
                                                                        Unary(PostFix(Primary(Identifier("x"))))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                assertEquals(tree.dump(), ref.dump())
            }
        }

        on("Function Definition. Function with int parameter and int return value + call to function. Correct syntax.") {
            parser.setString("def fun(x : int) : int 2 + x; fun(97);")
            val tree = parser.parse()
            it("should return the following value") {
                val ref = AST(
                        KDefs(
                                LocalDef(
                                        Defs(
                                                Prototype(
                                                        Identifier("fun"),
                                                        PrototypeArgs(
                                                                Args(
                                                                        Identifier("x"),
                                                                        VarType("int")
                                                                ),
                                                                FunType("int"))
                                                ),
                                                Expressions(
                                                        Expression(
                                                                BinOp("+", false,
                                                                        Unary(PostFix(Primary(Literal(DecimalConst("2"))))),
                                                                        Unary(PostFix(Primary(Identifier("x"))))
                                                                )
                                                        )
                                                )
                                        )
                                )

                        ),
                        KDefs(
                                TopExpr(
                                        Expressions(
                                                Expression(
                                                        Unary(PostFix(
                                                                Primary(Identifier("fun")),
                                                                CallExpr(Expression(Unary(PostFix(Primary(Literal(DecimalConst("97")))))))
                                                        ))
                                                )
                                        )
                                )
                        )
                )
                assertEquals(tree.dump(), ref.dump())
            }
        }

        on("Multi operator + assignment. Correct syntax.") {
            parser.setString("d = 3 + 2 - 1;")
            val tree = parser.parse()

            it("should return the following value") {
                val ref = AST(
                        KDefs(
                                TopExpr(
                                        Expressions(
                                                Expression(
                                                        BinOp("=", true, Unary(
                                                                PostFix(Primary(Identifier("d")))),
                                                                Expression(
                                                                        BinOp("-", false, BinOp(
                                                                                    "+", false, Unary(PostFix(Primary(Literal(DecimalConst("3"))))),
                                                                                    Unary(PostFix(Primary(Literal(DecimalConst("2")))))),
                                                                                Unary(PostFix(Primary(Literal(DecimalConst("1"))))))
                                                                        )
                                                                )

                                                )
                                        )
                                )
                        )
                )
                assertEquals(tree.dump(), ref.dump())
            }
        }


        on("If and else. Correct syntax.") {
            parser.setString("if 1 < 0 then putchar(97) else putchar(98);")
            val tree = parser.parse()

            it("should return the following value") {
                val ref = AST(
                        KDefs(
                                TopExpr(
                                        Expressions(
                                                IfExpr(
                                                        Expression(
                                                                BinOp("<", false, Unary(
                                                                        PostFix(Primary(Literal(DecimalConst("1"))))),
                                                                        Unary(
                                                                                PostFix(Primary(Literal(DecimalConst("0")))))
                                                                )
                                                        ),
                                                        Expressions(
                                                                Expression(
                                                                        Unary(PostFix(Primary(Identifier("putchar")),
                                                                                CallExpr(Expression(Unary(PostFix(Primary(Literal(DecimalConst("97")))))))
                                                                            )
                                                                        )
                                                                )
                                                        ),
                                                        Expressions(
                                                                Expression(
                                                                        Unary(PostFix(Primary(Identifier("putchar")),
                                                                                CallExpr(Expression(Unary(PostFix(Primary(Literal(DecimalConst("98")))))))
                                                                        )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )

                assertEquals(tree.dump(), ref.dump())
            }
        }

        on("Extern definition of putchar  + call putchar with 97:  Correct Syntax") {
            parser.setString("extern putchar(nb : int) : int; putchar(97);")
            val tree = parser.parse()
            it("should return the following value") {
                val ref = AST(
                        KDefs(
                                ExtDef(
                                        Prototype(
                                                Identifier("putchar"),
                                                PrototypeArgs(
                                                        Args(
                                                                Identifier("nb"),
                                                                VarType("int")
                                                        ),
                                                        FunType("int")
                                                )
                                        )
                                )
                        ),
                        KDefs(
                                TopExpr(
                                        Expressions(
                                                Expression(
                                                        Unary(PostFix(
                                                                Primary(Identifier("putchar")),
                                                                CallExpr(Expression(Unary(PostFix(Primary(Literal(DecimalConst("97")))))))
                                                        ))
                                                )
                                        )
                                )
                        )
                )
                assertEquals(ref.dump(), tree.dump())
            }
        }
    }
})