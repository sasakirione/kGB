class Cpu(private val memory: Memory){
    private var registerA: Byte = 0x00
    private var registerB: Byte = 0x00
    private var registerC: Byte = 0x00
    private var registerD: Byte = 0x00
    private var registerE: Byte = 0x00
    private var registerH: Byte = 0x00
    private var registerL: Byte = 0x00
    private var registerSP: Short = 0x0000
    private var registerPC: Short = 0x0000

    private var flagZero: Boolean = false
    private var flagN: Boolean = false
    private var flagH: Boolean = false
    private var flagCarry: Boolean = false

    private var interruptMasterEnable: Boolean = false

    /**
     * レジスタの値をダンプする
     */
    fun printRegisterDump() {
        println("A: ${registerA.toUByte().toString(16)}")
        println("B: ${registerB.toUByte().toString(16)}")
        println("C: ${registerC.toUByte().toString(16)}")
        println("D: ${registerD.toUByte().toString(16)}")
        println("E: ${registerE.toUByte().toString(16)}")
        println("H: ${registerH.toUByte().toString(16)}")
        println("L: ${registerL.toUByte().toString(16)}")
        println("SP: ${registerSP.toUShort().toString(16)}")
        println("PC: ${registerPC.toUShort().toString(16)}")
        println("Z: ${getFlagZ().toUByte().toString(16)}")
        println("N: ${getFlagN().toUByte().toString(16)}")
        println("H: ${getFlagH().toUByte().toString(16)}")
        println("C: ${getFlagC().toUByte().toString(16)}")
    }

    private fun getFlagZ(): Byte {
        return if (flagZero) 0x1 else 0x0
    }

    private fun getFlagN(): Byte {
        return if (flagN) 0x1 else 0x0
    }

    private fun getFlagH(): Byte {
        return if (flagH) 0x1 else 0x0
    }

    private fun getFlagC(): Byte {
        return if (flagCarry) 0x1 else 0x0
    }


    /**
     * PCを1薦める
     */
    private fun pcPlusOne() {
        registerPC = (registerPC + 1).toShort()
    }

    /**
     * メモリから8bitで値を読み込む
     *
     * @return メモリから読み込んだ値(8bit)
     */
    private fun readMemoryFrom8Bit(): Byte {
        val value = memory.getValue(registerPC)
        pcPlusOne()
        return value
    }

    /**
     * ccf: CarryFlagを完了する
     * 0x3F
     */
    private fun ccf() {
        flagCarry = !flagCarry
    }

    /**
     * scf: CarryFlagを設定する
     * 0x37
     */
    private fun scf() {
        flagCarry = true
    }

    /**
     * nop: 操作なし
     * 0x00
     */
    private fun nop() {
        println("NOP")
    }

    /**
     * halt: 割り込みが発生するまでお休み
     * 0x76
     */
    private fun halt() {
        println("HALT")
    }

    /**
     * stop: スタンバイモードに入る
     * 0x10
     */
    private fun stop() {
        println("STOP")
    }

    /**
     * 割り込みを有効にする
     */
    private fun ei() {
        interruptMasterEnable = true
    }

    /**
     * 割り込みを無効にする
     */
    private fun di() {
        interruptMasterEnable = false
    }

    /**
     * 命令を実行する
     */
    fun execInstructions() {
        when (readMemoryFrom8Bit()) {
            0x00.toByte() -> this.nop()
            0x10.toByte() -> this.stop()
            0x37.toByte() -> this.scf()
            0x3f.toByte() -> this.ccf()
            0x76.toByte() -> this.halt()
            0xf3.toByte() -> this.di()
            0xfb.toByte() -> this.ei()
            else -> println("Unknown instruction")
        }
    }
}