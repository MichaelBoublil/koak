package Main

import java.io.File
import java.io.InputStream

class Compiler(file : String) {
    val parser = Parser.PegParser()

    init {
        val inputStream: InputStream = File(file).inputStream()
        val inputString = inputStream.bufferedReader().use { it.readText() }

        val finalInput = inputString.replace(Regex("#.*\n"), "")
        parser.setString(finalInput.replace(Regex("\n"), ""))
    }

    fun compile() {
        try {
            val ast = parser.parse()
            if (ast.nodes.isEmpty())
                println("Syntax Error")
            else
                println(ast.dump())
        }
        catch(e : Exception) {
            System.err.println("Compilation Error.")
//                e.printStackTrace()
        }
    }
}