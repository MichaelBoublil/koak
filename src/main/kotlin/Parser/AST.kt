package Parser

class AST(vararg val nodes : INode) {
    fun dump() : String {
        val className = this.javaClass
        var str = className.kotlin.toString() + "("
        for (child in nodes) {
            str += child.dump()
            str += ", "
        }
        str += "\b\b)"

        return str
    }
//    var nodes : Array<INode> = arrayOf()
}