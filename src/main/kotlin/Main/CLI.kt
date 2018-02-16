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
}