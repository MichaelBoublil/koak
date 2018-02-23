package Llvm

import Parser.*
import jdk.nashorn.internal.ir.IfNode
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import java.lang.Thread.sleep
import javax.sound.sampled.Line

class Api {
    var incrInstr = 0
    var isInCondBlock : Boolean = false;
    var ir = Ir()
    var context = "main"
    var blockContext = "entry"

    val map : Map<InstructionType, (Info) -> String> = mapOf(
            (InstructionType.CALL_FUNC to {
            info ->
                val entry = ir.modules["main"]!!.functions[context]!!.Blocks[blockContext]!!
                var params = emptyList<String>()
                var i = 0
                while (i < info.attributes.size - 1) {
                    params += getInfos(info.attributes[i.toString()]!!)
                    i++
                }

                entry.append((info.attributes["func"]!!).value, arrayOf("call", (info.attributes["func"]!!).value, *params.toTypedArray()))
                info.attributes["func"]!!.value
            }),

            (InstructionType.DEC_VALUE to {
                info ->
                info.value
            }),
            (InstructionType.VALUE to {
                info ->
                info.value
            }),
            (InstructionType.EXPRESSION to {
                info ->
                var value : String = ""
                var i = 0
                while (i < info.attributes.size) {
                    value = getInfos(info.attributes[i.toString()]!!)
                    i++
                }
                if (context != "main") {
                    val entry = ir.modules["main"]!!.functions[context]!!.Blocks[blockContext]!!

                    entry.append("ret", arrayOf("return", value))
                }
                value
            }),
            (InstructionType.CONDITION to {
                info ->
                val condBlock = isInCondBlock
                isInCondBlock = true
                val actualFunc = ir.modules["main"]!!.functions[context]!!
                val entry = ir.modules["main"]!!.functions[context]!!.Blocks[blockContext]!!
                val ifBlock = actualFunc.addBlock("if" + incrInstr)
                var elseBlock : Ir.Block? = null
                if (info.attributes.size > 2)
                    elseBlock = actualFunc.addBlock("else" + incrInstr)

                val end = try {
                    actualFunc.Blocks["end"]!!
                } catch (e : Exception) {
                    actualFunc.addBlock("end")
                }

                val cond = getInfos(info.attributes["condition"]!!)

                if (elseBlock != null) {
                    entry.append("jump", arrayOf("conditional jump", cond, ifBlock.identifier, elseBlock.identifier))
                }
                else {
                    entry.append("jump", arrayOf("conditional jump", cond, ifBlock.identifier, end.identifier))
                }

                blockContext = "if" + incrInstr
                val ifExpr = getInfos(info.attributes["if"]!!)
                try {
                    actualFunc.search("ret")
                }
                catch(e: Exception) {
                    ifBlock.append("jump", arrayOf("jump", end.identifier))
                }

                if (info.attributes.size > 2) {
                    blockContext = "else" + incrInstr++

                    val elseExpr = getInfos(info.attributes["else"]!!)

                    try {
                        actualFunc.search("ret")
                    }
                    catch(e: Exception) {
                        elseBlock!!.append("jump", arrayOf("jump", end.identifier))
                    }
                }
                if (condBlock)
                    end.append("ret", arrayOf("return", "0"))

                isInCondBlock = false

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
                val mainFunction = ir.modules["main"]!!.functions[context]!!
                val lvalue = getInfos(info.attributes["lvalue"]!!)
                val rvalue = getInfos(info.attributes["rvalue"]!!)

                //TODO:type de variable en fonction de si int ou double.
                mainFunction.declareLocalVar(lvalue, "int32", rvalue, true)

                info.value
            }),
            (InstructionType.CALCULUS to {
                info ->
                var instrType = ""
                val keys = arrayOf("ope", "lvalue", "rvalue")
                val entry = ir.modules["main"]!!.functions[context]!!.Blocks[blockContext]!!
                var params = emptyList<String>()
                var i = 0
                while (i < keys.size) {
                    params += getInfos(info.attributes[keys[i]]!!)
                    i++
                }
                when (params[0]) {
                    "+" ->  instrType = "tmpadd"
                    "*" -> instrType = "tmpmul"
                    "-" -> instrType = "tmpsub"
                    "/" -> instrType = "tmpdiv"
                }
                entry.append(instrType, arrayOf("binop", params[0], *params.drop(1).toTypedArray()))
                instrType
            }),
            (InstructionType.COMPARE to {
                info ->
                val keys = arrayOf("ope", "lvalue", "rvalue")
                val entry = ir.modules["main"]!!.functions[context]!!.Blocks[blockContext]!!
                var params = emptyList<String>()
                var i = 0
                while (i < keys.size) {
                    params += getInfos(info.attributes[keys[i]]!!)
                    i++
                }
                val instrType = params[1] + " " + params[0] + " " + params[2]
                println("INSTRTYPE: $instrType")
                entry.append(instrType, arrayOf("double " + params[0], params[1], params[2]))
                instrType
            }),
            (InstructionType.DEF_FUNC to {
                info ->
                val knownTypes : MutableMap<String, LLVMTypeRef> = mutableMapOf()
                knownTypes["int"] = LLVMInt32Type()
                knownTypes["double"] = LLVMDoubleType()
                knownTypes["void"] = LLVMVoidType()

                var i = 0
                var paramName = emptyList<String>()
                var paramType = emptyList<LLVMTypeRef>()
                while (i < info.attributes.size - 3) {
                    paramType += knownTypes[info.attributes["param" + i]!!.attributes["type"]!!.value]!!
                    paramName += info.attributes["param" + i]!!.attributes["name"]!!.value
                    i++
                }

                val myFunc = ir.modules["main"]!!.
                        addFunction(
                                knownTypes[info.attributes["returnType"]!!.value]!!,
                                info.attributes["funName"]!!.value,
                                arrayOf(*paramType.toTypedArray())
                                )
                i = 0
                for (param in paramName) {
                    myFunc.declareParamVar(param, i++)
                }
                val entry = myFunc.addBlock("entry")
                context = info.attributes["funName"]!!.value
                getInfos(info.attributes["body"]!!)
                context = "main"
                info.value
            })
    )

