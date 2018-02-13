package Parser

class Expressions(vararg val children : INode) : INode {
    override fun dump(): String {
        val className = this.javaClass
        var str = className.kotlin.toString() + "("
        for (child in children) {
            str += child.dump()
            str += ", "
        }
        str += "\b\b"
        str += ")"
        return str
    }
}