package Llvm

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*

class Jit constructor(val module: Ir.Module)
{
    var value = "0"
    val error: BytePointer = BytePointer(null as Pointer?)
    val engine = LLVMExecutionEngineRef()
    init {
        println("Creating JIT Engine for IR")
        if (LLVMCreateJITCompilerForModule(engine, module._modLlvm, 2, error) != 0) {
            System.err.println(error.string)
            LLVMDisposeMessage(error)
            System.exit(-1)
        }

        val pass = LLVMCreatePassManager()
        LLVMAddConstantPropagationPass(pass)
        LLVMAddInstructionCombiningPass(pass)
        LLVMAddPromoteMemoryToRegisterPass(pass)
        LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
        LLVMAddGVNPass(pass)
        LLVMAddCFGSimplificationPass(pass)
        LLVMRunPassManager(pass, module._modLlvm)

        // EXECUTE MISSING HERE

        // Add to destructor of Ir or Api ?
//        LLVMDisposePassManager(pass)
        // LLVMDisposeBuilder(builder)
//        LLVMDisposeExecutionEngine(engine)
    }

    fun execute(functionIdentifier: String = "", value: Int) : String
    {
        var target = functionIdentifier
        if (functionIdentifier == "")
            target = module.main
        val exec_args = LLVMCreateGenericValueOfInt(LLVMInt32Type(), value.toLong(), 0)
        val exec_res = LLVMRunFunction(engine, module.functions[target]!!._funLlvm, 1, exec_args)
        System.err.println()
        val nbr = value.toLong()
        System.err.println("; Running $target($nbr) with JIT...")
        System.err.println("; Result: " + LLVMGenericValueToInt(exec_res, 0))
        return LLVMGenericValueToInt(exec_res, 0).toString()
    }
}