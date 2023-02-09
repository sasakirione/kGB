
class Cpu(private val chipset: Chipset){
    private var registerA: UByte = 0x00u
    private var registerB: UByte = 0x00u
    private var registerC: UByte = 0x00u
    private var registerD: UByte = 0x00u
    private var registerE: UByte = 0x00u
    private var registerH: UByte = 0x00u
    private var registerL: UByte = 0x00u
    private var registerSP: UShort = 0x0000u
    private var registerPC: UShort = 0x0100u

    private var tick: UByte = 0x00u

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
    private fun readMemoryFrom8BitOfInstructions(): UByte {
        val value = chipset.getValue(registerPC)
        pcPlusOne()
        return value
    }

    /**
     * メモリから16bitで値を読み込む
     *
     * @return メモリから読み込んだ値(16bit)
     */
    private fun readMemoryFrom16Bit(): UShort {
        val value = chipset.getValue(registerPC)
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

    // CPU制御命令
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
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        addA(sourceValue)
        tickFourCycle()
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
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        adcA(sourceValue)
        tickFourCycle()
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
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        subA(sourceValue)
        tickFourCycle()
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
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        andA(sourceValue)
        tickFourCycle()
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
     * Bレジスタに入力されたレジスタの値を格納する
     *
     * @param sss 格納するレジスタのアドレス
     */
    private fun ldBr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        registerB = sourceValue
    }

    /**
     * BレジスタにHLレジスタに格納されたアドレスの値を格納する
     */
    private fun ldBHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = getValueFromSSS(sourceAddress.toUByte())
        registerB = sourceValue
        tickFourCycle()
    }

    /**
     * Cレジスタに入力されたレジスタの値を格納する
     *
     * @param sss 格納するレジスタのアドレス
     */
    private fun ldCr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        registerC = sourceValue
    }

    /**
     * CレジスタにHLレジスタに格納されたアドレスの値を格納する
     */
    private fun ldCHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = getValueFromSSS(sourceAddress.toUByte())
        registerC = sourceValue
        tickFourCycle()
    }

    /**
     * Dレジスタに入力されたレジスタの値を格納する
     *
     * @param sss 格納するレジスタのアドレス
     */
    private fun ldDr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        registerD = sourceValue
    }

    /**
     * DレジスタにHLレジスタに格納されたアドレスの値を格納する
     */
    private fun ldDHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = getValueFromSSS(sourceAddress.toUByte())
        registerD = sourceValue
        tickFourCycle()
    }

    /**
     * Eレジスタに入力されたレジスタの値を格納する
     *
     * @param sss 格納するレジスタのアドレス
     */
    private fun ldEr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        registerE = sourceValue
    }

    /**
     * EレジスタにHLレジスタに格納されたアドレスの値を格納する
     */
    private fun ldEHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = getValueFromSSS(sourceAddress.toUByte())
        registerE = sourceValue
        tickFourCycle()
    }

    /**
     * Hレジスタに入力されたレジスタの値を格納する
     *
     * @param sss 格納するレジスタのアドレス
     */
    private fun ldHr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        registerH = sourceValue
    }

    /**
     * HレジスタにHLレジスタに格納されたアドレスの値を格納する
     */
    private fun ldHHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = getValueFromSSS(sourceAddress.toUByte())
        registerH = sourceValue
        tickFourCycle()
    }

    /**
     * Lレジスタに入力されたレジスタの値を格納する
     *
     * @param sss 格納するレジスタのアドレス
     */
    private fun ldLr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        registerL = sourceValue
    }

    /**
     * LレジスタにHLレジスタに格納されたアドレスの値を格納する
     */
    private fun ldLHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = getValueFromSSS(sourceAddress.toUByte())
        registerL = sourceValue
        tickFourCycle()
    }

    /**
     * HLレジスタに入力されたレジスタの値を格納する
     *
     * @param sss 格納するレジスタのアドレス
     */
    private fun ldHLr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        val destinationAddress = registerH * 16u + registerL
        chipset.setValue(destinationAddress.toUShort(), sourceValue)
    }

    /**
     * HLレジスタに入力されたレジスタの値を格納する
     *
     * @param sss 格納するレジスタのアドレス
     */
    private fun ldAr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        registerA = sourceValue
    }

    /**
     * AレジスタにHLレジスタに格納されたアドレスの値を格納する
     */
    private fun ldAHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = getValueFromSSS(sourceAddress.toUByte())
        registerA = sourceValue
        tickFourCycle()
    }

    /**
     * BCレジスタに格納されているアドレスの値をAレジスタにロードする
     */
    private fun ldABC() {
        val sourceAddress = registerB * 16u + registerC
        registerA = chipset.getValue(sourceAddress.toUShort())
        tickFourCycle()
    }

    /**
     * DEレジスタに格納されているアドレスの値をAレジスタにロードする
     */
    private fun ldADE() {
        val sourceAddress = registerD * 16u + registerE
        registerA = chipset.getValue(sourceAddress.toUShort())
        tickFourCycle()
    }

    // 16bit ロード命令
    /**
     * HLレジスタにSPレジスタの値をロードする
     */
    private fun ldSPHL() {
        val sourceAddress = registerH * 16u + registerL
        registerSP = sourceAddress.toUShort()
        tickFourCycle()
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
        val instruction = readMemoryFrom8BitOfInstructions()
        val value1 = instruction and 7u

        when (instruction.toInt()) {
            0x00 -> this.nop()
            0x0a -> this.ldABC()
            0x10 -> this.stop()
            0x1a -> this.ldADE()
            0x37 -> this.scf()
            0x3f -> this.ccf()
            0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x47 -> this.ldBr(value1)
            0x46 -> this.ldBHL()
            0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4f -> this.ldCr(value1)
            0x4e -> this.ldCHL()
            0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x57 -> this.ldDr(value1)
            0x56 -> this.ldDHL()
            0x58, 0x59, 0x5a, 0x5b, 0x5c, 0x5d, 0x5f -> this.ldEr(value1)
            0x5e -> this.ldEHL()
            0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x67 -> this.ldHr(value1)
            0x66 -> this.ldHHL()
            0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6f -> this.ldLr(value1)
            0x6e -> this.ldLHL()
            0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x77 -> this.ldHLr(value1)
            0x76 -> this.halt()
            0x78, 0x79, 0x7a, 0x7b, 0x7c, 0x7d, 0x7f -> this.ldAr(value1)
            0x7e -> this.ldAHL()
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
        tickFourCycle()
    }

    private fun tickFourCycle() {
        tick = (tick + 4u).toUByte()
    }
}
