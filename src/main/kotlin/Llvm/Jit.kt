@file:Suppress("UNUSED_PARAMETER")

package Llvm

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

fun String.runCommand(workingDir: File): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}

class Jit constructor (val module: Ir.Module,
                       val filename : String = "a.out",
                       optLevel: Int = 2,
                       verbose: Boolean = true)
{
    val error: BytePointer = BytePointer(null as Pointer?)
    val engine = LLVMExecutionEngineRef()
    // Only for optimization !

    val targetTriple = LLVMGetDefaultTargetTriple()
    val machineRef = LLVMTargetRef()
    val modulePass = LLVMCreatePassManager()

    init {
        LLVMInitializeAllTargetInfos();
        LLVMInitializeAllTargets();
        LLVMInitializeAllTargetMCs();
        LLVMInitializeAllAsmParsers();
        LLVMInitializeAllAsmPrinters();

        for (f in module.functions)
            f.value.print()

        if (LLVMCreateJITCompilerForModule(engine, module._modLlvm, optLevel, error) != 0) {
            System.err.println(error.string)
            LLVMDisposeMessage(error)
            System.exit(-1)
        }

        LLVMAddConstantPropagationPass(modulePass)
        LLVMAddInstructionCombiningPass(modulePass)
        LLVMAddPromoteMemoryToRegisterPass(modulePass)
        LLVMAddDemoteMemoryToRegisterPass(modulePass)
        LLVMAddGVNPass(modulePass)
        LLVMAddCFGSimplificationPass(modulePass)
        LLVMRunPassManager(modulePass, module._modLlvm)
    }

    /*
    Creates filename.s (assembly)
    Creates filename.o (ELF64)
    Creates filename (Executable)
     */
    fun compileToFile() {
        val targetIndex = LLVMGetTargetFromTriple(targetTriple, machineRef, error)
        val myMachine = LLVMCreateTargetMachine(machineRef, targetTriple.string, "generic", "", 0, 0, 0)
        val machineDataLayout = LLVMCreateTargetDataLayout(myMachine)


        LLVMSetModuleDataLayout(module._modLlvm, machineDataLayout)
        var bp = BytePointer(filename + ".s")
        LLVMTargetMachineEmitToFile(myMachine, module._modLlvm, bp, LLVMAssemblyFile, error)
        bp = BytePointer(filename + ".o")
        LLVMTargetMachineEmitToFile(myMachine, module._modLlvm, bp, LLVMObjectFile, error)

        ProcessBuilder("./linker.sh", filename).start().waitFor()

        if (targetIndex != 0)
            throw Exception(error.string)
    }

    /*
    Cette classe contient le resultat d'une execution de fonction (avec runFunction)
    content : Resultat de l'execution
    source : Commande executee
     */
    class ExecResult constructor(val content: String)
    {
        var source = ""
    }

    // TODO: runFunction with Array<String> pour pouvoir appeler n'importe quelle fonction.. :/
    fun runFunction(functionIdentifier: String, args : Array<String>) : ExecResult
    {
        var target = functionIdentifier

        val exec_args = PointerPointer(*args.map {
            if (it.contains(Regex("^.*[\\.].*$"))) {
                LLVMCreateGenericValueOfFloat(LLVMDoubleType(), it.toDouble())
            }
            else
                LLVMCreateGenericValueOfInt(LLVMInt32Type(), it.toLong(), 0)
        }.toTypedArray())
        val exec_res = LLVMRunFunction(engine, module.functions[target]!!._funLlvm, args.size, exec_args)
        val res = ExecResult(LLVMGenericValueToInt(exec_res, 0).toString())

        res.source = functionIdentifier
        return res
    }
    fun runFunction(functionIdentifier: String, args : Array<Int>) : ExecResult
    {
        val target = functionIdentifier

        val exec_args = PointerPointer(*(args.map { LLVMCreateGenericValueOfInt(LLVMInt32Type(), it.toLong(), 0) }.toTypedArray()))
        val exec_res = LLVMRunFunction(engine, module.functions[target]!!._funLlvm, args.size, exec_args)
        val res = ExecResult(LLVMGenericValueToInt(exec_res, 0).toString())

        res.source = functionIdentifier
        return res
    }
    fun runFunction(functionIdentifier: String, args : Array<Double>) : ExecResult
    {
        val target = functionIdentifier

        val exec_args = PointerPointer(*(args.map { LLVMCreateGenericValueOfFloat(LLVMDoubleType(), it) }.toTypedArray()))
        val exec_res = LLVMRunFunction(engine, module.functions[target]!!._funLlvm, args.size, exec_args)
        val res = ExecResult(LLVMGenericValueToInt(exec_res, 0).toString())

        res.source = functionIdentifier
        return res
    }
}