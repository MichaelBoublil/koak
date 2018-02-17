package Llvm

class Ir
{
    class Instruction constructor (val identifier : String)
    {

    }

    class Function constructor(val identifier: String)
    {
        var instructions : MutableMap<String, Instruction> = mutableMapOf()

        fun addInstruction(identifier: String) : Instruction
        {
            val i = Instruction(identifier)
            instructions[i.identifier] = i
            return i
        }
    }

    class Module constructor(val identifier: String)
    {
        var functions : MutableMap<String, Function> = mutableMapOf()

        fun addFunction(identifier: String) : Function
        {
            val f = Function(identifier)
            functions[f.identifier] = f
            return f
        }
    }

    var modules : MutableMap<String, Module> = mutableMapOf()
    fun createModule(identifier: String) : Module {
        val m = Module(identifier)
        modules[m.identifier] = m
        return m
    }
}