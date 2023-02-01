fun main() {
    val register = mutableMapOf(
        Register.R1 to 1,
        Register.R2 to 0,
        Register.R3 to 0,
        Register.R4 to 0,
        Register.PC to 0
    )

    while (true) {
        val (command, pointer1, pointer2) = code[register[Register.PC] ?: 0]
        val arg1 = register[pointer1] ?: 0
        val arg2 = register[pointer2] ?: 0
        when (command) {
            CommandSet.ADD -> {
                register[pointer1] = arg1 + arg2
            }
            CommandSet.MV -> {
                register[pointer1] = arg2
                register[pointer2] = arg1
            }
            CommandSet.CP -> {
                register[pointer2] = arg1
            }
            CommandSet.EXIT -> {
                break
            }
        }
        register[Register.PC] = register[Register.PC]!! + 1
    }
    println(register)
}

val code = listOf(
    // 1:1 ,2:0 ,3:0 ,4:0
    Triple(CommandSet.CP, Register.R1, Register.R2),
    // 1:1 ,2:1 ,3:0 ,4:0
    Triple(CommandSet.ADD, Register.R1, Register.R2),
    // 1:2 ,2:1 ,3:0 ,4:0
    Triple(CommandSet.MV, Register.R1, Register.R3),
    // 1:0 ,2:1 ,3:2 ,4:0
    Triple(CommandSet.CP, Register.R3, Register.R4),
    // 1:0 ,2:1 ,3:2 ,4:2
    Triple(CommandSet.EXIT, Register.R1, Register.R2),
)
enum class CommandSet {
    ADD, MV, CP, EXIT
}

enum class Register {
    R1, R2, R3, R4, PC
}