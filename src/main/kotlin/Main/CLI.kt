package Main

class CLI {
    var parser = Parser.PegParser()

    fun run() {
        while (true) {
            print("> ")
            val inputString = readLine() ?: break
            parser.setString(inputString.replace("\\s".toRegex(), ""))
            try {
                val ast = parser.parse()
                println(ast.dump())
            }
            catch(e : Exception) {
                println("Compilation Error.")
//                e.printStackTrace()
            }
        }
    }
}