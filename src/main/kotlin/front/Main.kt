package front

import Llvm.Api

fun main(args: Array<String>) {
    val llvm = Api()
    llvm.grok(args)
    return
    try {
        FrontEnd(args)
    }
    catch (e : Exception) {
        System.err.println("Compilation Error:")
        e.printStackTrace()
    }
}