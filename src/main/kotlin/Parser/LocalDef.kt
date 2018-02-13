package Parser

class LocalDef(vararg val children : INode) : INode {
    override fun dump() {
        var className = this.javaClass
        var str = className.kotlin.toString()
        for (child in children) {
            str += child.dump()
            str += ", "
        }
        str += ")"
    }
}