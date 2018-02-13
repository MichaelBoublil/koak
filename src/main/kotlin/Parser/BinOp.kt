package Parser

data class BinOp(val s : String, val isLeftAssoc : Boolean) : INode {
    override fun dump(): String {
        
        var str = this.javaClass.simpleName + "("

        str += s + "; leftAssoc = " + isLeftAssoc.toString() + ")"
        return str
    }
}