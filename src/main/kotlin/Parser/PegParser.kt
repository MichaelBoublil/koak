package Parser

import com.sun.corba.se.impl.resolver.INSURLOperationImpl
import jdk.nashorn.internal.codegen.CompilerConstants

class PegParser(private var _str : String? = null) {

    fun setString(str: String) {
        _str = str
    }

    fun parse(): AST {
        if (_str == null)
            throw Exception(Messages.nullString)
       val lol = startParse()
        if (lol == null)
            return (AST())
        //        return AST(LocalDef(Prototype(Identifier("fun"), PrototypeArgs(Params(Identifier("x"), VarType("int")), FunType("int"))), Expressions(Expression(Unary(PostFix(Primary(Literal(DecimalConst("2"))))), BinOp("+", true), Unary(PostFix(Primary(Identifier("x"))))))));
        return AST(lol!!)
    }

    private fun startParse() : INode? {
        println("La string au debut : " + _str)
        val ret = isKdefs(_str)
        println("str a la fin : " + ret.first)
        if (ret.second != null) {
            println("pas null")
            return ret.second
        }

        return (null)
        //isIdentifier(_str)
    }

    private fun isKdefs(str: String?) : Pair<String?, INode?> {
        // TODO : ext def -> local def -> top_expr
        val ret = isTopExpr(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val node = ret.second!!
                Pair(ret.first, Defs(node))
            }
        }
    }

    private fun isTopExpr(str: String?): Pair<String?, INode?> {
        val ret = isExpressions(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                if (ret.first.isNullOrEmpty())
                    return Pair(str, null)

                return when (ret.first!!.first()) {
                    ';' -> {
                        val node = ret.second!!
                        val newStr = ret.first!!
                        Pair(newStr.drop(1), TopExpr(node))
                    }
                    else -> Pair(str, null)
                }
            }
        }
    }

    private fun isExpressions(str: String?): Pair<String?, INode?> {
        // TODO: for -> if -> while -> expression(: expression)*

        val ret = isExpression(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val node = ret.second!!
                Pair(ret.first, Expressions(node))
            }
        }
    }

    private fun isExpression(str: String?): Pair<String?, INode?> {
        // TODO: unary (#binop (#left_assoc unary / #right_assoc expression))*
        val ret = isUnary(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val node = ret.second!!
                Pair(ret.first, Expression(node))
            }
        }
    }

    private fun isUnary(str: String?): Pair<String?, INode?> {
        //TODO : #unop unary / postfix
        val ret = isPostFix(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val node = ret.second!!
                Pair(ret.first, Unary(node))
            }
        }
    }

    private fun isPostFix(str : String?) : Pair<String?, INode?> {
        val ret = isPrimary(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val ret1 = isCallExpr(ret.first)
                val node = ret.second!!
                return when (ret1.second) {
                    null -> {
                        Pair(ret.first, PostFix(node))
                    }
                    else -> {
                        val node2 = ret1.second!!
                        Pair(ret1.first, PostFix(node, node2))
                    }
                }

            }
        }
    }

    private fun isCallExpr(str: String?) : Pair<String?, INode?> {
        return when (str!!.first()) {
            '(' -> {
                val ret = isExpression(str.drop(1))
                return when (ret.second) {
                    null -> Pair(str, null)
                    else -> {
                        return when (ret.first!!.first()) {
                            ')' -> {
                                val node = ret.second!!
                                Pair(ret.first!!.drop(1), CallExpr(node))
                            }
                            else -> Pair(str, null)
                        }

                    }
                }
            }
            else -> Pair(str, null)
        }
    }

    private fun isPrimary(str: String?) : Pair<String?, INode?> {
        val ret = isIdentifier(str)
        return when (ret.second) {
            null -> {//Pair(str, null)
                val ret1 = isLiteral(str)
                return when (ret1.second) {
                    null -> Pair(str, null)
                    else -> {
                        val node = ret1.second!!
                        Pair(ret1.first, Primary(node))
                    }
                }
            }
            else -> {
                val node = ret.second!!
                Pair(ret.first, Primary(node))
            }
        }
    }

    private fun isLiteral(str: String?): Pair<String?, INode?> {
        val ret = isHexadecimalConst(str)
        return when (ret.second) {
            null -> {
                val ret1 = isOctalConst(str)
                return when (ret1.second) {
                    null -> {
                        val ret2 = isDecimalConst(str, "")
                        return when (ret2.second) {
                            null -> Pair(str, null)
                            else -> {
                                val node = ret2.second!!
                                Pair(ret2.first, Literal(node))
                            }
                        }
                    }
                    else -> {
                        val node = ret1.second!!
                        Pair(ret1.first, Literal(node))
                    }
                }
            }
            else -> {
                val node = ret.second!!
                Pair(ret.first, Literal(node))
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
                    else -> return (Pair(str, HexadecimalDigit("0x" + nb)))
                }
            }
        }
    }

    private fun isHexadecimalConst(str : String?) : Pair<String?, INode?> {
        return when (str!!.startsWith("0x", true)) {
            true -> {
                val ret = isHexadecimalDigit(str.drop(2), "")
                return when (ret.second) {
                    null -> Pair(ret.first, null)
                    else -> {
                        val node = ret.second!!
                        Pair(ret.first, HexadecimalConst(node))
                    }
                }
            }
            else -> Pair(str, null)
        }
    }

    private fun isOctalDigit(str : String?, nb : String) : Pair<String?, INode?> {
        return when (str!!.first() in '0'..'7') {
            true -> isOctalDigit(str.drop(1), nb + str.first())
            false -> {
                return when (nb.length) {
                    0 -> Pair(str, null)
                    else -> return (Pair(str, OctalDigit("0" + nb)))
                }
            }
        }
    }

    private fun isOctalConst(str : String?) : Pair<String?, INode?> {
        return when (str!!.first()) {
            '0' -> {
                val ret = isOctalDigit(str.drop(1), "")
                return when (ret.second) {
                    null -> Pair(ret.first, null)
                    else -> {
                        val node = ret.second!!
                        Pair(ret.first, OctalConst(node))
                    }
                }
            }
            else -> Pair(str, null)
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
