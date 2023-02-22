fun main() {
    val chipset = Chipset("pokemon_red")
    val cpu = Cpu(chipset)

    val oneFlame = 456 * (144 + 10)
    while (true) {
        var step = 0
        while (step < oneFlame) {
            step += cpu.step()
        }
    }
}