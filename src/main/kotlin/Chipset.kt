@OptIn(ExperimentalUnsignedTypes::class)
class Chipset {
    /**
     * メモリ
     */
    private var memory: UByteArray = UByteArray(0x2000)
    /**
     * Highメモリ
     */
    private var highMemory: UByteArray = UByteArray(0x7f)

    /**
     * 割り込み有効化レジスタ(IE)
     */
    private var interruptEnableRegister: UByte = 0u

    /**
     * 16bitアドレスから値を読み出す
     *
     * @param address 16bitアドレス
     * @return アドレスに格納されている値
     */
    fun getValue(address: UShort): UByte {
        println(address)
        val value = when (address) {
            in 0xc000u..0xdfffu -> memory[address.toInt() - 0xc000]
            in 0xff80u..0xfffeu -> highMemory[address.toInt() - 0xff80]
            in 0xe000u..0xfdffu -> memory[address.toInt() - (0xe000+0x2000)]
            in 0xffffu..0xffffu -> interruptEnableRegister
            else -> 0xffu
        }
        return value
    }

    /**
     * 16bitアドレスに値を書き込む
     *
     * @param address 16bitアドレス
     * @param sourceValue 書き込む値
     */
    fun setValue(address: UShort, sourceValue: UByte) {
        println(address)
        println(sourceValue)
        when (address) {
            in 0xc000u..0xdfffu -> memory[address.toInt() - 0xc000] = sourceValue
            in 0xff80u..0xfffeu -> highMemory[address.toInt() - 0xff80] = sourceValue
            in 0xe000u..0xfdffu -> memory[address.toInt() - (0xe000+0x2000)] = sourceValue
            else -> {}
        }
    }
}
