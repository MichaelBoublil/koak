package Llvm

import kotlin.math.exp

enum class InstructionType {
    ERROR,
    CALL_FUNC,
    ASSIGNMENT,
    ADD,
    VALUE,
    DEC_VALUE,
    DOUBLE_VALUE,
    OCT_VALUE
}

class Info(val type : InstructionType, val value : String = "default value", vararg val expressions : Info) {
    fun dump() {
        println("Type : " + type.toString())
        println("Value : " + value)
        println("Children : ")
        for (child in expressions) {
            child.dump()
        }
    }

    fun interpret() {
    }
}