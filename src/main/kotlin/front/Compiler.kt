package front

import Llvm.Api
import Llvm.Ir
import org.bytedeco.javacpp.LLVM.LLVMInt32Type
import java.io.File
import java.io.InputStream

class Compiler(file : String) {
    val parser = Parser.PegParser()
    var ir = Ir()

    init {
        val inputStream: InputStream = File(file).inputStream()
        val inputString = inputStream.bufferedReader().use { it.readText() }

        val finalInput = inputString.replace(Regex("#.*"), "")
        parser.setString(finalInput.replace(Regex("\n"), ""))
        val main = ir.createModule("main").addFunction(LLVMInt32Type(), "main", arrayOf())
        main.addBlocks("entry", "end")
    }

    fun compile() {
        try {
            val ast = parser.parse()
            if (ast.nodes.isEmpty())
                println("Syntax Error")
            else {
                println(ast.dump())
                val llvm = Api()
                ir = llvm.toIR(ast, ir, "Compiler")
//                ir.modules["main"]!!.functions["main"]!!.Blocks["entry"]!!.append("return", arrayOf("return", "0"))
                ir.print()
                ir.verify()
                ir.compile("a.out")
            }
        }
        catch(e : Exception) {
            System.err.println("Compilation Error.")
            e.printStackTrace()
        }
    }
}