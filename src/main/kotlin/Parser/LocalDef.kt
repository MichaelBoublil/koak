package Parser

class LocalDef(vararg val children : INode) : INode() {
    override fun dump() {
        var str = "LocalDef("
        for (child in children) {
            str += child.dump()
            str += ", "
        }
        str += ")"
    }
}