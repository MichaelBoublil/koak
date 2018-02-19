package Main

import Llvm.Api
import Llvm.Ir

class CLI {
    var parser = Parser.PegParser()

    fun run() {
        while (true) {
            print("?> ")
            val inputString = readLine() ?: break
            parser.setString(inputString)
            try {
                val ast = parser.parse()
                if (ast.nodes.isEmpty())
                    println("Syntax Error")
                else {
                    println(ast.dump())
                    val llvm = Api()
                    val ir = llvm.toIR(ast)
                    ir.print()
                }
            }
            catch(e : Exception) {
                System.err.println("Compilation Error.")
//                e.printStackTrace()
            }
        }
    }
}