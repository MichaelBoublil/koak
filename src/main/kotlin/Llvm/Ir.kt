package Llvm

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*

class Ir
{
    enum class Actions
    {
        VarDec,
        VarSet,
        If,
        Else,
        Return
    }

    class Instruction constructor (val func: Function, val action: Actions, val identifier : String)
    {

    }

    class Function constructor(val module: Module,
                               val type: LLVMTypeRef, val identifier: String, argTypes: Array<LLVMTypeRef>)
    {
        val _funLlvm : LLVMValueRef

        init {
            _funLlvm = LLVMAddFunction(module._modLlvm, identifier, LLVMFunctionType(type, argTypes[0], argTypes.size, 0))
            LLVMSetFunctionCallConv(_funLlvm, LLVMCCallConv)
        }

        var instructions : MutableMap<String, Instruction> = mutableMapOf()

        fun addInstruction(action: Actions, identifier: String) : Instruction
        {
            val i = Instruction(this, action, identifier)
            instructions[i.identifier] = i
            return i
        }
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
    }

    init {
        val main= createModule("Main")
        val fac = main.addFunction(LLVMInt32Type(),"myFactorial", arrayOf(LLVMInt32Type()))
        fac.addInstruction(Actions.Return, "return 0")
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
}