    private fun topExprHandler(node: TopExpr) : Info {
        return expressionsHandler(node.children[0] as Expressions)
    }

    private fun localDefHandler(node: LocalDef) : Info {
        val defs = defsHandler(node.children[0] as Defs)
        return defs
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
                    expr.attributes[i.toString()] = forExprHandler(child)

                }
                is WhileExpr -> {
                    expr.attributes[i.toString()] = whileExprHandler(child)
                }
                is IfExpr -> {
                    expr.attributes[i.toString()] = ifExprHandler(child)
                }
                is Expression -> {
                    expr.attributes[i.toString()] = expressionHandler(child)
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
        when (node.s) {
            "=" -> instr = InstructionType.ASSIGNMENT
            "==" -> instr = InstructionType.COMPARE
            in "<>" -> instr = InstructionType.COMPARE
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
//        val FacEntry = myFacFunction.Blocks["entry"]!!
//        val FacFalse = myFacFunction.Blocks["iffalse"]!!
//        val FacRet = myFacFunction.Blocks["end"]!!

        val info = Info(InstructionType.CONDITION)
        info.attributes["condition"] = expressionHandler(node.children[0] as Expression)
        info.attributes["if"] = expressionsHandler(node.children[1] as Expressions)

        if (node.children.size > 2)
            info.attributes["else"] = expressionsHandler(node.children[2] as Expressions)

        return info
    }

    private fun whileExprHandler(node: WhileExpr) : Info {
        return Info(InstructionType.ERROR)
    }

    fun interpretInfos(infos : Info ) {
        getInfos(infos)
//        for (child in infos.attributes) {
//            getInfos(child.value)
//        }
    }

    fun getInfos(info : Info) : String {
        return map[info.type]?.invoke(info)!!
    }

    fun toIR(tree: AST, old : Ir? = null) : Ir {
        if (old != null)
            ir = old
        val main = ir.modules["main"]?.let { it } ?: ir.createModule("main")

        main.addFunction(LLVMInt32Type(), "putchar", arrayOf(LLVMInt32Type()))

        main.setMain("main")
        for (node in tree.nodes) {
            val def = node as KDefs

            for (child in def.children) {
                when (child) {
                    is TopExpr -> {
                        val infos = topExprHandler(child)
                        infos.dump()
                        println("====================")
                        interpretInfos(infos)
                    }
                    is LocalDef -> {
                        val infos = localDefHandler(child)
                        infos.dump()
                        interpretInfos(infos)
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

        val putchar = myMod.addFunction(LLVMInt32Type(), "putchar", arrayOf(LLVMInt32Type()))
        putchar.declareParamVar("c", 0)

        // Important to set an entry point in the module for JIT
        val main = myMod.addFunction(LLVMInt32Type(), "main", arrayOf())
        val entrypoint = main.addBlock("entrypoint")
        entrypoint.append("call putchar", arrayOf("call", "putchar", "97"))
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
