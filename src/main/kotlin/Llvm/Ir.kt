package Llvm

import com.sun.org.apache.xpath.internal.operations.Bool
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*

// TODO: Ne plus avoir un object parce que peut etre c'est mieux de le mettre dans le IR ?
object Builder
{
    val llvm = LLVMCreateBuilder()
}

// TODO: Reflechir a l'interet de IR a PART pour construire l'IR facilement
// Du coup ca pourrait etre un IrBuilder ?
// Du coup pas de notion de compile ou de jit, on jit des modules, pas des FICHIERS, enfin en fait je me rends compte que mono module ca le fait
class Ir
{
    val error: BytePointer = BytePointer(null as Pointer?)

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
                        first = func.getLocalVar(args[1])?.let { it } ?: func.search(args[1]) as LLVMValueRef
                        second = func.getLocalVar(args[2])?.let { it } ?: func.search(args[2]) as LLVMValueRef

                        placeEditorAtMe()
                        _content[identifier] = LLVMBuildICmp(Builder.llvm, LLVMIntEQ, first, second, identifier)
                        return true
                    }
            factory["call"] =
                    fun(identifier: String, args: Array<String>) : Boolean {
                        println("blablabla ${args.size}")
                        if (args.size < 2)
                            return false

                        // TODO: Find all arguments for the call

                        val call_args = args.filterIndexed({ i, s -> i > 1}).map {
                            func.search(it)?.let { it } ?: func.createConstInt(it)
                        }.toTypedArray()

                        val targetFunc = func.module.functions[identifier]?._funLlvm
                        placeEditorAtMe()
                        _content[identifier] = LLVMBuildCall(Builder.llvm, targetFunc!!, PointerPointer(*call_args), call_args.size, identifier)
                        println("added new function call ${identifier} in module label ${func.identifier}" )
                        return true
                    }
            factory["jump"] =
                    fun(identifier: String, args: Array<String>) : Boolean {
                        if (args.size < 2)
                            return false
                        placeEditorAtMe()
                        _content[identifier] = LLVMBuildBr(Builder.llvm, func.findBlock(args[1])._blockLlvm)
                        return true
                    }
            factory["conditional jump"] =
                    fun(identifier: String, args: Array<String>) : Boolean {
                        if (args.size < 4)
                            return false
                        val conditionalValue = _content[args[1]]

                        placeEditorAtMe()
                        _content[identifier] = LLVMBuildCondBr(Builder.llvm, conditionalValue, func.findBlock(args[2])._blockLlvm, func.findBlock(args[3])._blockLlvm)
                        return true
                    }
            factory["binop"] =
                    fun(identifier: String, args: Array<String>) : Boolean {
                        if (args.size < 4)
                            return false
                        placeEditorAtMe()
                        // TODO: Add other operations
                        if (args[1].compareTo("*") == 0)
                            _content[identifier] = LLVMBuildMul(Builder.llvm,
                                    func.getLocalVar(args[2])?.let { it } ?: func.search(args[2]),
                                    func.getLocalVar(args[3])?.let { it } ?: func.search(args[3]),
                                    identifier)
                        else if (args[1].compareTo("+") == 0)
                            _content[identifier] = LLVMBuildAdd(Builder.llvm,
                                    func.getLocalVar(args[2])?.let { it } ?: func.search(args[2]),
                                    func.getLocalVar(args[3])?.let { it } ?: func.search(args[3]),
                                    identifier)
                        return true
                    }
            factory["return"] =
                    fun(identifier: String, args: Array<String>) : Boolean {
                        if (args.size < 2)
                            return false
                        _content[identifier] = LLVMBuildRet(Builder.llvm, func.search(args[1]))
                        return true
                    }
            factory["declare"] = fun(identifier: String, args: Array<String>) : Boolean {
                placeEditorAtMe()

                return true
            }
            factory["phi int"] = fun(identifier: String, args: Array<String>) : Boolean {
                if (args.size < 5)
                    return false

                placeEditorAtMe()
                val res = LLVMBuildPhi(Builder.llvm, LLVMInt32Type(), identifier)
                val phi_blocks = args
                        .filterIndexed({ i, s -> i % 2 == 1 && i > 0})
                        .map { s -> func.findBlock(s)._blockLlvm }
                        .toTypedArray()
                val phi_vals = args
                        .filterIndexed({ i, s -> i % 2 == 0 && i > 0})
                        .map { s -> func.getLocalVar(s)?.let { it } ?: func.search(s)!! } // il faut aller chercher dans les contents de toute la fonction
                        .toTypedArray()
                _content[identifier] = res
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
                               val type: LLVMTypeRef, val identifier: String, val argTypes: Array<LLVMTypeRef>)
    {

        val _funLlvm : LLVMValueRef

        init {
            if (argTypes.isEmpty()) {
                _funLlvm = LLVMAddFunction(module._modLlvm, identifier, LLVMFunctionType(type, LLVMTypeRef(), 0, 0))
            } else {
                _funLlvm = LLVMAddFunction(module._modLlvm, identifier,
                        LLVMFunctionType(type, argTypes[0], argTypes.size, 0))
            }
        }

        var Blocks : MutableMap<String, Block> = mutableMapOf()
        init {
            LLVMSetFunctionCallConv(_funLlvm, LLVMCCallConv)
        }

        fun addBlocks(vararg args: String)
        {
            for (arg in args) {
                addBlock(arg)
            }
        }
        fun createConstInt(value: String) : LLVMValueRef {
            val ref = LLVMConstInt(LLVMInt32Type(), value.toLong(), 0)
            _local[value] = ref
            return ref
        }

        fun findBlock(identifier: String) : Block
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
        var localValues : MutableMap<String, String> = mutableMapOf()
        var argNames : MutableMap<Int, String> = mutableMapOf()
        fun declareParamVar(identifier: String, index: Int) : LLVMValueRef {
            _local[identifier] = LLVMGetParam(_funLlvm, index)
            argNames[index] = identifier
            return _local[identifier]!!
        }
        fun declareLocalVar(identifier: String, type: String, value: String) {
            val knownTypes : MutableMap<String, LLVMTypeRef> = mutableMapOf()
            knownTypes["int32"] = LLVMInt32Type()
            knownTypes["int64"] = LLVMInt64Type()
            knownTypes["double"] = LLVMDoubleType()
            knownTypes["void"] = LLVMVoidType()
            localValues[identifier] = value
            try {
                if (type == "int32") {
                    _local[identifier] = LLVMConstInt(LLVMInt32Type(), value.toLong(), 0)
                }
                if (type == "int64") {
                    _local[identifier] = LLVMConstInt(LLVMInt64Type(), value.toLong(), 0)
                }
                if (type == "double") {
                    _local[identifier] = LLVMConstReal(LLVMDoubleType(), value.toDouble())
                }
            } catch (e : Exception) {
                System.err.println("Cannot declare variable of type Expr")
                throw e
            }
        }
        fun getLocalVar(identifier: String) : LLVMValueRef?
        {
            return _local[identifier]
        }
        // search for instruction in any blocks of the function
        fun search(identifier: String, searchInLocal: Boolean = true) : LLVMValueRef?
        {
            println("TEST")
            try {
                return LLVMConstReal(LLVMDoubleType(), identifier.toDouble())
            } catch (e: Exception) {
                System.err.println("Search ${identifier} is not a long.")
            }
            for (block in Blocks) {
                for (inst in block.value._content)
                    if (inst.key == identifier) {
                        return inst.value
                    }
            }
            if (searchInLocal) {
                for (v in _local)
                    if (v.key == identifier)
                        return v.value
            }
            throw Exception("Unresolved identifier ${identifier}")
        }

        fun print() {
            val knownNames : MutableMap<LLVMTypeRef, String> = mutableMapOf()
            knownNames[LLVMInt32Type()] = "int32"
            knownNames[LLVMInt64Type()] = "int64"
            knownNames[LLVMDoubleType()] = "double"
            print(knownNames[this.type] + "\t${identifier} (")
            if (argTypes.isNotEmpty()) {
                var i = 0
                for (arg in this.argTypes) {
                    print(argNames[i] + ": " + knownNames[arg])
                    i++
                    if (i < argTypes.size)
                        print(", ")
                }
            }
            println(")")
            for (local in _local) {
                println("\t${local.key} = ${this.localValues[local.key]}")
            }
        }
    }

    operator fun Function.get(key: String): Block? {
        return Blocks[key]
    }

    class Module constructor(val identifier: String)
    {
        lateinit var main : String
        val _modLlvm = LLVMModuleCreateWithName(identifier)
        val avars : MutableMap<String, String> = mutableMapOf()
        val _vars : MutableMap<String, LLVMValueRef> = mutableMapOf()

        var functions : MutableMap<String, Function> = mutableMapOf()
        fun addFunction(type: LLVMTypeRef,
                        identifier: String, argTypes: Array<LLVMTypeRef> = arrayOf()) : Function
        {
            if (functions[identifier] != null)
                return functions[identifier]!!
            val f = Function(this, type, identifier, argTypes)
            functions[f.identifier] = f
            return f
        }

        fun jit(): Jit
        {
            val jit = Jit(this)
            return jit
        }

        fun setMain(identifier: String) : Boolean
        {
            if (functions.containsKey(identifier)) {
                main = identifier
                return true
            }
            return false
        }

        fun prettyPrint()
        {
            var str = LLVMPrintModuleToString(_modLlvm).string
            str = str.replace("icmp eq i32", "Are Ints Equal")
                    .replace("br i1", "Conditional Jump")
                    .replace("br", "Jump")
                    .replace("call", "Function Call")
                    .replace("mul i32", "Multiply Ints")
                    .replace("i32", "int")
                    .replace("phi int", "Conditional Value")
                    .replace("ret int", "Return Int")
                    .replace("; preds = ", "Incoming From ")
                    .replace("define", "fun")
                    .replace("; ModuleID = ", "Current Module : ")
            println(str)
        }

        fun declare(identifier: String, value: String) {
            avars[identifier] = value
        }

        fun print()
        {
            println("\t == DUMP ${identifier} ==")
            println("Module vars :")
            println("=============")
            for (mvar in avars) {
                println(mvar.key + "\t=\t" + mvar.value)
            }
            println("Module functions :")
            println("==================")
            for (f in functions)
                f.value.print()
            println("Resulting IR :")
            println("==============")
            val str = LLVMPrintModuleToString(_modLlvm).string
            println(str)
        }
    }

    var modules : MutableMap<String, Module> = mutableMapOf()

    fun createModule(identifier: String) : Module {
        val m = Module(identifier)
        modules[m.identifier] = m
        return m
    }

    fun jit(module: String = "") : MutableList<Jit>
    {
        val list : MutableList<Jit> = mutableListOf()
        for (mod in modules) {
            if (module != "") {
                val res = mod.value.jit()
                println("JIT Of ${mod.value.identifier} is :\n$res")
                list.add(res)
            } else {
                if (module == mod.key) {
                    val res = mod.value.jit()
                    println("JIT Of ${mod.value.identifier} is :\n$res")
                    list.add(res)
                }
            }
        }
        return list
    }

    fun compile(dest: String)
    {
        val jits = jit()
        for (jit in jits) {
            
        }
    }

    fun pretty()
    {
        for (module in modules) {
            module.value.prettyPrint()
        }
    }

    fun print()
    {
        for (module in modules) {
            module.value.print()
        }
    }

    fun verify()
    {
        System.err.println("Verify modules...")
        for (module in modules) {
            LLVMVerifyModule(module.value._modLlvm, LLVMAbortProcessAction, error)
        }
        LLVMDisposeMessage(error) // Handler == LLVMAbortProcessAction -> No need to check errors
        System.err.println("Modules verified.")
    }
}