package Main

import Llvm.Api

fun main(args: Array<String>) {
    val llvm = Api()
    val t = llvm.test()
    llvm.grok(args)
    if (t.state) {
        println("Success")
        println(t.content)
    }
    else {
        println("Failure")
        println(t.content)
    }
    return
    try {
        FrontEnd(args)
    }
    catch (e : Exception) {
        System.err.println("Compilation Error:")
        e.printStackTrace()
    }
}