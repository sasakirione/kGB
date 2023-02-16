fun main() {
    val chipset = Chipset("cpu_instrs")
    val cpu = Cpu(chipset)

    while (true) {
        cpu.execInstructions()
        cpu.printRegisterDump()
    }
}