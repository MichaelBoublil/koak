package Main

import Llvm.Api

fun main(args: Array<String>) {
    val llvm = Api()
    llvm.grok(arrayOf("5"))
    return
    try {
        FrontEnd(args)
    }
    catch (e : Exception) {
        System.err.println("Compilation Error:")
        e.printStackTrace()
    }
}