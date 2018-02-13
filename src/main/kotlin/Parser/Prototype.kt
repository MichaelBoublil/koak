package Parser

class Prototype(vararg val children : INode) : INode {
    override fun dump(): String {
        
        var str = this.javaClass.simpleName + "("
        for (child in children) {
            str += child.dump()
            str += ", "
        }
        str += "\b\b"
        str += ")"
        return str
    }
}