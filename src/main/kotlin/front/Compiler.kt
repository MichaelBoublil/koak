package front

import Llvm.Api
import Llvm.Ir
import Parser.AST
import org.bytedeco.javacpp.LLVM.LLVMInt32Type
import java.io.File
import java.io.InputStream

class Compiler(var file : String = "") {
    val parser = Parser.PegParser()
    var ir = Ir()
    var isEmpty = false

    init {
        createParser()

        val main = ir.createModule("main").addFunction(LLVMInt32Type(), "main", arrayOf())
        main.addBlocks("entry", "end")
    }

    fun setFileName(fileName : String) {
        file = fileName
    }

    fun createParser() {
        if (file.isNotEmpty()) {
            val inputStream: InputStream = File(file).inputStream()
            val inputString = inputStream.bufferedReader().use { it.readText() }

            val finalInput = inputString.replace(Regex("#.*"), "")
            val finallInput = finalInput.replace(Regex("\n"), "")
            if (finallInput.replace(Regex("[ \t]"), "").isEmpty())
                isEmpty = true
            parser.setString(finallInput)
        }
    }

    fun compile() {
        try {
            val ast = if (!isEmpty)
                parser.parse()
            else
                AST()
            if (ast.nodes.isEmpty() && !isEmpty)
                println("Syntax Error")
            else {
                val llvm = Api()
                ir = llvm.toIR(ast, ir, "Compiler")
                ir.print()
                ir.verify()
                ir.jit("main")[0].compileToFile()
            }
        }
        catch(e : Exception) {
            System.err.println("Compilation Error.")
            e.printStackTrace()
        }
    }
}