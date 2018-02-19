package Main

import Llvm.Api
import java.io.File
import java.io.InputStream

class Compiler(file : String) {
    val parser = Parser.PegParser()

    init {
        val inputStream: InputStream = File(file).inputStream()
        val inputString = inputStream.bufferedReader().use { it.readText() }

        val finalInput = inputString.replace(Regex("#.*"), "")
        parser.setString(finalInput.replace(Regex("\n"), ""))
    }

    fun compile() {
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