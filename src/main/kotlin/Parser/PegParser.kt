package Parser

class PegParser(private var _str : String? = null) {

    fun setString(str: String) {
        _str = str
    }

    fun parse(): AST {
        if (_str == null)
            throw Exception(Messages.nullString)
//        return AST(LocalDef(Prototype(Identifier("fun"), PrototypeArgs(Params(Identifier("x"), VarType("int")), FunType("int"))), Expressions(Expression(Unary(PostFix(Primary(Literal(DecimalConst("2"))))), BinOp("+", true), Unary(PostFix(Primary(Identifier("x"))))))));
        return AST()
    }
}
