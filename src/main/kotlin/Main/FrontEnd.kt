package Main

class FrontEnd(args : Array<String>) {
    private var cli = CLI()
    private var compiler : Compiler? = null

    init {
        if (args.isEmpty()) {
            cli.run()
        }
        else {
            val idx = args[0].indexOf(".koak")
            if (idx == -1 || idx + ".koak".length != args[0].length)
                throw Exception(Messages.wrongFileExtension)
            compiler = Main.Compiler(args[0])
            compiler!!.compile()
        }
    }
}