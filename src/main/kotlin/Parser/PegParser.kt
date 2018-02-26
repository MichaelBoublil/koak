package Parser

import com.sun.corba.se.impl.resolver.INSURLOperationImpl
import com.sun.org.apache.xml.internal.dtm.ref.sax2dtm.SAX2DTM2
import jdk.nashorn.internal.codegen.CompilerConstants
import java.lang.System.out
import java.util.regex.Pattern

class PegParser(private var _str : String? = null) {

    fun setString(str: String) {
        _str = str
    }

    fun parse(): AST {
        if (_str == null)
            throw Exception(Messages.nullString)
       val res = startParse(_str!!, emptyList())
        if (res.second == null || res.first!!.isNotEmpty())
            return (AST())
        return AST(*res.second!!.toTypedArray())
    }

    private fun startParse(str: String, list: List<INode>) : Pair<String?, List<INode>?> {
        val ret = isKdefs(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val str1 = epurSpace(ret.first!!)
                return when (str1.isNotEmpty()) {
                    true -> startParse(str1, list + ret.second!!)
                    false -> Pair(str1, list + ret.second!!)
                }
            }
        }
    }

    private fun isKdefs(str: String?) : Pair<String?, INode?> {
        val ret = isExtDef(str)
        return when (ret.second) {
            null -> {
                val ret1 = isLocalDef(str)
                return when (ret1.second) {
                    null -> {
                       val ret2 = isTopExpr(str)
                        return when (ret2.second) {
                            null -> Pair(str, null)
                            else -> Pair(ret2.first, KDefs(ret2.second!!))
                        }
                    }
                    else -> Pair(ret1.first, KDefs(ret1.second!!))
                }
            }
            else -> Pair(ret.first, KDefs(ret.second!!))
            
        }
    }

    private fun isExtDef(str: String?): Pair<String?, INode?> {
        return when (str!!.contains(Regex("^extern[ \t]+"))) {
            true -> {
                val ret = isPrototype(str.drop(6))
                return when (ret.second) {
                    null -> Pair(str, null)
                    else -> {
                        return when (ret.first!!.first()) {
                            ';' -> Pair(ret.first!!.drop(1), ExtDef(ret.second!!))
                            else -> Pair(str, null)
                        }
                    }
                }
            }
            false -> Pair(str, null)
        }
    }

    private fun isSpacing(str: String?) : Boolean {
        return (str!!.contains(Regex("^[ \t]+")))
    }

    private fun epurSpace(str: String) : String {
        return (str.replace(Regex("^[ \t]*"), ""))
    }

    private fun isLocalDef(str: String?): Pair<String?, INode?> {
        return when (str!!.startsWith("def")) {
            true -> {
                val str1 = str.drop(3);
                return when (isSpacing(str1)) {
                    false -> Pair(str, null)
                    true -> {
                        val ret = isDefs(str1.replace(Regex("^[ \t]+"), ""))
                        return when (ret.second) {
                            null -> Pair(str, null)
                            else -> {
                                return when (ret.first!!.first()) {
                                    ';' -> Pair(ret.first!!.drop(1), LocalDef(ret.second!!))
                                    else -> Pair(str, null)
                                }
                            }
                        }
                    }
                }
            }
            false -> Pair(str, null)
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

    private fun isDefs(str: String): Pair<String?, INode?> {
        val ret = isPrototype(str)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val ret1 = isExpressions(epurSpace(ret.first!!))
                return when (ret1.second) {
                    null -> Pair(str, null)
                    else -> Pair(ret1.first, Defs(ret.second!!, ret1.second!!))
                }
            }
        }
    }

    private fun isPrototype(str: String): Pair<String?, INode?> {
        //TODO : ('unary' . decimal_const? / 'binary' . decimal_const? / identifier) prototype_args
        val str1 : String = epurSpace(str)

        when (str1.startsWith("unary")) {
            true -> {
                val ope = str1.drop(5).first()
                val retDec = isDecimalConst(str1.drop(6), "")
                when (retDec.second) {
                    null -> {
                        val ret1 = isProtoArgs(str1.drop(6))
                        return when (ret1.second) {
                            null -> Pair(str, null)
                            else -> Pair(ret1.first, PrototypeArgs(Identifier("unary" + ope), ret1.second!!))
                        }
                    }
                    else -> {
                        val ret1 = isProtoArgs(retDec.first)
                        return when (ret1.second) {
                            null -> Pair(str, null)
                            else -> Pair(ret1.first, PrototypeArgs(Identifier("unary" + ope), retDec.second!!, ret1.second!!))
                        }

                    }
                }
            }
            else -> {
                when (str1.startsWith("binary")) {
                    true -> {
                        val ope = str1.drop(6).first()
                        val retDec = isDecimalConst(str1.drop(7), "")
                        when (retDec.second) {
                            null -> {
                                val ret1 = isProtoArgs(str1.drop(7))
                                return when (ret1.second) {
                                    null -> Pair(str, null)
                                    else -> Pair(ret1.first, PrototypeArgs(Identifier("binary" + ope), ret1.second!!))
                                }
                            }
                            else -> {
                                val ret1 = isProtoArgs(retDec.first)
                                return when (ret1.second) {
                                    null -> Pair(str, null)
                                    else -> Pair(ret1.first, PrototypeArgs(Identifier("binary" + ope), retDec.second!!, ret1.second!!))
                                }

                            }
                        }
                    }
                    else -> {
                        val ret = isIdentifier(str1)
                        return when (ret.second) {
                            null -> Pair(str, null)
                            else -> {
                                val ret1 = isProtoArgs(ret.first)
                                return when (ret1.second) {
                                    null -> Pair(str, null)
                                    else -> Pair(ret1.first, Prototype(ret.second!!, ret1.second!!))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ProtoArgsRec(str: String?, list: List<INode>) : Pair<String?, List<INode>?> {
        val ret = isIdentifier(str)
        when (ret.second) {
            null -> {
                return when (epurSpace(str!!).first()) {
                    ')' -> Pair(epurSpace(str).drop(1), list)
                    else -> Pair(str, null)
                }
            }
            else -> {
                val str1 = epurSpace(ret.first!!)
                return when (str1.first()) {
                    ':' -> {
                        val ret1 = isVarType(epurSpace(str1.drop(1)))
                        return when (ret1.second) {
                            null -> Pair(str, null)
                            else -> ProtoArgsRec(ret1.first, list + Args(ret.second!!, ret1.second!!))
                        }
                    }
                    else -> ProtoArgsRec(ret.first, list + Args(ret.second!!, VarType("unknown")))
                }
            }
        }
    }

    private fun isProtoArgs(str: String?): Pair<String?, INode?> {
        val str1 = epurSpace(str!!)
        return when (str1.first()) {
            '(' -> {
                val (next, list) = ProtoArgsRec(str1.drop(1), emptyList())
                return when (list) {
                    null -> Pair(str, null)
                    else -> {
                        val next1 = epurSpace(next!!)
                        return when (next1.first()) {
                            ':' -> {
                                val ret1 = isFuncType(epurSpace(next1.drop(1)))
                                return when (ret1.second) {
                                    null -> Pair(str, null)
                                    else -> Pair(ret1.first, PrototypeArgs(*list.toTypedArray(), ret1.second!!))
                                }
                            }
                            else -> Pair(next, PrototypeArgs(*list.toTypedArray(), FunType("void")))
                        }
                    }
                }
            }
            else -> Pair(str, null)
        }
    }

    private fun isType(str: String?) : Pair<String, Boolean> {
        return when (str!!.startsWith("int")) {
            true -> Pair("int", true)
            false -> {
                return when (str!!.startsWith("double")) {
                    true -> Pair("double", true)
                    false -> {
                        return when (str!!.startsWith("void")) {
                            true -> Pair("void", true)
                            false -> Pair("", false)
                        }
                    }
                }
            }
        }
    }

    private fun isFuncType(str: String?): Pair<String?, INode?> {
        val ret = isType(str)
        return when (ret.second){
            true -> Pair(str!!.drop(ret.first.length), FunType(ret.first))
            false -> Pair(str, null)
        }
    }

    private fun isVarType(str: String?): Pair<String?, INode?> {
        val ret = isType(str)
        return when (ret.second){
            true -> Pair(str!!.drop(ret.first.length), VarType(ret.first))
            false -> Pair(str, null)
        }
    }

    private fun expressionRec(str: String?, list: List<INode>) : Pair<String?, List<INode>?> {
        val ret = isExpression(str)
        return when (ret.second) {
            null -> Pair(str, list)
            else -> {
                return when (ret.first!!.contains(Regex("^[ \t]*:[ \t]*"))) {
                    true -> {
                        val next1 = ret.first!!.replace(Regex("^[ \t]*:[ \t]*"), "")
                        expressionRec(next1, list + ret.second!!)
                    }
                    false -> Pair(ret.first, list + ret.second!!)
                }
            }
        }
    }

    private fun isExpressions(str: String?): Pair<String?, INode?> {
        // TODO: for -> if -> while -> expression(: expression)*
        val str1 = epurSpace(str!!)
        val ret = isForExpr(str1)
        return when (ret.second) {
            null -> {
                val ret1 = isIfExpr(str1)
                return when (ret1.second) {
                    null -> {
                        val ret2 = isWhileExpr(str1)
                        return when (ret2.second) {
                            null -> {
                                val ret3 = expressionRec(str1, emptyList())
                                return when (ret3.second) {
                                    emptyList<INode>() -> Pair(str, null)
                                    else -> Pair(ret3.first, Expressions(*ret3.second!!.toTypedArray()))
                                }
                            }
                            else -> Pair(ret2.first, Expressions(ret2.second!!))
                        }
                    }
                    else -> Pair(ret1.first, Expressions(ret1.second!!))
                }
            }
            else -> {
                Pair(ret.first, Expressions(ret.second!!))
            }
        }


    }

    private fun isIfExpr(str: String?): Pair<String?, INode?> {
        return when (str!!.contains(Regex("^if[ \t]+"))) {
            true -> {
                val ret = isExpression(str.drop(2))
                return when (ret.second) {
                    null -> Pair(str, null)
                    else -> {
                        val str1 = epurSpace(ret.first!!)
                        return when (str1.contains(Regex("^then[ \t]+"))) {
                            true -> {
                                val ret1 = isExpressions(str1.drop(4))
                                return when (ret1.second) {
                                    null -> Pair(str, null)
                                    else -> {
                                        val str2 = epurSpace(ret1.first!!)
                                        return when (str2.contains(Regex("^else[ \t]+"))) {
                                            true -> {
                                                val ret2 = isExpressions(str2.drop(4))
                                                return when (ret2.second) {
                                                    null -> Pair(str, null)
                                                    else -> Pair(ret2.first, IfExpr(ret.second!!, ret1.second!!, ret2.second!!))
                                                }
                                            }
                                            false -> Pair(ret1.first, IfExpr(ret.second!!, ret1.second!!))
                                        }
                                    }
                                }
                            }
                            false -> Pair(str, null)
                        }
                    }
                }
            }
            false -> Pair(str, null)
        }
    }

    private fun isWhileExpr(str: String?): Pair<String?, INode?> {
        return when (str!!.contains(Regex("^while[ \t]+"))) {
            true -> {
                val ret = isExpression(str.drop(5))
                return when (ret.second) {
                    null -> Pair(str, null)
                    else -> {
                        val str1 = epurSpace(ret.first!!)
                        return when (str1.contains(Regex("^do[ \t]+"))) {
                            true -> {
                                val ret1 = isExpressions(str1.drop(2))
                                return when (ret1.second) {
                                    null -> Pair(str, null)
                                    else -> Pair(ret1.first, WhileExpr(ret.second!!, ret1.second!!))
                                }
                            }
                            false -> Pair(str, null)
                        }
                    }
                }
            }
            false -> Pair(str, null)
        }
    }

    private fun isForExpr(str: String?): Pair<String?, INode?> {
        return when (str!!.contains(Regex("^for[ \t]+"))) {
            true -> {
                val ret = isIdentifier(str.drop(3))
                return when (ret.second) {
                    null -> Pair(str, null)
                    else -> {
                        val str1 = epurSpace(ret.first!!)
                        return when (str1.first()) {
                            '=' -> {
                                val ret1 = isExpression(str1.drop(1))
                                return when (ret1.second) {
                                    null -> Pair(str, null)
                                    else -> {
                                        val str2 = epurSpace(ret1.first!!)
                                        return when (str2.first()) {
                                            ',' -> {
                                                val ret2 = isIdentifier(str2.drop(1))
                                                return when (ret2.second) {
                                                    null -> Pair(str, null)
                                                    else -> {
                                                        val str3 = epurSpace(ret2.first!!)
                                                        return when (str3.first()) {
                                                            '<' -> {
                                                                val ret3 = isExpression(str3.drop(1))
                                                                return when (ret3.second) {
                                                                    null -> Pair(str, null)
                                                                    else -> {
                                                                        val str4 = epurSpace(ret3.first!!)
                                                                        return when (str4.first()) {
                                                                            ',' -> {
                                                                                val ret4 = isExpression(str4.drop(1))
                                                                                return when (ret4.second) {
                                                                                    null -> Pair(str, null)
                                                                                    else -> {
                                                                                        val str5 = epurSpace(ret4.first!!)
                                                                                        return when (str5.contains(Regex("in[ \t]+"))) {
                                                                                            true -> {
                                                                                                val ret5 = isExpressions(str5.drop(2))
                                                                                                return when (ret5.second) {
                                                                                                    null -> Pair(str, null)
                                                                                                    else -> Pair(ret5.first, ForExpr(ret.second!!,
                                                                                                            ret1.second!!, ret2.second!!,
                                                                                                            ret3.second!!, ret4.second!!,
                                                                                                            ret5.second!!))
                                                                                                }
                                                                                            }
                                                                                            false -> Pair(str, null)
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            else -> Pair(str, null)
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            else -> Pair(str, null)
                                                        }
                                                    }
                                                }
                                            }
                                            else -> Pair(str, null)
                                        }
                                    }
                                }
                            }
                            else -> Pair(str, null)
                        }
                    }
                }
            }
            false -> Pair(str, null)
        }
    }

    private fun expressionRec(str: String?, lvalue : INode) : Pair<String?, INode?> {
        val str1 = epurSpace(str!!)
        val ret1 = isBinop(str1)
        when (ret1.second) {
            null -> return (Pair(str, lvalue))
            else -> {
                val assoc: BinOp = ret1.second as BinOp
                val ret2 = if (assoc.isRightAssoc)
                    isExpression(ret1.first)
                else
                    isUnary(epurSpace(ret1.first!!))
                return when (ret2.second) {
                    null -> return (Pair(str, null))
                    else -> {
                        assoc.setChildrentoAdd(lvalue, ret2.second!!)
                        expressionRec(ret2.first, assoc)
                    }
                }
            }
        }

    }

    private fun isExpression(str: String?): Pair<String?, INode?> {
        val str1 = epurSpace(str!!)
        val ret = isUnary(str1)
        return when (ret.second) {
            null -> Pair(str, null)
            else -> {
                val ret1 = expressionRec(ret.first, ret.second!!)
                Pair(ret1.first, Expression(ret1.second!!))
            }
        }
    }

    private fun isBinop(str: String?) : Pair<String?, INode?> {
        return when (str!!.first()) {
            '=' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), false))
                    else -> Pair(str.drop(1), BinOp("=", true))
                }
            }
            '+' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), true))
                    else -> Pair(str.drop(1), BinOp("+", false))
                }
            }
            '-' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), true))
                    else -> Pair(str.drop(1), BinOp("-", false))
                }
            }
            '*' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), true))
                    else -> Pair(str.drop(1), BinOp("*", false))
                }
            }
            '/' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), true))
                    else -> Pair(str.drop(1), BinOp("/", false))
                }
            }
            '%' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), true))
                    else -> Pair(str.drop(1), BinOp("%", false))
                }
            }
            '&' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), true))
                    '&' -> Pair(str.drop(2), BinOp(str.substring(0, 2), false))
                    else -> Pair(str.drop(1), BinOp("&", false))
                }
            }
            '^' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), true))
                    else -> Pair(str.drop(1), BinOp("^", false))
                }
            }
            '!' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), false))
                    else -> Pair(str, null)
                }
            }
            '|' -> {
                when (str.drop(1).first()) {
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), true))
                    '|' -> Pair(str.drop(2), BinOp(str.substring(0, 2), false))
                    else -> Pair(str.drop(1), BinOp("|", false))
                }
            }
            '<' -> {
                when (str.drop(1).first()) {
                    '<' -> {
                        when (str.drop(2).first()) {
                            '=' -> Pair(str.drop(3), BinOp(str.substring(0, 3), true))
                            else -> Pair(str.drop(2), BinOp(str.substring(0, 2), false))
                        }
                    }
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), false))
                    else -> Pair(str.drop(1), BinOp("<", false))
                }
            }
            '>' -> {
                when (str.drop(1).first()) {
                    '>' -> {
                        when (str.drop(2).first()) {
                            '=' -> Pair(str.drop(3), BinOp(str.substring(0, 3), true))
                            else -> Pair(str.drop(2), BinOp(str.substring(0, 2), false))
                        }
                    }
                    '=' -> Pair(str.drop(2), BinOp(str.substring(0, 2), false))
                    else -> Pair(str.drop(1), BinOp(">", false))
                }
            }
            else -> Pair(str, null)
        }
    }

    private fun isUnary(str: String?): Pair<String?, INode?> {
        val ret = isUnop(str)
        when (ret.second) {
            null -> {
                val ret2 = isPostFix(str)
                return when (ret2.second) {
                    null -> Pair(str, null)
                    else -> {
                        val node = ret2.second!!
                        Pair(ret2.first, Unary(node))
                    }
                }
            }
            else -> {
                val ret1 = isUnary(ret.first)
                return when (ret1.second) {
                    null -> Pair(str, null)
                    else -> Pair(ret1.first, Unary(ret.second!!, ret1.second!!))
                }
            }
        }
    }

    private fun isUnop(str: String?): Pair<String?, INode?> {
        return when (str!!.first()) {
            in "!~+-" -> Pair(str.drop(1), UnOp(str.first().toString()))
            else -> Pair(str, null)
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

    private fun callExprRec(str: String?, list : List<INode>) : Pair<String?, List<INode>?> {
        val str1 = epurSpace(str!!)
        val ret = isExpression(str1)
        return when (ret.second) {
            null -> {
                when (str1.first()) {
                    ')' -> Pair<String?, List<INode>?>(str1.drop(1), emptyList())
                    else -> Pair(str, null)
                }
            }
            else -> {
                val str2 = epurSpace(ret.first!!)
                return when (str2.first()) {
                    ',' -> callExprRec(str2.drop(1), list + ret.second!!)
                    ')' -> Pair(str2.drop(1), list + ret.second!!)
                    else -> Pair(str, null)
                }
            }
        }
    }

    private fun isCallExpr(str: String?) : Pair<String?, INode?> {
        return when (str!!.first()) {
            '(' -> {
                val (next, list) = callExprRec(str.drop(1), emptyList())
                return when (list) {
                    null -> Pair(str, null)
                    emptyList<INode>() -> Pair(next, CallExpr())
                    else -> Pair(next, CallExpr(*(list.toTypedArray())))
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
                    null -> {
                        val ret2 = isParenExpr(str)
                        return when (ret2.second) {
                            null -> Pair(str, null)
                            else -> Pair(ret2.first, Primary(ret2.second!!))
                        }
                    }
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

    private fun isParenExpr(str: String?): Pair<String?, INode?> {
        return when (str!!.first()) {
            '(' -> {
                val ret = isExpressions(str.drop(1))
                return when (ret.second) {
                    null -> Pair(str, null)
                    else -> {
                        val str1 = epurSpace(ret.first!!)
                        return when (str1.first()) {
                            ')' -> Pair(str1.drop(1), ParenExpr(ret.second!!))
                            else -> Pair(str, null)
                        }
                    }
                }
            }
            else -> Pair(str, null)
        }
    }

    private fun isLiteral(str: String?): Pair<String?, INode?> {
        val ret = isHexadecimalConst(str)
        return when (ret.second) {
            null -> {
                val ret1 = isOctalConst(str)
                return when (ret1.second) {
                    null -> {
                        val ret2 = isDoubleConst(str)
                        return when (ret2.second) {
                            null -> {
                                val ret3 = isDecimalConst(str, "")
                                return when (ret3.second) {
                                    null -> Pair(str, null)
                                    else -> Pair(ret3.first, Literal(ret3.second!!))
                                }
                            }
                            else -> Pair(ret2.first, Literal(ret2.second!!))
                        }
                    }
                    else -> Pair(ret1.first, Literal(ret1.second!!))
                }
            }
            else -> Pair(ret.first, ret.second!!)
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

    private fun isDot(str: String?) : Pair<String?, INode?> {
        return when (str!!.first()) {
            '.' -> {
                return when (str.drop(1).first()) {
                    '.' -> Pair(str, null)
                    else -> Pair(str.drop(1), Dot(str.first().toString()))
                }
            }
            else -> Pair(str, null)
        }
    }

    private fun isExp(str: String?) : Pair<String?, INode?> {
        return when (str!!.first().toUpperCase()) {
            'E' -> {
                when (str.drop(1).first()) {
                    '+' -> {
                        val ret = isDecimalConst(str.drop(2), "")
                        val dec = ret.second as DecimalConst
                        return when (ret.second) {
                            null -> Pair(str, null)
                            else -> Pair(ret.first, Exp("e+" + dec.s))
                        }
                    }
                    '-' -> {
                        val ret = isDecimalConst(str.drop(2), "")
                        val dec = ret.second as DecimalConst
                        return when (ret.second) {
                            null -> Pair(str, null)
                            else -> Pair(ret.first, Exp("-" + dec.s))
                        }
                    }
                    else -> {
                        val ret = isDecimalConst(str.drop(1), "")
                        val dec = ret.second as DecimalConst
                        return when (ret.second) {
                            null -> Pair(str, null)
                            else -> Pair(ret.first, Exp("+" + dec.s))
                        }
                    }
                }
            }
            else -> Pair(str, null)
        }
    }

    private fun isDoubleConst(str: String?) : Pair<String?, INode?> {
        val ret = isDecimalConst(str, "")
        when (ret.second) {
            null -> {
                val ret1 = isDot(str)
                return when (ret1.second) {
                    null -> Pair(str, null)
                    else -> {
                        val ret2 = isDecimalConst(ret1.first, "")
                        return when (ret2.second) {
                            null -> Pair(str, null)
                            else -> {
                                val ret5 = isExp(ret2.first)
                                return when (ret5.second) {
                                    null -> Pair(ret2.first, DoubleConst(ret1.second!!, ret2.second!!))
                                    else -> Pair(ret5.first, DoubleConst(ret1.second!!, ret2.second!!, ret5.second!!))
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                val ret3 = isDot(ret.first)
                return when (ret3.second) {
                    null -> Pair(str, null)
                    else -> {
                        val ret4 = isDecimalConst(ret3.first, "")
                        when (ret4.second) {
                            null -> {
                                val ret6 = isExp(ret3.first)
                                return when (ret6.second) {
                                    null -> Pair(ret3.first, DoubleConst(ret.second!!, ret3.second!!))
                                    else -> Pair(ret6.first, DoubleConst(ret.second!!, ret3.second!!, ret6.second!!))
                                }
                            }
                            else -> {
                                val ret6 = isExp(ret4.first)
                                return when (ret6.second) {
                                    null -> Pair(ret4.first, DoubleConst(ret.second!!, ret3.second!!, ret4.second!!))
                                    else -> Pair(ret6.first, DoubleConst(ret.second!!, ret3.second!!, ret4.second!!, ret6.second!!))
                                }
                            }
                        }
                    }
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
        val str1 = epurSpace(str!!)
        if (str1.first().isLetter()) {
            return finishIdentifier(str1.drop(1), "" + str1.first())
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
