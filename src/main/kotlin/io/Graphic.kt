package io

import util.Logger

@OptIn(ExperimentalUnsignedTypes::class)
class Graphic {
    /**
     * スクリーンサイズ横
     */
    private val SCREEN_WIDTH = 160
    /**
     * スクリーンサイズ縦
     */
    private val SCREEN_HEIGHT = 144

    /**
     * VRAM
     */
    private var vram: UByteArray = UByteArray(0x2000)
    /**
     * OAM
     */
    private var oam: UByteArray = UByteArray(0xa0)
    /**
     * VRAMステータス(ゲームボーイだとこれで書き込みと読み出しが合致会わないようにしている)
     */
    private var vramStatus: UByte = 0u

    /**
     * グラフィック領域から値を読み出す
     *
     * @param address 16bitアドレス
     * @return アドレスに格納されている値
     */
    fun getValue(address: UShort): UByte {
        val value = when (address) {
            in 0x8000u..0x9fffu -> {
                if (vramStatus.toInt() and 0x3 == 0x3) {
                    Logger.warn("VRAMのアクセスが許可されていないタイミングです。規定値の0xffを返します。")
                    0xffu
                } else {
                    vram[address.toInt() - 0x8000]
                }
            }
            in 0xfe00u..0xfe9fu -> {
                if (vramStatus.toInt() and 0x3 == 0x0 || vramStatus.toInt() and 0x3 == 0x1) {
                    Logger.warn("VRAMのアクセスが許可されていないタイミングです。規定値の0xffを返します。")
                    0xffu
                } else {
                    oam[address.toInt() - 0xfe00]
                }
            }
            in 0xff41u..0xff41u -> vramStatus
            else -> {
                Logger.warn("存在しないGraphicIO領域からの読み出しです。規定値の0xffを返します。")
                0xffu
            }
        }
        return value
    }

    /**
     * グラフィック領域に値を書き込む
     *
     * @param address 16bitアドレス
     * @param sourceValue 書き込む値
     */
    fun setValue(address: UShort, sourceValue: UByte) {
        when (address) {
            in 0x8000u..0x9fffu -> vram[address.toInt() - 0x8000] = sourceValue
            in 0xfe00u..0xfe9fu -> oam[address.toInt() - 0xfe00] = sourceValue
            in 0xff41u..0xff41u -> vramStatus = sourceValue
            else -> {
                Logger.warn("存在しないGraphicIO領域への書き込みです。")
            }
        }
    }

}
