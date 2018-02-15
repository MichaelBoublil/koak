package Main

class CLI {
    var parser = Parser.PegParser()

    fun run() {
        while (true) {
            val inputString = readLine() ?: break
            println(inputString)
            parser.setString(inputString.replace("\\s".toRegex(), ""))
            parser.parse()
        }
    }
}