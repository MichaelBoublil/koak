package Parser

data class HexadecimalDigit(val s : String) : INode {
    override fun dump(): String {
        
        var str = this.javaClass.simpleName + "("
        str += s + ")"
        return str
    }
}