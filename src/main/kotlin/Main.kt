fun main() {
    val chipset = Chipset("pokemon_red")
    val cpu = Cpu(chipset)

    while (true) {
        cpu.execInstructions()
        cpu.printRegisterDump()
    }
}