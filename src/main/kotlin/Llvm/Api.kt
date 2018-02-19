package Llvm

import Parser.*
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import java.lang.Thread.sleep

class Api {
    val ir = Ir()
    val main = ir.createModule("main")

    private fun expressionsHandler(node: Expressions) {
        for (child in node.children) {
            when (child) {
                is ForExpr -> {
                    forExprHandler(child)
                }
                is WhileExpr -> {
                    whileExprHandler(child)
                }
                is IfExpr -> {
                    ifExprHandler(child)
                }
                is Expression -> {
                    expressionHandler(child)
                }
            }
        }
    }

    private fun expressionHandler(node: Expression) {

        for (child in node.children) {
            when (child) {
                is Unary -> {
                    unaryHandler(child)
                }
                is BinOp -> {
                    binOpHandler(child)
                }
            }
        }
    }

    private fun binOpHandler(node: BinOp) {}

    private fun unaryHandler(node: Unary) {
        for (child in node.children) {
            when (child) {
                is UnOp -> {
                    unOpHandler(child)
                }
                is PostFix -> {
                    postFixHandler(child)
                }
            }
        }
    }

    private fun postFixHandler(node: PostFix) {
        println("postFix")
        val primaryChild = node.children[0] as Primary
        primaryHandler(primaryChild)
        if (node.children.size > 1) {
            val callExprChild = node.children[1] as CallExpr
            callExprHandler(callExprChild)
        }
    }

    private fun callExprHandler(node: CallExpr) {
        println("callExpr")
        for (child in node.children) {
            when (child) {
                is Expression -> {
                    expressionHandler(child)
                }
            }
        }
    }

    private fun primaryHandler(node: Primary) {
        println("primary")
        for (child in node.children) {
            when (child) {
                is Identifier -> {
                    identifierHandler(child)
                }
                is Literal -> {
                    literalHandler(child)
                }
                is ParenExpr -> {
                    parenExprHandler(child)
                }
            }
        }
    }

    private fun parenExprHandler(node: ParenExpr) {}

    private fun literalHandler(node: Literal) {
        println("literal")
        for (child in node.children) {
            when (child) {
                is HexadecimalConst -> {
                    hexaDecimalConstHandler(child)
                }
                is DecimalConst -> {
                    decimalConstHandler(child)
                }
                is OctalConst -> {
                    octalConstHandler(child)
                }
                is DoubleConst -> {
                    doubleConstHandler(child)
                }
            }
        }
    }

    private fun hexaDecimalConstHandler(node: HexadecimalConst) {}

    private fun decimalConstHandler(node: DecimalConst) : Info {
        println("decimal")
        return Info(node.s)
    }

    private fun octalConstHandler(node: OctalConst) {}

    private fun doubleConstHandler(node: DoubleConst) {}

    private fun identifierHandler(node: Identifier) : Info {
        return Info(node.s)
    }

    private fun unOpHandler(node: UnOp) {}

    private fun forExprHandler(node: ForExpr) {

    }

    private fun ifExprHandler(node: IfExpr) {

    }

    private fun whileExprHandler(node: WhileExpr) {

    }

    private fun topExprHandler(node: TopExpr) {
        for (child in node.children) {
            expressionsHandler(child as Expressions)
        }
    }

    fun toIR(tree: AST) : Ir {
        for (node in tree.nodes) {
            val def = node as KDefs

            for (child in def.children) {
                when (child) {
                    is TopExpr -> {
                        topExprHandler(child)
                    }
                    is LocalDef -> {
                        localDefHandler(child)
                    }
                    is ExtDef -> {
                    }
                }
            }
        }
        return ir
    }

    private fun localDefHandler(node: LocalDef) {}

    fun jit(tree: AST) {
        val ir = toIR(tree)
        val jit = ir.jit()
        return jit
    }

    init {
        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()
    }

    fun grok(args: Array<String>) {
        // An IR is a collection of Modules
        val ir = Ir()

        // That a module, it can contain more than one function
        val myMod = ir.createModule("fac_module")

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
