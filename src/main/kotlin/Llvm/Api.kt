package Llvm

import Parser.*
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import java.lang.Thread.sleep

class Api {
    fun test() : Test
    {
        val parser = PegParser()
        val api = Api()

        val fakeTree = AST(KDefs(LocalDef(Defs(
                Prototype(Identifier("myFactorial"), PrototypeArgs(Identifier("nb"), VarType("int"), FunType("int"))),
                Expressions(
                        IfExpr(BinOp("=", true, Unary(PostFix(Primary(Identifier("nb")))), Unary(PostFix(Primary(Literal(DecimalConst("0")))))),
                                Expressions(BinOp("*", false, Unary(PostFix(Primary(Identifier("nb")))),
                                        Unary(PostFix(Primary(Identifier("myFactorial")),
                                                CallExpr(BinOp("-", false, Unary(PostFix(Primary(Identifier("nb")))), Unary(PostFix(Primary(Literal(DecimalConst("1"))))))))))),
                                Expressions(Unary(PostFix(Primary(Identifier("nb")))))))))))

        val ref = 5 * 4 * 3 * 2
        val ret = api.jit(fakeTree)

        if (ref == ret.value.toInt())
            return Test(true, ret.value)
        return Test(false, ret.value)
    }

    // TopLevel abstraction
    fun toIR(tree: AST) : Ir {
        val ir = Ir()

        return ir
    }

    fun jit(tree: AST) : Jit {
        // Build the module from the ast
        // Jit the module
        // extract return value of jit ?
        throw Error("Not Implemented")
    }

    init {
        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()
    }

    // This function is to be ignored. It's a simple example.
    fun grok(args: Array<String>) {
        val ir = Ir()
        val myMod = ir.createModule("fac_module")
        // on pourrait avoir un array de identifier<>Type ?
        val myFacFunction = myMod.addFunction(LLVMInt32Type(), "myFactorial", arrayOf(LLVMInt32Type()))
        myFacFunction.declareParamVar("n", 0)

        // On pourrait faciliter la creation de plusieurs label d'un coup
        // myFacFunction.bulkAddBlocks("entry", "iffalse", "end") (donc 4 lignes au lieu de 3... super !)
        myFacFunction.addBlocks("entry", "iffalse", "end")
        val FacEntry = myFacFunction.Blocks["entry"]!!
        val FacFalse = myFacFunction.Blocks["iffalse"]!!
        val FacRet = myFacFunction.Blocks["end"]!!
//        val FacEntry = myFacFunction.addBlock("entry")
//        val FacFalse = myFacFunction.addBlock("iffalse")
//        val FacRet = myFacFunction.addBlock("end")

        FacEntry.append("n == 1", arrayOf("compare ints", "n", "1"))
        FacEntry.append("jump", arrayOf("conditional jump", "n == 1", FacRet.identifier, FacFalse.identifier))

        myFacFunction.createConstInt("-1")
        FacFalse.append("n - 1", arrayOf("binop", "+", "n", "-1"))
        FacFalse.append("fac(n - 1)", arrayOf("call", myFacFunction.identifier, "n - 1"))
        FacFalse.append("n * fac(n - 1)", arrayOf("binop", "*", "n", "fac(n - 1)"))
        FacFalse.append("jump", arrayOf("jump", FacRet.identifier))

        myFacFunction.createConstInt("1")
        FacRet.append("result", arrayOf("phi int",
                FacFalse.identifier, "n * fac(n - 1)",
                FacEntry.identifier, "1"))
        FacRet.append("return result", arrayOf("return", "result"))

        // Important to set an entry point in the module for JIT
        myMod.setMain("myFactorial")

        ir.verify()
        myMod.print()
        ir.jit()
    }
}
