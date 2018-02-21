package Main

import Llvm.Api

fun main(args: Array<String>) {
    try {
        FrontEnd(args)
    }
    catch (e : Exception) {
        System.err.println("Compilation Error:")
        e.printStackTrace()
    }
}