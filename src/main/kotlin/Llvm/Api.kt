package Llvm

import Parser.*
import jdk.nashorn.internal.ir.IfNode
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import java.lang.Thread.sleep
import javax.sound.sampled.Line

class Api {
    var ir = Ir()

    val map : Map<InstructionType, (Info) -> String> = mapOf(
            (InstructionType.CALL_FUNC to {
            info ->
                val entry = ir.modules["main"]!!.functions["main"]!!.Blocks["entry"]!!
                var params = emptyList<String>()
                for (param in info.expressions) {
                    params += getInfos(param)
                }
                entry.append(info.value, arrayOf("call", info.value, *params.toTypedArray()))
                info.value
            }),
            (InstructionType.DEC_VALUE to {
                info ->
                info.value
            }),
            (InstructionType.ASSIGNMENT to {
                info ->
                val mainFunction = ir.modules["main"]!!.functions["main"]!!
                println("assign: " + info.expressions[0].value + ", to: " + info.expressions[1].value)
                // entry.append(info.expressions[0].value, arrayOf("declare", info.expressions[1].value))
                mainFunction.declareLocalVar(info.expressions[0].value, "double", info.expressions[1].value)
                // /entry.append("b", arrayOf("binop", "+", info.expressions[1].value, "0"))
                info.value
            }),
            (InstructionType.ADD to {
                info ->
                val entry = ir.modules["main"]!!.functions["main"]!!.Blocks["entry"]!!
                println("add: " + info.expressions[0].value + " and " + info.expressions[1].value)
                entry.append("tmpadd", arrayOf("binop", "+", info.expressions[0].value, info.expressions[1].value))
                entry.append("is5", arrayOf("compare ints", "tmpadd", "5"))
                entry.append("jump", arrayOf("conditional jump", "is5", entry.identifier, entry.identifier))
                info.value
            })
    )

    private fun topExprHandler(node: TopExpr) : List<Info> {
        return expressionsHandler(node.children[0] as Expressions)
    }

    private fun localDefHandler(node: LocalDef) {}

    private fun expressionsHandler(node: Expressions) : List<Info> {
        var expr : List<Info> = emptyList()
        for (child in node.children) {
            when (child) {
                is ForExpr -> {
                    expr += forExprHandler(child)
                }
                is WhileExpr -> {
                    expr += whileExprHandler(child)
                }
                is IfExpr -> {
                    expr += ifExprHandler(child)
                }
                is Expression -> {
                    expr += expressionHandler(child)
                }
            }
        }
        return expr
    }

    private fun expressionHandler(node: Expression) : List<Info> {
        var expr : List<Info> = emptyList()
        for (child in node.children) {
            when (child) {
                is Unary -> {
                    expr += unaryHandler(child)
                }
                is BinOp -> {
                    expr += binOpHandler(child)
                }
            }
        }
        return expr
    }

    private fun binOpHandler(node: BinOp) : Info {
        var instr = InstructionType.ERROR
        when (node.s) {
            "=" -> instr = InstructionType.ASSIGNMENT
            "+" -> instr = InstructionType.ADD
        }
        return if (node.isRightAssoc)
            Info(instr, node.s, unaryHandler(node.children[0] as Unary), *expressionHandler(node.children[1] as Expression).toTypedArray())
        else
            Info(instr, node.s, unaryHandler(node.children[0] as Unary), unaryHandler(node.children[1] as Unary))
    }

    private fun unaryHandler(node: Unary) : Info {
        val child = node.children[0]
        return when (child) {
            is UnOp -> {
                unOpHandler(child)
            }
            is PostFix -> {
                postFixHandler(child)
            }
            else -> Info(InstructionType.ERROR)
        }
    }


