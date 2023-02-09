class Chipset {
    fun getValue(address: UShort): UByte {
        println(address)
        return 0x00u
    }

    fun setValue(address: UShort, sourceValue: UByte) {
        println(address)
        println(sourceValue)
    }
}
