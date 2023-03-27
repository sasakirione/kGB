package io

import util.Logger

@OptIn(ExperimentalUnsignedTypes::class)
object Graphic {
    /**
     * スクリーンサイズ横
     */
    private const val SCREEN_WIDTH = 160
    /**
     * スクリーンサイズ縦
     */
    private const val SCREEN_HEIGHT = 144

    /**
     * VRAM
     */
    private var vram: UByteArray = UByteArray(0x2000)
    /**
     * OAM
     */
    private var oam: UByteArray = UByteArray(0xa0)
    /**
     * LCDコントロール
     */
    private var lcdControl: UByte = 0x80u
    /**
     * 現在モードでの経過クロック数
     */
    private var currentModeClock: UByte = 0u

    /**
     * グラフィック領域のイメージバッファ
     */
    var imageBuffer: UByteArray = UByteArray(SCREEN_WIDTH * SCREEN_HEIGHT)
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

    /**
     * 画面を更新する(1フレーム)
     *
     * @param tick タイマーのカウント値
     */
    fun update(tick: UByte) {
        // LCDがオフの場合は何もしない
        if (lcdControl.toInt() and 0x80 == 0) {
            return
        }

        // 現在のモードを取得
        val currentMode = vramStatus.toInt() and 0x3

        // 現在のモードでの経過クロック数を更新
        currentModeClock = (tick + currentModeClock).toUByte()

        // 現在のモードでの経過クロック数がモードによって異なる場合はモードを変更する
        when (currentMode) {
            // H-Blank
            0 -> {
                if (currentModeClock.toInt() >= 204) {
                    currentModeClock = 0u
                    vramStatus = (vramStatus.toInt() and 0xfc or 0x2).toUByte()
                }
            }
            // V-Blank
            1 -> {
                if (currentModeClock.toInt() >= 456) {
                    currentModeClock = 0u
                    vramStatus = (vramStatus.toInt() and 0xfc or 0x3).toUByte()
                }
            }
            // OAM
            2 -> {
                if (currentModeClock.toInt() >= 80) {
                    currentModeClock = 0u
                    vramStatus = (vramStatus.toInt() and 0xfc or 0x0).toUByte()
                }
            }
            // VRAM
            3 -> {
                if (currentModeClock.toInt() >= 172) {
                    currentModeClock = 0u
                    vramStatus = (vramStatus.toInt() and 0xfc or 0x0).toUByte()
                }
            }
        }
    }


}
