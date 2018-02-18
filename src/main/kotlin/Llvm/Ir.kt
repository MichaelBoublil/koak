package Llvm

import org.bytedeco.javacpp.LLVM.*

object Builder
{
    val llvm = LLVMCreateBuilder()
}

class Ir
{
    enum class Instructions
    {
        Condition,
        VarDec,
        VarSet,
        If,
        Else,
        Return
    }

    abstract class Instruction constructor(val type: Instructions)
    {

    }

    class Condition : Instruction(Instructions.Condition)
    {

    }

    // An instruction is an LLVM Block
    class Block constructor (val func: Function, val identifier : String)
    {
        val _blockLlvm : LLVMBasicBlockRef = LLVMAppendBasicBlock(func._funLlvm, identifier)
        val _content : MutableMap<String, LLVMValueRef> = mutableMapOf()

        lateinit var _cond : LLVMValueRef
        val factory : MutableMap<String, (String, Array<String>) -> Boolean> = mutableMapOf()

        private fun placeEditorAtMe()
        {
            LLVMPositionBuilderAtEnd(Builder.llvm, _blockLlvm)
        }

        init {
            factory["compare ints"] =
                    fun(identifier: String, args: Array<String>) : Boolean {
                        if (args.size < 3)
                            return false
                        var first : LLVMValueRef
                        var second : LLVMValueRef
                        if (func.getParam(args[1]) != null)
                            first = func.getParam(args[1])!!
                        else
                            first = LLVMConstInt(LLVMInt32Type(), args[1].toLong(), 0)
                        if (func.getParam(args[2]) != null)
                            second = func.getParam(args[2])!!
                        else
                            second = LLVMConstInt(LLVMInt32Type(), args[2].toLong(), 0)

                        placeEditorAtMe()
                        _content[identifier] = LLVMBuildICmp(Builder.llvm, LLVMIntEQ, first, second, identifier)
                        return true
                    }
            factory["conditional jump"] =
                    fun(identifier: String, args: Array<String>) : Boolean {
                        if (args.size < 4)
                            return false
                        val conditionalValue = _content[args[1]]

                        placeEditorAtMe()
                        LLVMBuildCondBr(Builder.llvm, conditionalValue, func.find(args[2])._blockLlvm, func.find(args[3])._blockLlvm)
                        return true
                    }
        }
        fun append(identifier: String, args: Array<String>) : Boolean {
            if (args.size == 0)
                return false
            val ret = factory[args[0]]?.invoke(identifier, args)
            if (ret == null)
                return false
            return ret
        }
        fun onCondition(ifTrue: Block, ifFalse: Block) {
            // LLVMBuildCondBr(Builder.llvm, _cond, ifTrue._blockLlvm, ifFalse._blockLlvm)
        }
    }

    class Function constructor(val module: Module,
                               val type: LLVMTypeRef, val identifier: String, argTypes: Array<LLVMTypeRef>)
    {
        val _funLlvm : LLVMValueRef = LLVMAddFunction(module._modLlvm, identifier, LLVMFunctionType(type, argTypes[0], argTypes.size, 0))

        var Blocks : MutableMap<String, Block> = mutableMapOf()
        init {
            LLVMSetFunctionCallConv(_funLlvm, LLVMCCallConv)
        }

        fun find(identifier: String) : Block
        {
            return Blocks[identifier]!!
        }

        fun addBlock(identifier: String) : Block
        {
            val i = Block(this, identifier)
            Blocks[i.identifier] = i
            return i
        }

        var _paramVarsLlvm : MutableMap<String, LLVMValueRef> = mutableMapOf()
        fun declareParamVar(identifier: String, index: Int) : LLVMValueRef {
            _paramVarsLlvm[identifier] = LLVMGetParam(_funLlvm, index)
            return _paramVarsLlvm[identifier]!!
        }
        fun getParam(identifier: String) : LLVMValueRef?
        {
            return _paramVarsLlvm[identifier]
        }
    }

    operator fun Function.get(key: String): Block? {
        return Blocks[key]
    }

    class Module constructor(val identifier: String)
    {
        val _modLlvm = LLVMModuleCreateWithName(identifier)

        var functions : MutableMap<String, Function> = mutableMapOf()
        fun addFunction(type: LLVMTypeRef,
                        identifier: String, argTypes: Array<LLVMTypeRef>) : Function
        {
            val f = Function(this, type, identifier, argTypes)
            functions[f.identifier] = f
            return f
        }

        fun jit(): Jit
        {
            return Jit()
        }

        fun print()
        {
            println("DumpModule : [$identifier]")
            LLVMDumpModule(_modLlvm)
        }
    }
    var modules : MutableMap<String, Module> = mutableMapOf()

    fun createModule(identifier: String) : Module {
        val m = Module(identifier)
        modules[m.identifier] = m
        return m
    }

    fun jit() : Jit
    {
        return Jit()
    }

    fun compile(dest: String)
    {
        // Compile IR to file
    }

    fun print()
    {
        for (module in modules) {
            module.value.print()
        }
    }
}