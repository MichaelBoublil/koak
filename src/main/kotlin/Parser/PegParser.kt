package Parser

import com.sun.corba.se.impl.resolver.INSURLOperationImpl

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
        println("La string au debut : " + _str)
        val ret = isPostFix(_str)
        println("str a la fin : " + ret.first)
        if (ret.second != null) {
            println("pas null")
        }

        //isIdentifier(_str)
    }

    private fun isPostFix(str : String?) : Pair<String?, INode?> {
        val ret = isPrimary(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val ret1 = isCallExpr(ret.first)
                val node = ret.second!!
                Pair(ret.first, PostFix(node))
            }
        }
    }

    private fun isCallExpr(str: String?) : Pair<String?, INode?> {
        return Pair(str, null)
    }

    private fun isPrimary(str: String?) : Pair<String?, INode?> {
        val ret = isIdentifier(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val node = ret.second!!
                Pair(ret.first, Primary(node))
            }
        }
    }

    private fun isHexadecimalDigit(str : String?, nb : String) : Pair<String?, INode?> {
        return when (str!!.first() in '0'..'9' ||
                     str.first() in 'a'..'f' ||
                     str.first() in 'A'..'F') {
            true -> isHexadecimalDigit(str.drop(1), nb + str.first())
            false -> {
                return when (nb.length) {
                    0 -> Pair(str, null)
                    else -> return (Pair(str, HexadecimalDigit(nb)))
                }
            }
        }
    }

    private fun isOctalDigit(str : String?, nb : String) : Pair<String?, INode?> {
        return when (str!!.first() in '0'..'7') {
            true -> isOctalDigit(str.drop(1), nb + str.first())
            false -> {
                return when (nb.length) {
                    0 -> Pair(str, null)
                    else -> return (Pair(str, OctalDigit(nb)))
                }
            }
        }
    }

    private fun isDecimalConst(str : String?, nb : String) : Pair<String?, INode?> {
        return when (str!!.first().isDigit()) {
            true -> isDecimalConst(str.drop(1), nb + str.first())
            false -> {
                return when (nb.length) {
                    0 -> Pair(str, null)
                    else -> return (Pair(str, DecimalConst(nb)))
                }
            }
        }
    }

    private fun isIdentifier(str : String?) : Pair<String?, INode?> {
        if (str!!.first().isLetter()) {
            return finishIdentifier(str.drop(1), "" + str.first())
        }
        return Pair(str, null)
    }

    private fun finishIdentifier(str : String?, identifier : String) : Pair<String?, INode?> {
        return when (str!!.first().isLetterOrDigit()) {
            true -> finishIdentifier(str.drop(1), identifier + str.first())
            false -> {
                return when (identifier.length) {
                    0 -> Pair(str, null)
                    else -> return (Pair(str, Identifier(identifier)))
                }
            }
        }
    }
}
