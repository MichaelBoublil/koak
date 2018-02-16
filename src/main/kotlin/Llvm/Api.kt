package Llvm

import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*

class Api {
    fun grok(args: Arra y<String>) {

        // INIT
        val error = BytePointer(null as Pointer?) // Used to retrieve messages from functions

        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()

        // Creating a module.
        val mod = LLVMModuleCreateWithName("fac_module")
        // Creating module args
        val fac_args = arrayOf(LLVMInt32Type())
        // Creating function
        val fac = LLVMAddFunction(mod, "fac", LLVMFunctionType(LLVMInt32Type(), fac_args[0], 1, 0))



        // Wut?
        LLVMSetFunctionCallConv(fac, LLVMCCallConv)
        // On garde le premier parametre de la fonction fac
        val n = LLVMGetParam(fac, 0)

        // On ajoute des instructions dans la fonction fac
        // Entry
        val entry = LLVMAppendBasicBlock(fac, "entry")
        // iftrye
        val iftrue = LLVMAppendBasicBlock(fac, "iftrue")
        // iffalse
        val iffalse = LLVMAppendBasicBlock(fac, "iffalse")
        // end
        val end = LLVMAppendBasicBlock(fac, "end")

        // On cree un LLVM builder (vers IR?)
        val builder = LLVMCreateBuilder()

        // Soit.
        LLVMPositionBuilderAtEnd(builder, entry)
        // If est un comparateur d'int (LLVMIntEQ) entre n et une constante (LLVMInt32) qui vaut 0, on nomme cette comparaison : n == 0
        val If = LLVMBuildICmp(builder, LLVMIntEQ, n, LLVMConstInt(LLVMInt32Type(), 0, 0), "n == 0")
        // Build condition break
        // si If vaut true alors on va dans iftrue
        // si If vaut false on va dans iffalse
        LLVMBuildCondBr(builder, If, iftrue, iffalse)

        // Ensuite, on dit que le resultat de iftrue est une constante qui vaut 1 (logique, fac de 0 = 1)
        LLVMPositionBuilderAtEnd(builder, iftrue)
        val res_iftrue = LLVMConstInt(LLVMInt32Type(), 1, 0)
        // On ajoute un break : apres iftrue, on va a end. ( en d'autres termes : on return )
        LLVMBuildBr(builder, end)

        // Mais si n != 0, c'est a dire iffalse, que faisons-nous ?
        LLVMPositionBuilderAtEnd(builder, iffalse)
        // D'abord, on cree une soustraction, qui nous donne n-1.
        // C'est a dire qu'on build sub avec n et une constante 1 que l'on nomme "n - 1"
        val n_minus = LLVMBuildSub(builder, n, LLVMConstInt(LLVMInt32Type(), 1, 0), "n - 1")
        // On construit ensuite le tableau des arguments de l'appel recursif.
        //
        val call_fac_args = arrayOf(n_minus)
        // On ajoute un appel de fonction, on l'on precise qu'il y a un seul argument
        val call_fac = LLVMBuildCall(builder, fac, PointerPointer(*call_fac_args), 1, "fac(n - 1)")
        // Et donc si if false, on return n * fac(n - 1)
        val res_iffalse = LLVMBuildMul(builder, n, call_fac, "n * fac(n - 1)")
        // return de iffalse
        LLVMBuildBr(builder, end)

        // Probablement le printer de result
        LLVMPositionBuilderAtEnd(builder, end)
        // Phi ?
        val res = LLVMBuildPhi(builder, LLVMInt32Type(), "result")
        // Tableau de res_iftrue et res_iffalse ?
        val phi_vals = arrayOf(res_iftrue, res_iffalse)
        // Tableau de iftrue, iffalse ?
        val phi_blocks = arrayOf(iftrue, iffalse)
        // AddIncoming ??
        LLVMAddIncoming(res, PointerPointer(*phi_vals), PointerPointer(*phi_blocks), 2)
        // Je pense que c'est l'assemblage final.
        LLVMBuildRet(builder, res)

        System.err.println("Verify module...")
        LLVMVerifyModule(mod, LLVMAbortProcessAction, error)
        LLVMDisposeMessage(error) // Handler == LLVMAbortProcessAction -> No need to check errors
        System.err.println("Module verified.")

        System.err.println("Creating JIT Engine for our module.")
        val engine = LLVMExecutionEngineRef()
        if (LLVMCreateJITCompilerForModule(engine, mod, 2, error) != 0) {
            System.err.println(error.string)
            LLVMDisposeMessage(error)
            System.exit(-1)
        }
        System.err.println("Engine created.")

        System.err.println("Creating passManager")
        val pass = LLVMCreatePassManager()
        LLVMAddConstantPropagationPass(pass)
        LLVMAddInstructionCombiningPass(pass)
        LLVMAddPromoteMemoryToRegisterPass(pass)
        // LLVMAddDemoteMemoryToRegisterPass(pass); // Demotes every possible value to memory
        LLVMAddGVNPass(pass)
        LLVMAddCFGSimplificationPass(pass)
        LLVMRunPassManager(pass, mod)
        
        // Here we get the IR
        System.err.println("Our factorial module : ")
        LLVMDumpModule(mod)
        System.err.println("passManager ok")

        System.err.println("Creating arguments and running Factorial function")
        val exec_args = LLVMCreateGenericValueOfInt(LLVMInt32Type(), 10, 0)
        val exec_res = LLVMRunFunction(engine, fac, 1, exec_args)
        System.err.println()
        System.err.println("; Running fac(10) with JIT...")
        System.err.println("; Result: " + LLVMGenericValueToInt(exec_res, 0))

        LLVMDisposePassManager(pass)
        LLVMDisposeBuilder(builder)
        LLVMDisposeExecutionEngine(engine)
    }
}
