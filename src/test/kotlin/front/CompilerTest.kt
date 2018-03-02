package front

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert.assertEquals
import java.io.PrintStream
import java.io.ByteArrayOutputStream
import java.io.File


class CompilerTest: Spek({
    var compiler : Compiler? = null
    val file = File(".test.txt")
    var res = ""

    fun execTestOnFile(compiler: Compiler, filename: String) {
        compiler.setFileName("tests/" + filename)
        compiler.createParser()
        compiler.compile()
        val pb = ProcessBuilder("./a.out")
        pb.redirectOutput(file)
        pb.start().waitFor()
        res = file.readText()
    }

    given("A compiler") {
        beforeEachTest {
            compiler = Compiler()
        }

        on("a simple putchar in main, with extern definition of putchar") {

            execTestOnFile(compiler!!, "PutcharInMain.koak")

            it("Should return the following value") {
                val ref = "a"
                assertEquals(ref, res)
            }
        }
        on("simple return in function add(x:int)x+2;") {

            execTestOnFile(compiler!!, "SimpleReturnInFunction.koak")

            it("Should return the following value") {
                val ref = "d"
                assertEquals(ref, res)
            }
        }
        on("Variable definition using a function") {

            execTestOnFile(compiler!!, "VarDefinitionWithFunction.koak")

            it("Should return the following value") {
                val ref = "a"
                assertEquals(ref, res)
            }
        }
        on("condition in function and operation in function call and multiple function call") {

            execTestOnFile(compiler!!, "ConditionInFunction.koak")

            it("Should return the following value") {
                val ref = "adef"
                assertEquals(ref, res)
            }
        }
        on("Function call with multiple parameters") {

            execTestOnFile(compiler!!, "MultiParamFunction.koak")

            it("Should return the following value") {
                val ref = "a"
                assertEquals(ref, res)
            }
        }
        on("Multiple function definitions and call, and nested function call") {

            execTestOnFile(compiler!!, "MultipleFunctionDefinitionsAndCall.koak")

            it("Should return the following value") {
                val ref = "ae"
                assertEquals(ref, res)
            }
        }
        on("Empty File") {

            execTestOnFile(compiler!!, "EmptyFile.koak")

            it("Should return the following value") {
                val ref = ""
                assertEquals(ref, res)
            }
        }
    }
})