package Parser

enum class NodeType {
    UNDEFINED,
    EXT_DEF,
    LOCAL_DEF,
    TOP_EXPR,
    DEFS,
    PROTOTYPE,
    PROTOTYPE_ARGS,
    PARAMS,
    IDENTIFIER,
    WHILE_EXPR,
    FOR_EXPR,
    IF_EXPR,
    EXPRESSIONS,
    EXPRESSION,
    UNARY,
    POSTFIX,
    PRIMARY,
    CALL_EXPR,
    PAREN_EXPR,
    DOT,
    EXP,
    DECIMAL_CONST,
    HEXADECIMAL_PREFIX,
    HEXADECIMAL_DIGIT,
    HEXADECIMAL_CONST,
    OCTAL_DIGIT,
    OCTAL_CONST,
    DOUBLE_CONST,
    LITTERAL,
    VARTYPE,
    FUNTYPE,

    BINOP,
    LEFT_ASSOC,
    RIGHT_ASSOC,
    UNOP
}

open class NodeModel(type : NodeType = NodeType.UNDEFINED) {
    var type : NodeType = type
}