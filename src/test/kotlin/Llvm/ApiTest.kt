package Llvm

import Main.Compiler
import Main.FrontEnd
import org.junit.Assert.*
import Parser.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert.*

class ApiTest: Spek({
    given("Our LLVM Api using our PegParser") {
        val parser = PegParser()
        val api = Api()

        on("A Basic factorial function") {
            val fakeTree = AST(KDefs(LocalDef(Defs(
                    Prototype(Identifier("myFactorial"), PrototypeArgs(Identifier("nb"), VarType("int"), FunType("int"))),
                    Expressions(
                            IfExpr(BinOp("=", true, Unary(PostFix(Primary(Identifier("nb")))), Unary(PostFix(Primary(Literal(DecimalConst("0")))))),
                                    Expressions(BinOp("*", false, Unary(PostFix(Primary(Identifier("nb")))),
                                            Unary(PostFix(Primary(Identifier("myFactorial")),
                                                    CallExpr(BinOp("-", false, Unary(PostFix(Primary(Identifier("nb")))), Unary(PostFix(Primary(Literal(DecimalConst("1"))))))))))),
                                    Expressions(Unary(PostFix(Primary(Identifier("nb")))))))))))

            it("should return the following value") {
                val ref = 5 * 4 * 3 * 2
                val ret = api.jit(fakeTree)
                assertEquals(ref, ret.value.toInt())
            }
        }
    }
})