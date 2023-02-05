
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

    // 算術命令
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

    /**
     * Aレジスタに入力された値を加算する
     *
     * @param sourceValue 加算する値
     */
    private fun addA(sourceValue: UByte) {
        val result = registerA + sourceValue
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerA and 0xfu) + (sourceValue and 0xfu) > 0xfu
        flagCarry = result > 0xffu
        registerA = result.toUByte()
    }

    /**
     * Aレジスタに入力されたレジスタの値を繰り上がり加算する
     * (桁数の大きい計算を桁ごとで分割して行う時に前回計算した時の繰り上がりを考慮できる)
     *
     * @param sss 加算するレジスタのアドレス+8
     */
    private fun adcAr(sss: UByte) {
        val realSSS = sss - 8u
        val sourceValue = getValueFromSSS(realSSS.toUByte())
        adcA(sourceValue)
    }

    /**
     * AレジスタにHLレジスタに格納されたアドレスの値を繰り上がり加算する
     * (桁数の大きい計算を桁ごとで分割して行う時に前回計算した時の繰り上がりを考慮できる)
     */
    private fun adcAHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = memory.getValue(sourceAddress.toUShort())
        adcA(sourceValue)
    }

    /**
     * Aレジスタに入力された値を繰り上がり加算する
     * (桁数の大きい計算を桁ごとで分割して行う時に前回計算した時の繰り上がりを考慮できる)
     */
    private fun adcA(sourceValue: UByte) {
        val result = registerA + sourceValue + if (flagCarry) 1u else 0u
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerA and 0xfu) + (sourceValue and 0xfu) > 0xfu
        flagCarry = result > 0xffu
        registerA = result.toUByte()
    }

    /**
     * Aレジスタに入力されたレジスタの値を減算する
     *
     * @param sss 減算するレジスタのアドレス
     */
    private fun subAr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        subA(sourceValue)
    }

    /**
     * AレジスタにHLレジスタに格納されたアドレスの値を減算する
     */
    private fun subAHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = memory.getValue(sourceAddress.toUShort())
        subA(sourceValue)
    }

    /**
     * Aレジスタに入力された値を減算する
     *
     * @param sourceValue 減算する値
     */
    private fun subA(sourceValue: UByte) {
        val result = registerA - sourceValue
        flagZero = result == 0x0u
        flagN = true
        flagH = (registerA and 0xfu) - (sourceValue and 0xfu) < 0x0u
        flagCarry = result < 0x0u
        registerA = result.toUByte()
    }

    /**
     * Aレジスタに入力されたレジスタの値を論理積する
     *
     * @param sss 論理積するレジスタのアドレス
     */
    private fun andAr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        andA(sourceValue)
    }

    /**
     * AレジスタにHLレジスタに格納されたアドレスの値を論理積する
     */
    private fun andAHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = memory.getValue(sourceAddress.toUShort())
        andA(sourceValue)
    }

    /**
     * Aレジスタに入力された値を論理積する
     *
     * @param sourceValue 論理積する値
     */
    private fun andA(sourceValue: UByte) {
        val result = registerA and sourceValue
        flagZero = result == 0x0u.toUByte()
        flagN = false
        flagH = true
        flagCarry = false
        registerA = result
    }

    // 8bit ロード命令
    /**
     * BCレジスタに格納されているアドレスの値をAレジスタにロードする
     */
    private fun ldABC() {
        val sourceAddress = registerB * 16u + registerC
        registerA = memory.getValue(sourceAddress.toUShort())
    }

    /**
     * DEレジスタに格納されているアドレスの値をAレジスタにロードする
     */
    private fun ldADE() {
        val sourceAddress = registerD * 16u + registerE
        registerA = memory.getValue(sourceAddress.toUShort())
    }

    // 16bit ロード命令
    /**
     * HLレジスタにSPレジスタの値をロードする
     */
    private fun ldSPHL() {
        val sourceAddress = registerH * 16u + registerL
        registerSP = sourceAddress.toUShort()
    }

    // ジャンプ命令
    /**
     * HLレジスタに格納されているアドレスにジャンプする
     */
    private fun jpHL() {
        val sourceAddress = registerH * 16u + registerL
        registerPC = sourceAddress.toUShort()
    }

    // 命令を振り分けるアレアレアレ
    /**
     * 命令を実行する
     */
    fun execInstructions() {
        val instruction = readMemoryFrom8Bit()
        val value1 = instruction and 7u
        //val value2 = (instruction shr 3u) and 7u

        when (instruction.toInt()) {
            0x00 -> this.nop()
            0x0a -> this.ldABC()
            0x10 -> this.stop()
            0x1a -> this.ldADE()
            0x37 -> this.scf()
            0x3f -> this.ccf()
            0x76 -> this.halt()
            0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x87 -> this.addAr(value1)
            0x86 -> this.addAHL()
            0x88, 0x89, 0x8a, 0x8b, 0x8c, 0x8d, 0x8f -> this.adcAr(value1)
            0x8e -> this.adcAHL()
            0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x97 -> this.subAr(value1)
            0x96 -> this.subAHL()
            0xa0, 0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa7 -> this.andAr(value1)
            0xa6 -> this.andAHL()
            0xe9 -> this.jpHL()
            0xf3 -> this.di()
            0xf9 -> this.ldSPHL()
            0xfb -> this.ei()
            else -> println("Unknown instruction")
        }
    }
}
