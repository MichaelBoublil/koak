package Main

import java.io.File
import java.io.InputStream

class Compiler(file : String) {
    val parser = Parser.PegParser()

    init {
        val inputStream: InputStream = File(file).inputStream()
        val inputString = inputStream.bufferedReader().use { it.readText() }

        parser.setString(inputString)
    }
}