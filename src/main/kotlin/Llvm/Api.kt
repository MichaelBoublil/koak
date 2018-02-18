package Llvm

import Parser.*
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.LLVM.*

class Api {
    fun test() : Test
    {
        val parser = PegParser()
        val api = Api()

        val fakeTree = AST(KDefs(LocalDef(Defs(
                Prototype(Identifier("myFactorial"), PrototypeArgs(Identifier("nb"), VarType("int"), FunType("int"))),
                Expressions(
                        IfExpr(BinOp("=", true, Unary(PostFix(Primary(Identifier("nb")))), Unary(PostFix(Primary(Literal(DecimalConst("0")))))),
                                Expressions(BinOp("*", false, Unary(PostFix(Primary(Identifier("nb")))),
                                        Unary(PostFix(Primary(Identifier("myFactorial")),
                                                CallExpr(BinOp("-", false, Unary(PostFix(Primary(Identifier("nb")))), Unary(PostFix(Primary(Literal(DecimalConst("1"))))))))))),
                                Expressions(Unary(PostFix(Primary(Identifier("nb")))))))))))

        val ref = 5 * 4 * 3 * 2
        val ret = api.jit(fakeTree)

        if (ref == ret.value.toInt())
            return Test(true, ret.value)
        return Test(false, ret.value)
    }

    // TopLevel abstraction
    fun toIR(tree: AST) : Ir {
        val ir = Ir()

        return ir
    }

    fun jit(tree: AST) : Jit {
        // Build the module from the ast
        // Jit the module
        // extract return value of jit ?
        return Jit()
    }

    val error: BytePointer = BytePointer(null as Pointer?)
    init {
        LLVMLinkInMCJIT()
        LLVMInitializeNativeAsmPrinter()
        LLVMInitializeNativeAsmParser()
        LLVMInitializeNativeDisassembler()
        LLVMInitializeNativeTarget()
    }

    // This function is to be ignored. It's a simple example.
    fun grok(args: Array<String>) {

//        // INIT
//        val error = BytePointer(null as Pointer?) // Used to retrieve messages from functions
//
//        LLVMLinkInMCJIT()
//        LLVMInitializeNativeAsmPrinter()
//        LLVMInitializeNativeAsmParser()
//        LLVMInitializeNativeDisassembler()
//        LLVMInitializeNativeTarget()

        // Our own encapsulation of IR
        val ir = Ir()

        // On cree un LLVM builder (vers IR)
        val builder = LLVMCreateBuilder()

        // Creating a module. // javacpp-llvm
        val mod = LLVMModuleCreateWithName("fac_module")
        // Replaced by // Ir
        val myMod = ir.createModule("fac_module")

        // Creating function args
        val fac_args = arrayOf(LLVMInt32Type())
        // Creating function
        val fac = LLVMAddFunction(mod, "fac", LLVMFunctionType(LLVMInt32Type(), fac_args[0], 1, 0))
        // Replaced by // Ir
        val myFacFunction = myMod.addFunction(LLVMInt32Type(), "myFactorial", arrayOf<LLVMTypeRef>(LLVMInt32Type()))

        // SetFunctionCallConvention ?
        LLVMSetFunctionCallConv(fac, LLVMCCallConv)
        // On garde le premier parametre de la fonction fac

        // HOW TO ADD CODE IN FUNCTION :
        val n = LLVMGetParam(fac, 0)
        // n is now bound to first argument of function
        val myFactorialParameter = myFacFunction.declareParamVar("n", 0)


        //
        // TO BE CONTINUED
        //

        // On ajoute des blocks dans la fonction fac
        // Entry
        val entry = LLVMAppendBasicBlock(fac, "entry")
        // iftrye
        val iftrue = LLVMAppendBasicBlock(fac, "iftrue")
        // iffalse
        val iffalse = LLVMAppendBasicBlock(fac, "iffalse")
        // end
        val end = LLVMAppendBasicBlock(fac, "end")

        // Les blocs de la fonction factorial
        val FacEntry = myFacFunction.addBlock("entry")
        val FacTrue = myFacFunction.addBlock("iftrue")
        val FacFalse = myFacFunction.addBlock("iffalse")
        val FacRet = myFacFunction.addBlock("end")

        // Meaning to append entry ? because maybe you can build elsewhere ?
        LLVMPositionBuilderAtEnd(builder, entry)
        // If est un comparateur d'int (LLVMIntEQ) entre n et une constante (LLVMInt32) qui vaut 0, on nomme cette comparaison : n == 0
        val If = LLVMBuildICmp(builder, LLVMIntEQ, n, LLVMConstInt(LLVMInt32Type(), 0, 0), "n == 0")
        // Build condition break
        // si If vaut true alors on va dans iftrue
        // si If vaut false on va dans iffalse
        LLVMBuildCondBr(builder, If, iftrue, iffalse)

        // We'll have a compare instruction of n and 1
        FacEntry.append("n == 1", arrayOf("compare ints", "n", "1"))
        // And then we'll have a conditional jump for n == 0, if its true it will go to FacRet, otherwise to FacFalse
        FacEntry.append("jump", arrayOf("conditional jump", "n == 1", FacRet.identifier, FacFalse.identifier))

        FacFalse.append("n - 1", arrayOf("opp", "+", "n", "-1"))
        FacFalse.append("fac(n - 1)", arrayOf("call", myFacFunction.identifier))
        FacFalse.append("n * fac(n - 1)", arrayOf("opp", "*", "n", "fac(n - 1)"))
        FacFalse.append("jump", arrayOf("jump", FacRet.identifier))

        FacRet.append("phi", arrayOf("phi", WHAT THE FUCK ?))
//        FacElse.next(FacRet)
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

        ir.print()
    }
}
