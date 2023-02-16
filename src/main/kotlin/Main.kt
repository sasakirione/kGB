fun main() {
    val chipset = Chipset("pokemon_red")
    val cpu = Cpu(chipset)

    while (true) {
        var step = 0
        while (true) {
            step += cpu.step()
            if (step >= 456 * (144 + 10)) {
                break
            }
        }
    }
}