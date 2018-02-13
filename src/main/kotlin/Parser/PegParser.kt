package Parser

class PegParser(private var _str : String? = null) {

    fun setString(str : String) { _str = str }

    fun parse() : AST {
        if (_str == null)
            throw Exception(Messages.nullString)
        return AST(LocalDef(Prototype(Identifier("fun"), PrototypeArgs(Params(Identifier("i"), VarType("int")), FunType("int")))));
    }
}