package Parser

class PegParser(private var _str : String? = null) {

    fun setString(str: String) {
        _str = str
    }

    fun parse(): AST {
        if (_str == null)
            throw Exception(Messages.nullString)
        startParse()
//        return AST(LocalDef(Prototype(Identifier("fun"), PrototypeArgs(Params(Identifier("x"), VarType("int")), FunType("int"))), Expressions(Expression(Unary(PostFix(Primary(Literal(DecimalConst("2"))))), BinOp("+", true), Unary(PostFix(Primary(Identifier("x"))))))));
        return AST()
    }

    private fun startParse() {
        isIdentifier(_str)
    }

    private fun isIdentifier(str : String?) : Pair<String?, AST?> {
        if (str!!.first().isLetter()) {
             return finishIdentifier(str.drop(1))
        }
        return Pair(str, null)
    }

    private fun finishIdentifier(str : String?) : Pair<String?, AST?> {
        println(str)

        return Pair(str, null)
    }
}
