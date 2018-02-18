package Llvm

import org.bytedeco.javacpp.*
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
                        if (func.getLocalVar(args[1]) != null)
                            first = func.getLocalVar(args[1])!!
                        else
                            first = LLVMConstInt(LLVMInt32Type(), args[1].toLong(), 0)
                        if (func.getLocalVar(args[2]) != null)
                            second = func.getLocalVar(args[2])!!
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
                        _content[identifier] = LLVMBuildCondBr(Builder.llvm, conditionalValue, func.find(args[2])._blockLlvm, func.find(args[3])._blockLlvm)
                        return true
                    }
            factory["binop"] =
                    fun(identifier: String, args: Array<String>) : Boolean {
                        if (args.size < 4)
                            return false
                        placeEditorAtMe()
                        if (args[1].compareTo("*") == 0) // TODO: This is squared, not a real mul based on args !!
                            _content[identifier] = LLVMBuildMul(Builder.llvm, func.getLocalVar("n"), func.getLocalVar("n"), identifier)
                        return true
                    }
            factory["phi int"] =
                    fun (identifier: String, args: Array<String>) : Boolean {
                        if (args.size < 5)
                            return false

                        placeEditorAtMe()
                        val res = LLVMBuildPhi(Builder.llvm, LLVMInt32Type(), identifier)
                        val phi_blocks = args
                                .filterIndexed({ i, s -> i % 2 == 1 && i > 0})
                                .map { s -> func.find(s)._blockLlvm }
                                .toTypedArray()
                        val phi_vals = args
                                .filterIndexed({ i, s -> i % 2 == 0 && i > 0})
                                .map { s -> func.getLocalVar(s)?.let { it } ?: func.search(s)!! } // il faut aller chercher dans les contents de toute la fonction
                                .toTypedArray()
                        LLVMAddIncoming(res, PointerPointer(*phi_vals), PointerPointer(*phi_blocks), phi_blocks.size)
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
    }

    class Function constructor(val module: Module,
                               val type: LLVMTypeRef, val identifier: String, argTypes: Array<LLVMTypeRef>)
    {
        val _funLlvm : LLVMValueRef = LLVMAddFunction(module._modLlvm, identifier, LLVMFunctionType(type, argTypes[0], argTypes.size, 0))

        var Blocks : MutableMap<String, Block> = mutableMapOf()
        init {
            LLVMSetFunctionCallConv(_funLlvm, LLVMCCallConv)
        }

        fun createConstInt(value: String) : LLVMValueRef {
            val ref = LLVMConstInt(LLVMInt32Type(), value.toLong(), 0)
            _local[value] = ref
            return ref
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

        var _local : MutableMap<String, LLVMValueRef> = mutableMapOf()
        fun declareParamVar(identifier: String, index: Int) : LLVMValueRef {
            _local[identifier] = LLVMGetParam(_funLlvm, index)
            return _local[identifier]!!
        }
        fun getLocalVar(identifier: String) : LLVMValueRef?
        {
            return _local[identifier]
        }
        // search for instruction in any blocks of the function
        fun search(identifier: String) : LLVMValueRef?
        {
            for (block in Blocks) {
                for (inst in block.value._content)
                    if (inst.key == identifier) {
                        return inst.value
                    }
            }
            return null
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