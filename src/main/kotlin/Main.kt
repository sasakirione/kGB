fun main() {
    val chipset = Chipset()
    val cpu = Cpu(chipset)

    while (true) {
        cpu.execInstructions()
        cpu.printRegisterDump()
    }
}