package Parser

class BinOp(val s : String, val isRightAssoc : Boolean, vararg var children : INode) : INode {
    override fun dump(): String {
        
        var str = this.javaClass.simpleName + "("

        str += s + "; rightAssoc = " + isRightAssoc.toString() + "; "

        for (child in children) {
            str += child.dump()
            str += ", "
        }
        str += "\b\b"
        str += ")"
        return str
    }

    fun setChildrentoAdd(vararg childrenToAdd : INode) {
        children = childrenToAdd
    }
}