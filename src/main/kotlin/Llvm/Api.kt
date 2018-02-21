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
                var i = 0
                while (i < info.attributes.size - 1) {
                    params += getInfos(info.attributes[i.toString()]!!)
                    i++
                }

                println("la fonction : " + (info.attributes["func"]!!).value)
                println("i : " + i)
                println("param : " + params[0])
                entry.append((info.attributes["func"]!!).value, arrayOf("call", (info.attributes["func"]!!).value, *params.toTypedArray()))
                info.value
            }),

            (InstructionType.DEC_VALUE to {
                info ->
                info.value
            }),
            (InstructionType.EXPRESSION to {
                info ->
                var i = 0
                while (i < info.attributes.size) {
                    getInfos(info.attributes[i.toString()]!!)
                    i++
                }
                info.value

            }),
            (InstructionType.BODY to {
                info ->
                var i = 0
                while (i < info.attributes.size) {
                    getInfos(info.attributes[i.toString()]!!)
                    i++
                }
                info.value

            }),
            (InstructionType.ASSIGNMENT to {
                info ->
                info.value
                /*val mainFunction = ir.modules["main"]!!.functions["main"]!!
                println("assign: " + info.expressions[0].value + ", to: " + info.expressions[1].value)
                // entry.append(info.expressions[0].value, arrayOf("declare", info.expressions[1].value))
                mainFunction.declareLocalVar(info.expressions[0].value, "double", info.expressions[1].value)
                // /entry.append("b", arrayOf("binop", "+", info.expressions[1].value, "0"))
                info.value*/
            }),
            (InstructionType.CALCULUS to {
                info ->
                info.value
                /*val entry = ir.modules["main"]!!.functions["main"]!!.Blocks["entry"]!!
                println(info.value + ": " + info.expressions[0].value + " and " + info.expressions[1].value)
                var params = emptyList<String>()
                for (param in info.expressions) {
                    params += getInfos(param)
                }
                val id = "tmp" + info.value + info.expressions[0].value + info.expressions[1].value
                entry.append(id, arrayOf("binop", info.value, *params.toTypedArray()))
                entry.append("is5", arrayOf("compare ints", id, "5"))
                entry.append("jump", arrayOf("conditional jump", "is5", entry.identifier, entry.identifier))
                info.value*/
            })
    )

    private fun topExprHandler(node: TopExpr) : Info {
        return expressionsHandler(node.children[0] as Expressions)
    }

    private fun localDefHandler(node: LocalDef) : Info {
        println(node.dump())
        val defs = defsHandler(node.children[0] as Defs)
        return defs
        //return (Info(InstructionType.DEF_FUNC, defs[0].value, *defs.drop(1).toTypedArray()))
        /*val myFacFunction = myMod.addFunction(LLVMInt32Type(), "myFactorial", arrayOf(LLVMInt32Type()))
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
        FacRet.append("return result", arrayOf("return", "result"))*/
    }

    private fun defsHandler(node: Defs) : Info {
        val info  = Info(InstructionType.DEF_FUNC)
        for (child in node.children) {
            when (child) {
                is Prototype -> {
                    val expr = prototypeHandler(child)
                    info.attributes += expr.attributes
                }
                is Expressions -> {
                    info.attributes["body"] = expressionsHandler(child)
                }
            }
        }
        return info
    }

    private fun prototypeHandler(node: Prototype): Info {
        val info = Info(InstructionType.PROTOTYPE)
        info.attributes["funName"] = identifierHandler(node.children[0] as Identifier)
        info.attributes += prototypeArgsHandler(node.children[1] as PrototypeArgs).attributes
        return info
    }

    private fun prototypeArgsHandler(node: PrototypeArgs): Info {
        val prototype = Info(InstructionType.PROTOARGS)
        var paramNb = 0
        for (child in node.children) {
            when (child) {
                is Args -> {
                    val param = Info(InstructionType.PARAM)
                    param.attributes["name"] = identifierHandler(child.children[0] as Identifier)
                    param.attributes["type"] = varTypeHandler(child.children[1] as VarType)
                    prototype.attributes["param" + paramNb] = param
                    paramNb += 1
                }
                is FunType -> {
                    prototype.attributes["returnType"] = funTypeHandler(child)
                }
            }
        }
        return prototype
    }

    private fun varTypeHandler(node: VarType): Info {
        return Info(InstructionType.VARTYPE, node.s)
    }

    private fun funTypeHandler(node: FunType): Info {
        return Info(InstructionType.FUNTYPE, node.s)
    }

    private fun expressionsHandler(node: Expressions) : Info {
        val expr = Info(InstructionType.BODY)
        var i = 0
        for (child in node.children) {
            when (child) {
                is ForExpr -> {
                    expr.attributes["expr"+i] = forExprHandler(child)

                }
                is WhileExpr -> {
                    expr.attributes["expr"+i] = whileExprHandler(child)
                }
                is IfExpr -> {
                    expr.attributes["expr"+i] = ifExprHandler(child)
                }
                is Expression -> {
                    expr.attributes["expr"+i] = expressionHandler(child)
                }
                else -> i -= 1
            }
            i+= 1
        }
        return expr
    }

    private fun expressionHandler(node: Expression) : Info {
        val expr = Info(InstructionType.EXPRESSION)
        var i = 0
        for (child in node.children) {

            when (child) {
                is Unary -> {
                    expr.attributes[i.toString()] = unaryHandler(child)
                }
                is BinOp -> {
                    expr.attributes[i.toString()] = binOpHandler(child)
                }
                else -> i -= 1
            }
            i += 1
        }
        return expr
    }

    private fun binOpHandler(node: BinOp) : Info {
        var instr = InstructionType.ERROR
        when (node.s.first()) {
            '=' -> instr = InstructionType.ASSIGNMENT
            in "+-*/" -> instr = InstructionType.CALCULUS
        }
        return if (node.isRightAssoc) {
            val info = Info(instr)
            info.attributes["ope"] = Info(InstructionType.VALUE, node.s)
            info.attributes["lvalue"] = unaryHandler(node.children[0] as Unary)
            info.attributes["rvalue"] = expressionHandler(node.children[1] as Expression)
            info
        }
        else {
            when (node.children[0]) {
                is Unary -> {
                    val info = Info(instr)
                    info.attributes["ope"] = Info(InstructionType.VALUE, node.s)
                    info.attributes["lvalue"] = unaryHandler(node.children[0] as Unary)
                    info.attributes["rvalue"] = unaryHandler(node.children[1] as Unary)
                    info
                }
                is BinOp -> {
                    val info = Info(instr)
                    info.attributes["ope"] = Info(InstructionType.VALUE, node.s)
                    info.attributes["lvalue"] = binOpHandler(node.children[0] as BinOp)
                    info.attributes["rvalue"] = unaryHandler(node.children[1] as Unary)
                    info
                }
                else -> Info(InstructionType.ERROR)
            }
        }

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
            val info = Info(InstructionType.CALL_FUNC)

            val callExprChild = node.children[1] as CallExpr
            val callExprInfo = callExprHandler(callExprChild)
            info.attributes += callExprInfo.attributes
            info.attributes["func"] = Info(InstructionType.VALUE, primaryInfo.value)
            return info
        }
        return (primaryInfo)
    }

    private fun callExprHandler(node: CallExpr) : Info {
        var i = 0
        val params = Info(InstructionType.PARAM)
        for (child in node.children) {
            when (child) {
                is Expression -> {
                    params.attributes += expressionHandler(child).attributes
                    i+=1
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

    fun interpretInfos(infos : Info ) {
        for (child in infos.attributes) {
            getInfos(child.value)
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
                println("lala")
                when (child) {
                    is TopExpr -> {
                        val infos = topExprHandler(child)
                        main.addFunction(LLVMInt32Type(), "putchar", arrayOf(LLVMInt32Type()))
                        infos.dump()
                        println("====================")
                        interpretInfos(infos)
                    }
                    is LocalDef -> {
                        val infos = localDefHandler(child)
                        infos.dump()

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