    private fun postFixHandler(node: PostFix) : Info {
        println("postFix")
        val primaryChild = node.children[0] as Primary
        val primaryInfo = primaryHandler(primaryChild)
        if (node.children.size > 1) {
            val callExprChild = node.children[1] as CallExpr
            val callExprInfo = callExprHandler(callExprChild)
            return (Info(InstructionType.CALL_FUNC, primaryInfo.value, *callExprInfo.toTypedArray()));
        }
        return (primaryInfo)
    }

    private fun callExprHandler(node: CallExpr) : List<Info> {
        println("callExpr")
        var params : List<Info> = emptyList()
        for (child in node.children) {
            when (child) {
                is Expression -> {
                    params += expressionHandler(child)
                }
            }
        }
        return params
    }

    private fun primaryHandler(node: Primary) : Info {
        println("primary")
        val child = node.children[0]
        return when (child) {
            is Identifier -> {
                identifierHandler(child)
            }
            is Literal -> {
                literalHandler(child)
            }
            is ParenExpr -> {
                parenExprHandler(child)
            }
            else -> Info(InstructionType.ERROR)
        }
    }

    private fun parenExprHandler(node: ParenExpr) : Info {return Info(InstructionType.ERROR)}

    private fun literalHandler(node: Literal) : Info {
        println("literal")
        val child = node.children[0]
        return when (child) {
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
            else -> Info(InstructionType.ERROR)
        }
    }

    private fun hexaDecimalConstHandler(node: HexadecimalConst) : Info { return Info(InstructionType.ERROR)}

    private fun decimalConstHandler(node: DecimalConst) : Info {
        println("decimal")
        return Info(InstructionType.DEC_VALUE, node.s)
    }

    private fun octalConstHandler(node: OctalConst) : Info {return Info(InstructionType.OCT_VALUE)}

    private fun doubleConstHandler(node: DoubleConst) : Info {return Info(InstructionType.DOUBLE_VALUE)}

    private fun identifierHandler(node: Identifier) : Info {
        return Info(InstructionType.VALUE, node.s)
    }

    private fun unOpHandler(node: UnOp) : Info {return Info(InstructionType.ERROR)}

    private fun forExprHandler(node: ForExpr) : Info {
        return Info(InstructionType.ERROR)
    }

    private fun ifExprHandler(node: IfExpr) : Info {
        return Info(InstructionType.ERROR)
    }

    private fun whileExprHandler(node: WhileExpr) : Info {
        return Info(InstructionType.ERROR)
    }

    fun interpretInfos(infos : List<Info> ) {
        for (child in infos) {
            map[child.type]?.invoke(child)
        }
    }

    fun getInfos(info : Info) : String {
        return map[info.type]?.invoke(info)!!
    }

    fun toIR(tree: AST, old : Ir? = null) : Ir {
        if (old != null)
            ir = old
        val main = ir.modules["main"]?.let { it } ?: ir.createModule("main")
        main.setMain("main")
        for (node in tree.nodes) {
            val def = node as KDefs

            for (child in def.children) {
                when (child) {
                    is TopExpr -> {
                        val infos = topExprHandler(child)
                        main.addFunction(LLVMInt32Type(), "putchar", arrayOf(LLVMInt32Type()))
                        interpretInfos(infos)
                    }
                    is LocalDef -> {
                        localDefHandler(child)
                    }
                    is LocalDef -> {

                    }
                    is ExtDef -> {
                    }
                }
            }
        }
        return ir
    }

    fun jit(tree: AST): MutableList<Jit> {
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

        FacEntry.append("n == 1", arrayOf("int ==", "n", "1"))
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
        val main = myMod.addFunction(LLVMInt32Type(), "main", arrayOf())
        val entrypoint = main.addBlock("entrypoint")
        entrypoint.append("ret", arrayOf("return", "0"))
        myMod.setMain("main")

        myMod.print()
        ir.verify()
        sleep(1000)
        val arr = ir.jit("fac_module")
        val res = arr[0].runFunction("myFactorial", arrayOf(5))
        println(res.content)
        ir.compile("compiledIR")
    }
}
