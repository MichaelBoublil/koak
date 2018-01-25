package Main

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert.*
import org.junit.runner.RunWith

class ParserTest: Spek({
    given("A parser") {
        val parser = Parser("def fun(i : int) {}")
        on("IR assemble") {
            val ir = parser.toIR()
            it("should return the following value") {
                assertEquals("salut", ir)
            }
        }
    }
})