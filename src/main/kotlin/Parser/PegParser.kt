package Parser

class PegParser(private var _str : String? = null) {

    fun setString(str : String) { _str = str }

    fun Parse() : AST {
        if (_str == null)
            throw Exception(Messages.nullString)
        return AST()
    }
}