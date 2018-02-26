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
        on("Function with int parameter and int return value. Correct syntax.") {
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
        on("Simple Command Hello World. Correct Syntax") {
            parser.setString("putchar(48);")
            val tree = parser.parse()

            it("should return the following value") {
                val ref = AST(
                        KDefs(
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
                        )
                );
                assertEquals(ref.dump(), tree.dump())
            }
        }

//        on("Multiplication Function. Correct Syntax") {
//            parser.setString("def mult(nb1 : int nb2 : int) : int nb1 * nb2;")
//            val tree = parser.parse()
//            val falseTree = "AST(KDefs(LocalDef(Defs(Prototype(Identifier(mult), PrototypeArgs(Identifier(nb1), VarType(int), Identifier(nb2), VarType(int), FunType(int))), Expressions(Expression(BinOp(*; rightAssoc = false; Unary(PostFix(Primary(Identifier(nb1)))), Unary(PostFix(Primary(Identifier(nb2)))))))))))"
//            it("should return the following value") {
//                val ref = AST(
//                        KDefs(
//                                LocalDef(
//                                        Defs(
//                                                Prototype(Identifier("mult"),
//                                                          PrototypeArgs(Identifier("nb1"),
//                                                                        VarType("int"),
//                                                                        Identifier("nb1"),
//                                                                        VarType("int"),
//                                                                        FunType("int"))),
//                                                Expressions(
//                                                   Expression(
//                                                      BinOp("*", false,
//                                                         Unary(
//                                                           PostFix(
//                                                             Primary(
//                                                                Identifier("nb1")))), Unary(PostFix(Primary(Identifier("nb2")))))))))));
//
//                println("REFERENCE: " + ref.dump());
//                println("ACTUAL: " + falseTree);
//                assertEquals(ref.dump(), falseTree)
//            }
//        }
//
//        on("Addition Function. Correct Syntax") {
//            parser.setString("def add(nb1 : int nb2 : int) : int nb1 + nb2;")
//            val tree = parser.parse()
//            val falseTree = "AST(KDefs(LocalDef(Defs(Prototype(Identifier(add), PrototypeArgs(Identifier(nb1), VarType(int), Identifier(nb2), VarType(int), FunType(int))), Expressions(Expression(BinOp(+; rightAssoc = false; Unary(PostFix(Primary(Identifier(nb1)))), Unary(PostFix(Primary(Identifier(nb2)))))))))))"
//            it("should return the following value") {
//                val ref = AST(
//                        KDefs(
//                                LocalDef(
//                                        Defs(
//                                                Prototype(Identifier("add"),
//                                                        PrototypeArgs(Identifier("nb1"),
//                                                                VarType("int"),
//                                                                Identifier("nb1"),
//                                                                VarType("int"),
//                                                                FunType("int"))),
//                                                Expressions(
//                                                        Expression(
//                                                                BinOp("+", false,
//                                                                        Unary(
//                                                                                PostFix(
//                                                                                        Primary(
//                                                                                                Identifier("nb1")))), Unary(PostFix(Primary(Identifier("nb2")))))))))));
//
//                println("REFERENCE: " + ref.dump());
//                println("ACTUAL: " + falseTree);
//                assertEquals(ref.dump(), falseTree)
//            }
//        }
//
//        on("Substraction Function. Correct Syntax") {
//            parser.setString("def sub(nb1 : int nb2 : int) : int nb1 - nb2;")
//            val tree = parser.parse()
//            val falseTree = "AST(KDefs(LocalDef(Defs(Prototype(Identifier(sub), PrototypeArgs(Identifier(nb1), VarType(int), Identifier(nb2), VarType(int), FunType(int))), Expressions(Expression(BinOp(-; rightAssoc = false; Unary(PostFix(Primary(Identifier(nb1)))), Unary(PostFix(Primary(Identifier(nb2)))))))))))"
//            it("should return the following value") {
//                val ref = AST(
//                        KDefs(
//                                LocalDef(
//                                        Defs(
//                                                Prototype(Identifier("sub"),
//                                                        PrototypeArgs(Identifier("nb1"),
//                                                                VarType("int"),
//                                                                Identifier("nb1"),
//                                                                VarType("int"),
//                                                                FunType("int"))),
//                                                Expressions(
//                                                        Expression(
//                                                                BinOp("-", false,
//                                                                        Unary(
//                                                                                PostFix(
//                                                                                        Primary(
//                                                                                                Identifier("nb1")))), Unary(PostFix(Primary(Identifier("nb2")))))))))));
//
//                println("REFERENCE: " + ref.dump());
//                println("ACTUAL: " + falseTree);
//                assertEquals(ref.dump(), falseTree)
//            }
//        }
//
//        on("Division Function. Correct Syntax") {
//            parser.setString("def div(nb1 : int nb2 : int) : int nb1 / nb2;")
//            val tree = parser.parse()
//            val falseTree = "AST(KDefs(LocalDef(Defs(Prototype(Identifier(div), PrototypeArgs(Identifier(nb1), VarType(int), Identifier(nb2), VarType(int), FunType(int))), Expressions(Expression(BinOp(/; rightAssoc = false; Unary(PostFix(Primary(Identifier(nb1)))), Unary(PostFix(Primary(Identifier(nb2)))))))))))"
//            it("should return the following value") {
//                val ref = AST(
//                        KDefs(
//                                LocalDef(
//                                        Defs(
//                                                Prototype(Identifier("div"),
//                                                        PrototypeArgs(Identifier("nb1"),
//                                                                VarType("int"),
//                                                                Identifier("nb1"),
//                                                                VarType("int"),
//                                                                FunType("int"))),
//                                                Expressions(
//                                                        Expression(
//                                                                BinOp("/", false,
//                                                                        Unary(
//                                                                                PostFix(
//                                                                                        Primary(
//                                                                                                Identifier("nb1")))), Unary(PostFix(Primary(Identifier("nb2")))))))))));
//
//                println("REFERENCE: " + ref.dump());
//                println("ACTUAL: " + falseTree);
//                assertEquals(ref.dump(), falseTree)
//            }
//        }

        // Ce test suggererait qu'on fait en effet l'inférence de type.
        /*on("Simple Variable affectation WITH inference of type") {
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
        }*/
    }
})