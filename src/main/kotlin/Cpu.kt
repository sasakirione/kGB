import kotlin.experimental.and

class Cpu(private val memory: Memory){
    private var registerA: UByte = 0x00u
    private var registerB: UByte = 0x00u
    private var registerC: UByte = 0x00u
    private var registerD: UByte = 0x00u
    private var registerE: UByte = 0x00u
    private var registerH: UByte = 0x00u
    private var registerL: UByte = 0x00u
    private var registerSP: UShort = 0x0000u
    private var registerPC: UShort = 0x0100u

    private var flagZero: Boolean = false
    private var flagN: Boolean = false
    private var flagH: Boolean = false
    private var flagCarry: Boolean = false

    private var interruptMasterEnable: Boolean = false

    /**
     * レジスタの値をダンプする
     */
    fun printRegisterDump() {
        println("A: ${registerA.toString(16)}")
        println("B: ${registerB.toString(16)}")
        println("C: ${registerC.toString(16)}")
        println("D: ${registerD.toString(16)}")
        println("E: ${registerE.toString(16)}")
        println("H: ${registerH.toString(16)}")
        println("L: ${registerL.toString(16)}")
        println("SP: ${registerSP.toString(16)}")
        println("PC: ${registerPC.toString(16)}")
        println("Z: ${getFlagZ().toString(16)}")
        println("N: ${getFlagN().toString(16)}")
        println("H: ${getFlagH().toString(16)}")
        println("C: ${getFlagC().toString(16)}")
    }

    private fun getFlagZ(): UByte {
        return if (flagZero) 0x1u else 0x0u
    }

    private fun getFlagN(): UByte {
        return if (flagN) 0x1u else 0x0u
    }

    private fun getFlagH(): UByte {
        return if (flagH) 0x1u else 0x0u
    }

    private fun getFlagC(): UByte {
        return if (flagCarry) 0x1u else 0x0u
    }


    /**
     * PCを1薦める
     */
    private fun pcPlusOne() {
        registerPC = (registerPC + 1u).toUShort()
    }

    /**
     * メモリから8bitで値を読み込む
     *
     * @return メモリから読み込んだ値(8bit)
     */
    private fun readMemoryFrom8Bit(): UByte {
        val value = memory.getValue(registerPC)
        pcPlusOne()
        return value
    }

    /**
     * メモリから16bitで値を読み込む
     *
     * @return メモリから読み込んだ値(16bit)
     */
    private fun readMemoryFrom16Bit(): UShort {
        val value = memory.getValue(registerPC)
        pcPlusOne()
        return value.toUShort()
    }

    /**
     * レジスタにDDDアドレスを使用して値を書き込む
     *
     * @param ddd レジスタの宛先を表すビットパターン
     * @param value 書き込む値
     */
    private fun insertValueFromDDD(ddd: UByte, value: UByte) {
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
    private fun getValueFromSSS(sss: UByte): UByte {
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
     * Aレジスタに入力されたレジスタの値を加算する
     *
     * @param sss 加算するレジスタのアドレス
     */
    private fun addAr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        addA(sourceValue)
    }

    /**
     * AレジスタにHLレジスタに格納されたアドレスの値を加算する
     */
    private fun addAHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = memory.getValue(sourceAddress.toUShort())
        addA(sourceValue)
    }

    private fun addA(sourceValue: UByte) {
        val result = registerA + sourceValue
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerA and 0xfu) + (sourceValue and 0xfu) > 0xfu
        flagCarry = result > 0xffu
        registerA = result.toUByte()
    }

    /**
     * 命令を実行する
     */
    fun execInstructions() {
        val instruction = readMemoryFrom8Bit()
        val value1 = instruction and 7u
        //val value2 = (instruction shr 3u) and 7u

        when (instruction.toInt()) {
            0x00 -> this.nop()
            0x10 -> this.stop()
            0x37 -> this.scf()
            0x3f -> this.ccf()
            0x76 -> this.halt()
            0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x87 -> this.addAr(value1)
            0x86 -> this.addAHL()
            0xf3 -> this.di()
            0xfb -> this.ei()
            else -> println("Unknown instruction")
        }
    }
}
