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
        main.addBlock("entry")
    }

    fun run() {
        while (true) {
            print("?> ")
            val inputString = readLine() ?: break

            val isCompileCommand = inputString.matches(Regex("^compile \\w+$"))
            if (isCompileCommand) {
                val outputFile = inputString.replace("compile ", "")
                println("You wish to compile to " + outputFile)
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
                        ir = llvm.toIR(ast, ir)
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