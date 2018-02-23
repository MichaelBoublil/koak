package Llvm

import java.util.*
import kotlin.collections.HashMap
import kotlin.math.exp

enum class InstructionType {
    ERROR,
    CALL_FUNC,
    EXT_FUNC,
    ASSIGNMENT,
    CONDITION,
    DEF_FUNC,

    CALCULUS,
    COMPARE,

    VALUE,
    DEC_VALUE,
    DOUBLE_VALUE,
    OCT_VALUE,
    VARTYPE,
    FUNTYPE,
    PROTOTYPE,
    PARAM,
    PROTOARGS,
    BODY,
    EXPRESSION,

}

class Info(val type : InstructionType, val value : String = "default value", val attributes : MutableMap<String, Info> = mutableMapOf<String, Info>()) {
    fun dump() {
        println("Type : " + type.toString())
        println("Value : " + value)
        println("Children : ")
        for ((i, s) in attributes) {
            println(i)
            s.dump()
        }
        println("====================================")
    }

    fun interpret() {
    }
}