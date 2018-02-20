package Llvm

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import org.bytedeco.javacpp.annotation.Cast

class Jit constructor(val module: Ir.Module, val optLevel: Int = 2)
{
    val typeNames : MutableMap<LLVMTypeRef, String> = mutableMapOf()

    var value = "0"
    val error: BytePointer = BytePointer(null as Pointer?)
    val filename = module.identifier
    val engine = LLVMExecutionEngineRef()
    // Only for optimization !

    val targetTriple = LLVMGetDefaultTargetTriple()
    val machineRef = LLVMTargetRef()
    val modulePass = LLVMCreatePassManager()


    init {
        val targetIndex = LLVMGetTargetFromTriple(targetTriple, machineRef, error)
        val myMachine = LLVMCreateTargetMachine(machineRef, targetTriple.string, "generic", "", 0, 0, 0)
        val machineDataLayout = LLVMCreateTargetDataLayout(myMachine)
        LLVMSetModuleDataLayout(module._modLlvm, machineDataLayout)
        var bp = BytePointer(filename)
        LLVMTargetMachineEmitToFile(myMachine, module._modLlvm, bp, 1, error)
        if (targetIndex != 0)
            throw Exception(error.string)

        LLVMInitializeAllTargetInfos();
        LLVMInitializeAllTargets();
        LLVMInitializeAllTargetMCs();
        LLVMInitializeAllAsmParsers();
        LLVMInitializeAllAsmPrinters();

        typeNames[LLVMInt32Type()] = "int32"
        typeNames[LLVMInt64Type()] = "int64"
        typeNames[LLVMDoubleType()] = "double"

        println("Creating JIT Engine for Module ${module.identifier}")
        for (f in module.functions)
            println("Containing ${f.key} : ${typeNames[f.value.type]}")

        if (LLVMCreateJITCompilerForModule(engine, module._modLlvm, optLevel, error) != 0) {
            System.err.println(error.string)
            LLVMDisposeMessage(error)
            System.exit(-1)
        }

        LLVMAddConstantPropagationPass(modulePass)
        LLVMAddInstructionCombiningPass(modulePass)
        LLVMAddPromoteMemoryToRegisterPass(modulePass)
        LLVMAddDemoteMemoryToRegisterPass(modulePass); // Demotes every possible value to memory
        LLVMAddGVNPass(modulePass)
        LLVMAddCFGSimplificationPass(modulePass)
        LLVMRunPassManager(modulePass, module._modLlvm)

        // Ready to execute module ?
    }

    class ExecResult constructor(val content: String)
    {
        var source = ""
    }

    // TODO: Il faut utiliser modulePass
    fun execute() : ExecResult
    {
        return ExecResult("Not Implemented Yet")
    }

    // TODO: il faudrait generer une pass fonction et l'utiliser ?
    fun runFunction(functionIdentifier: String, args : Array<Int>) : ExecResult
    {
        var target = functionIdentifier
        val exec_args = LLVMCreateGenericValueOfInt(LLVMInt32Type(), args[0].toLong(), 0)

        val exec_res = LLVMRunFunction(engine, module.functions[target]!!._funLlvm, 1, exec_args)
        val res = ExecResult(LLVMGenericValueToInt(exec_res, 0).toString())

        res.source = functionIdentifier
        return res
    }
}