package Parser

data class Exp(val s : String) : INode {
    override fun dump(): String {
        val className = this.javaClass
        var str = className.kotlin.toString()
        str += " " + s
        return str
    }
}