package io

import util.Const.ROM_FOLDER
import util.Const.SAVE_FOLDER
import util.Logger.info
import java.io.File

@OptIn(ExperimentalUnsignedTypes::class)
class Cartridge(romName: String) {
    private val rom: UByteArray = UByteArray(0x8000)
    private var ram: UByteArray = UByteArray(0x2000)
    private val romPath: String = "$ROM_FOLDER/$romName.gb"
    private val ramPath: String = "$SAVE_FOLDER/$romName.sav"
    val loadingRomName: String

    init {
        // カートリッジの読み込み
        val romFile = File(romPath)
        if (!romFile.exists()) {
            throw Exception("ROMファイルが存在しません。")
        }
        val romStream = romFile.inputStream()
        romStream.read(rom.asByteArray())
        romStream.close()
        // セーブデータの読み込み
        val ramFile = File(ramPath)
        if (ramFile.exists()) {
            val ramStream = ramFile.inputStream()
            ramStream.read(ram.asByteArray())
            ramStream.close()
        }
        // カートリッジのタイトルを取得
        val title = String(rom.copyOfRange(0x134, 0x143).map { it.toByte() }.toByteArray(), Charsets.US_ASCII)
        info("カートリッジのタイトル: $title")
        loadingRomName = title
    }


    /**
     * カートリッジからアドレスの値を読み出す
     *
     * @param address アドレス
     * @return アドレスに格納されている値
     */
    fun getValue(address: UShort): UByte {
        val value = when (address) {
            in 0x0000u..0x7fffu -> rom[address.toInt()]
            in 0xa000u..0xbfffu -> ram[address.toInt() - 0xa000]
            else -> {
                throw Exception("カートリッジの存在しない領域を読み込もうとしています。")
            }
        }
        return value
    }

    /**
     * カートリッジのアドレスに値を書き込む
     *
     * @param address アドレス
     * @param sourceValue 書き込む値
     */
    fun setValue(address: UShort, sourceValue: UByte) {
        when (address) {
            in 0x0000u..0x7fffu -> rom[address.toInt()] = sourceValue
            in 0xa000u..0xbfffu -> ram[address.toInt() - 0xa000] = sourceValue
            else -> {
                throw Exception("カートリッジの存在しない領域に書き込もうとしています。")
            }
        }
    }

    /**
     * セーブデータをファイルに書き込む
     */
    fun writeSaveData() {
        val ramFile = File(ramPath)
        if (!ramFile.exists()) {
            ramFile.createNewFile()
        }
        val ramStream = ramFile.outputStream()
        ramStream.write(ram.asByteArray())
        ramStream.close()
    }

    fun getLogoData(): UByteArray {
        return rom.copyOfRange(0x104, 0x133)
    }
}