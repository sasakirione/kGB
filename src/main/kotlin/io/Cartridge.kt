package io

import util.Const.ROM_FOLDER
import util.Const.SAVE_FOLDER
import util.Logger.trace
import java.io.File

@OptIn(ExperimentalUnsignedTypes::class)
class Cartridge(romName: String): IO {
    private val rom: UByteArray = UByteArray(0x8000)
    private var ram: UByteArray = UByteArray(0x2000)
    private val romPath: String = "$ROM_FOLDER/$romName.gb"
    private val ramPath: String = "$SAVE_FOLDER/$romName.sav"

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
    }


    override fun getValue(address: UShort): UByte {
        val value = when (address) {
            in 0x0000u..0x7fffu -> rom[address.toInt()]
            in 0xa000u..0xbfffu -> ram[address.toInt() - 0xa000]
            else -> {
                throw Exception("カートリッジの存在しない領域を読み込もうとしています。")
            }
        }
        return value
    }

    override fun setValue(address: UShort, sourceValue: UByte) {
        when (address) {
            in 0x0000u..0x7fffu -> rom[address.toInt()] = sourceValue
            in 0xa000u..0xbfffu -> ram[address.toInt() - 0xa000] = sourceValue
            else -> {
                throw Exception("カートリッジの存在しない領域に書き込もうとしています。")
            }
        }
    }

    override fun refresh(tick: UByte) {
        trace(tick.toString())
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
}