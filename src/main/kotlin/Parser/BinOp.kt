package Parser

data class BinOp(val x : String) : INode {
    override fun dump(): String {
        val className = this.javaClass
        val str = className.kotlin.toString()
        return str
    }
}