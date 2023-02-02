import kotlin.experimental.and

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
     * メモリから16bitで値を読み込む
     *
     * @return メモリから読み込んだ値(16bit)
     */
    private fun readMemoryFrom16Bit(): Short {
        val value = memory.getValue(registerPC)
        pcPlusOne()
        return value.toShort()
    }

    /**
     * レジスタにDDDアドレスを使用して値を書き込む
     *
     * @param ddd レジスタの宛先を表すビットパターン
     * @param value 書き込む値
     */
    private fun insertValueFromDDD(ddd: Byte, value: Byte) {
        when (ddd.toInt()) {
            0x00 -> registerB = value
            0x01 -> registerC = value
            0x02 -> registerD = value
            0x03 -> registerE = value
            0x04 -> registerH = value
            0x05 -> registerL = value
            0x06 -> throw Exception("Unknown Address: 0x06")
            0x07 -> registerA = value
        }
    }

    /**
     * レジスタからSSSアドレスを使用して値を読み込む
     *
     * @param sss レジスタの宛先を表すビットパターン
     * @return レジスタから読み込んだ値
     */
    private fun getValueFromSSS(sss: Byte): Byte {
        return when (sss.toInt()) {
            0x00 -> registerB
            0x01 -> registerC
            0x02 -> registerD
            0x03 -> registerE
            0x04 -> registerH
            0x05 -> registerL
            0x06 -> throw Exception("Unknown Address: 0x06")
            0x07 -> registerA
            else -> {
                throw Exception("Unknown Address: $sss")
            }
        }
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
     * Aレジスタに入力された値を加算する
     *
     * @param value 加算する値
     */
    private fun addAr(value: Byte) {
        val result = registerA + value
        flagZero = result == 0x0
        flagN = false
        flagH = (registerA and 0xf) + (value and 0xf) > 0xf
        flagCarry = result > 0xff
        registerA = result.toByte()
    }

    /**
     * 命令を実行する
     */
    fun execInstructions() {
        val instruction = readMemoryFrom8Bit()
        val value1 = instruction and 7

        when (instruction.toInt()) {
            0x00 -> this.nop()
            0x10 -> this.stop()
            0x37 -> this.scf()
            0x3f -> this.ccf()
            0x76 -> this.halt()
            in 0x80..0x87 -> this.addAr(value1)
            0xf3 -> this.di()
            0xfb -> this.ei()
            else -> println("Unknown instruction")
        }
    }
}