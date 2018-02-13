package Parser

class AST(vararg val nodes : INode) {
    fun dump() : String {
        
        var str = this.javaClass.simpleName + "("
        for (child in nodes) {
            str += child.dump()
            str += ", "
        }
        str += "\b\b)"

        return str
    }
//    var nodes : Array<INode> = arrayOf()
}