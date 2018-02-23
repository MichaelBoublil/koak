package Main

import Llvm.Api
import Llvm.Ir
import org.bytedeco.javacpp.LLVM.LLVMInt32Type

class CLI {
    var parser = Parser.PegParser()
    val llvm = Api()
    var ir = Ir()

    init {
        val main = ir.createModule("main").addFunction(LLVMInt32Type(), "main", arrayOf())
        main.addBlocks("entry", "end")
    }

    fun run() {
        while (true) {
            print("?> ")
            val inputString = readLine() ?: break

            val isCompileCommand = inputString.matches(Regex("^compile \\w+$"))
            if (isCompileCommand) {
                val outputFile = inputString.replace("compile ", "")
                println("You wish to compile to " + outputFile)
                if (ir.modules["main"]!!.functions["main"]!!.Blocks.size == 2)
                    ir.modules["main"]!!.functions["main"]!!.Blocks["entry"]!!.append("jump", arrayOf("jump", "end"))

                ir.modules["main"]!!.functions["main"]!!.Blocks["end"]!!.append("return", arrayOf("return", "0"))
//                ir.modules["main"]!!.functions["main"]!!.Blocks["entry"]!!.append("return", arrayOf("return", "0"))
                ir.verify()
                ir.compile(outputFile)
            } else {
                parser.setString(inputString)
                try {
                    val ast = parser.parse()
                    if (ast.nodes.isEmpty())
                        println("Syntax Error")
                    else {
                        println(ast.dump())
                        ir = llvm.toIR(ast, ir, "CLI")
                        ir.print()
                    }
                } catch (e: Exception) {
                    System.err.println("Compilation Error.")
                    e.printStackTrace()
                }
            }
        }
    }
}