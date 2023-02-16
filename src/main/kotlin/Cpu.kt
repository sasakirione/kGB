
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
        print("A: ${registerA.toString(16)}, ")
        print("B: ${registerB.toString(16)}, ")
        print("C: ${registerC.toString(16)}, ")
        print("D: ${registerD.toString(16)}, ")
        print("E: ${registerE.toString(16)},")
        print("H: ${registerH.toString(16)}, ")
        println("L: ${registerL.toString(16)}")
        print("SP: ${registerSP.toString(16)}, ")
        println("PC: ${registerPC.toString(16)}")
        print("Z: ${getFlagZ().toString(16)}, ")
        print("N: ${getFlagN().toString(16)}, ")
        print("H: ${getFlagH().toString(16)}, ")
        println("C: ${getFlagC().toString(16)}")
        println("--------------------------------------------------------")
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
        val value = readMemoryFrom8BitOfInstructions()
        if (value != 0x00u.toUByte()) {
            throw Exception("Unknown value: $value")
        }
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

    /**
     * BCレジスタに格納されたアドレスの値をインクリメントする
     */
    private fun incBC() {
        val sourceAddress = registerB * 16u + registerC
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        val result = sourceValue + 1u
        chipset.setValue(sourceAddress.toUShort(), result.toUByte())
        tickFourCycle()
    }

    /**
     * DEレジスタに格納されたアドレスの値をインクリメントする
     */
    private fun incDE() {
        val sourceAddress = registerD * 16u + registerE
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        val result = sourceValue + 1u
        chipset.setValue(sourceAddress.toUShort(), result.toUByte())
        tickFourCycle()
    }

    /**
     * HLレジスタに格納されたアドレスの値をインクリメントする
     */
    private fun incHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        val result = sourceValue + 1u
        chipset.setValue(sourceAddress.toUShort(), result.toUByte())
        tickFourCycle()
    }

    /**
     * SPレジスタに格納されたアドレスの値をインクリメントする
     */
    private fun incSP() {
        val sourceAddress = registerSP
        val sourceValue = chipset.getValue(sourceAddress)
        val result = sourceValue + 1u
        chipset.setValue(sourceAddress, result.toUByte())
        tickFourCycle()
    }

    /**
     * BCレジスタに格納されたアドレスの値をデクリメントする
     */
    private fun decBC() {
        val sourceAddress = registerB * 16u + registerC
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        val result = sourceValue - 1u
        chipset.setValue(sourceAddress.toUShort(), result.toUByte())
        tickFourCycle()
    }

    /**
     * DEレジスタに格納されたアドレスの値をデクリメントする
     */
    private fun decDE() {
        val sourceAddress = registerD * 16u + registerE
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        val result = sourceValue - 1u
        chipset.setValue(sourceAddress.toUShort(), result.toUByte())
        tickFourCycle()
    }

    /**
     * HLレジスタに格納されたアドレスの値をデクリメントする
     */
    private fun decHL() {
        val sourceAddress = registerH * 16u + registerL
        val sourceValue = chipset.getValue(sourceAddress.toUShort())
        val result = sourceValue - 1u
        chipset.setValue(sourceAddress.toUShort(), result.toUByte())
        tickFourCycle()
    }

    /**
     * SPレジスタに格納されたアドレスの値をデクリメントする
     */
    private fun decSP() {
        val sourceAddress = registerSP
        val sourceValue = chipset.getValue(sourceAddress)
        val result = sourceValue - 1u
        chipset.setValue(sourceAddress, result.toUByte())
        tickFourCycle()
    }

    /**
     * Bレジスタの値をインクリメントする
     */
    private fun incB() {
        val result = registerB + 1u
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerB and 0xfu) + 1u > 0xfu
        registerB = result.toUByte()
    }

    /**
     * Cレジスタの値をインクリメントする
     */
    private fun incC() {
        val result = registerC + 1u
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerC and 0xfu) + 1u > 0xfu
        registerC = result.toUByte()
    }

    /**
     * Dレジスタの値をインクリメントする
     */
    private fun incD() {
        val result = registerD + 1u
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerD and 0xfu) + 1u > 0xfu
        registerD = result.toUByte()
    }

    /**
     * Eレジスタの値をインクリメントする
     */
    private fun incE() {
        val result = registerE + 1u
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerE and 0xfu) + 1u > 0xfu
        registerE = result.toUByte()
    }

    /**
     * Hレジスタの値をインクリメントする
     */
    private fun incH() {
        val result = registerH + 1u
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerH and 0xfu) + 1u > 0xfu
        registerH = result.toUByte()
    }

    /**
     * Lレジスタの値をインクリメントする
     */
    private fun incL() {
        val result = registerL + 1u
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerL and 0xfu) + 1u > 0xfu
        registerL = result.toUByte()
    }

    /**
     * Aレジスタの値をインクリメントする
     */
    private fun incA() {
        val result = registerA + 1u
        flagZero = result == 0x0u
        flagN = false
        flagH = (registerA and 0xfu) + 1u > 0xfu
        registerA = result.toUByte()
    }

    /**
     * HLレジスタの値をインクリメントする
     */
    private fun incHL2() {
        val hl = registerH * 16u + registerL
        val value = hl + 1u
        registerH = (value / 16u).toUByte()
        registerL = (value % 16u).toUByte()
        tickFourCycle()
        tickFourCycle()
    }

    /**
     * Bレジスタの値をデクリメントする
     */
    private fun decB() {
        val result = registerB - 1u
        flagZero = result == 0x0u
        flagN = true
        flagH = (registerB and 0xfu) - 1u < 0x0u
        registerB = result.toUByte()
    }

    /**
     * Cレジスタの値をデクリメントする
     */
    private fun decC() {
        val result = registerC - 1u
        flagZero = result == 0x0u
        flagN = true
        flagH = (registerC and 0xfu) - 1u < 0x0u
        registerC = result.toUByte()
    }

    /**
     * Dレジスタの値をデクリメントする
     */
    private fun decD() {
        val result = registerD - 1u
        flagZero = result == 0x0u
        flagN = true
        flagH = (registerD and 0xfu) - 1u < 0x0u
        registerD = result.toUByte()
    }

    /**
     * Eレジスタの値をデクリメントする
     */
    private fun decE() {
        val result = registerE - 1u
        flagZero = result == 0x0u
        flagN = true
        flagH = (registerE and 0xfu) - 1u < 0x0u
        registerE = result.toUByte()
    }

    /**
     * Hレジスタの値をデクリメントする
     */
    private fun decH() {
        val result = registerH - 1u
        flagZero = result == 0x0u
        flagN = true
        flagH = (registerH and 0xfu) - 1u < 0x0u
        registerH = result.toUByte()
    }

    /**
     * Lレジスタの値をデクリメントする
     */
    private fun decL() {
        val result = registerL - 1u
        flagZero = result == 0x0u
        flagN = true
        flagH = (registerL and 0xfu) - 1u < 0x0u
        registerL = result.toUByte()
    }

    /**
     * Aレジスタの値をデクリメントする
     */
    private fun decA() {
        val result = registerA - 1u
        flagZero = result == 0x0u
        flagN = true
        flagH = (registerA and 0xfu) - 1u < 0x0u
        registerA = result.toUByte()
    }

    /**
     * HLレジスタの値をデクリメントする
     */
    private fun decHL2() {
        val hl = registerH * 16u + registerL
        val value = hl - 1u
        registerH = (value / 16u).toUByte()
        registerL = (value % 16u).toUByte()
        tickFourCycle()
        tickFourCycle()
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
     * 入力されたレジスタの値をHLレジスタのアドレスに格納する
     *
     * @param sss 入力されたレジスタのアドレス
     */
    private fun ldHLr(sss: UByte) {
        val sourceValue = getValueFromSSS(sss)
        val destinationAddress = registerH * 16u + registerL
        chipset.setValue(destinationAddress.toUShort(), sourceValue)
        tickFourCycle()
    }

    /**
     * 入力された値をHLレジスタのアドレスに格納する
     */
    private fun ldHLN() {
        val sourceValue = readMemoryFrom8BitOfInstructions()
        val destinationAddress = registerH * 16u + registerL
        chipset.setValue(destinationAddress.toUShort(), sourceValue)
        tickFourCycle()
        tickFourCycle()
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

    /**
     * Aレジスタに格納された値をBCレジスタに格納されたアドレスに格納する
     */
    private fun ldBCA() {
        val destinationAddress = registerB * 16u + registerC
        chipset.setValue(destinationAddress.toUShort(), registerA)
        tickFourCycle()
    }

    /**
     * Aレジスタに格納された値をDEレジスタに格納されたアドレスに格納する
     */
    private fun ldDEA() {
        val destinationAddress = registerD * 16u + registerE
        chipset.setValue(destinationAddress.toUShort(), registerA)
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
     * 入力されたアドレスにジャンプする
     */
    private fun jpNN() {
        val sourceAddress1 = readMemoryFrom8BitOfInstructions()
        val sourceAddress2 = readMemoryFrom8BitOfInstructions()
        registerPC = (sourceAddress1 * 16u + sourceAddress2).toUShort()
        tickFourCycle()
        tickFourCycle()
        tickFourCycle()
    }

    /**
     * HLレジスタに格納されているアドレスにジャンプする
     */
    private fun jpHL() {
        val sourceAddress = registerH * 16u + registerL
        registerPC = sourceAddress.toUShort()
    }

    /**
     * 指定されたアドレスの命令を呼び出す
     */
    private fun callNN() {
        val sourceAddress1 = readMemoryFrom8BitOfInstructions()
        val sourceAddress2 = readMemoryFrom8BitOfInstructions()
        val sourceAddress = sourceAddress1 * 16u + sourceAddress2
        registerSP = (registerSP - 2u).toUShort()
        chipset.setValue(registerSP, registerPC.toUByte())
        registerPC = sourceAddress.toUShort()
        tickFourCycle()
        tickFourCycle()
        tickFourCycle()
        tickFourCycle()
        tickFourCycle()
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
            0x02 -> this.ldBCA()
            0x03 -> this.incBC()
            0x04 -> this.incB()
            0x05 -> this.decB()
            0x0a -> this.ldABC()
            0x0b -> this.decBC()
            0x0c -> this.incC()
            0x0d -> this.decC()
            0x10 -> this.stop()
            0x12 -> this.ldDEA()
            0x13 -> this.incDE()
            0x14 -> this.incD()
            0x15 -> this.decD()
            0x1a -> this.ldADE()
            0x1b -> this.decDE()
            0x1c -> this.incE()
            0x1d -> this.decE()
            0x23 -> this.incHL()
            0x24 -> this.incH()
            0x25 -> this.decH()
            0x2b -> this.decHL()
            0x2c -> this.incL()
            0x2d -> this.decL()
            0x33 -> this.incSP()
            0x34 -> this.incHL2()
            0x35 -> this.decHL2()
            0x36 -> this.ldHLN()
            0x37 -> this.scf()
            0x3b -> this.decSP()
            0x3c -> this.incA()
            0x3d -> this.decA()
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
            0xc3 -> this.jpNN()
            0xcd -> this.callNN()
            0xe9 -> this.jpHL()
            0xf3 -> this.di()
            0xf9 -> this.ldSPHL()
            0xfb -> this.ei()
            else -> {
                error("存在しない命令です: $instruction")
            }
        }
        tickFourCycle()
    }

    private fun tickFourCycle() {
        tick = (tick + 4u).toUByte()
    }
}
