import io.Cartridge
import io.Graphic
import util.Logger.error
import util.Logger.warn

@OptIn(ExperimentalUnsignedTypes::class)
class Chipset(romName: String) {
    /**
     * メモリ
     */
    private var memory: UByteArray = UByteArray(0x2000)
    /**
     * Highメモリ
     */
    private var highMemory: UByteArray = UByteArray(0x7f)
    /**
     * カートリッジ
     */
    private val cartridge: Cartridge = Cartridge(romName)
    /**
     * 内部ロゴデータ
     */
    private val innerLogoData = ubyteArrayOf(0xCEu,0xEDu,0x66u,0x66u,0xCCu,0x0Du,0x00u,0x0Bu,0x03u,0x73u,0x00u,0x83u,0x00u,0x0Cu,0x00u,0x0Du,0x00u,0x08u,0x11u,0x1Fu,0x88u,0x89u,0x00u,0x0Eu,0xDCu,0xCCu,0x6Eu,0xE6u,0xDDu,0xDDu,0xD9u,0x99u,0xBBu,0xBBu,0x67u,0x63u,0x6Eu,0x0Eu,0xECu,0xCCu,0xDDu,0xDCu,0x99u,0x9Fu,0xBBu,0xB9u,0x33u)

    /**
     * 割り込み有効化レジスタ(IE)
     */
    private var interruptEnableRegister: UByte = 0u

    init {
        // カートリッジの $0104 から $0133 に配置されている任天堂ロゴデータを取得する
        val logoData = cartridge.getLogoData()

        // ロゴデータが不正ではないかチェックする
        logoData.forEachIndexed { index, value ->
            if (value != innerLogoData[index]) {
                error("カートリッジの任天堂ロゴが不正です")
                throw Exception("起動チェックエラー")
            }
        }

        // カートリッジのチェックサムをチェックする
        var sum = 0
        for (i in 0x0134..0x014D) {
            sum = (cartridge.getValue(i.toUShort()) + sum.toUInt()).toInt()
        }
        sum += 25
        if ((sum.toUByte() and 1u) != 0u.toUByte()) {
            error("カートリッジのチェックサムが一致しません。${sum.toUByte() and 1u}")
            throw Exception("起動チェックエラー")
        }
    }

    /**
     * 16bitアドレスから値を読み出す
     *
     * @param address 16bitアドレス
     * @return アドレスに格納されている値
     */
    fun getValue(address: UShort): UByte {
        val value = when (address) {
            in 0x0000u..0x7fffu -> cartridge.getValue(address)
            in 0x8000u..0x9fffu -> Graphic.getValue(address)
            in 0xa000u..0xbfffu -> cartridge.getValue(address)
            in 0xc000u..0xdfffu -> memory[address.toInt() - 0xc000]
            in 0xff80u..0xfffeu -> highMemory[address.toInt() - 0xff80]
            in 0xe000u..0xfdffu -> memory[(address.toInt() - 0xe000)]
            in 0xffffu..0xffffu -> interruptEnableRegister
            else -> {
                warn("存在しないIO領域からの読み出しです。規定値の0xffを返します。")
                0xffu
            }
        }
        return value
    }

    /**
     * 16bitアドレスに値を書き込む
     *
     * @param address 16bitアドレス
     * @param sourceValue 書き込む値
     */
    fun setValue(address: UShort, sourceValue: UByte) {
        when (address) {
            in 0x0000u..0x7fffu -> cartridge.setValue(address, sourceValue)
            in 0x8000u..0x9fffu -> Graphic.setValue(address, sourceValue)
            in 0xa000u..0xbfffu -> cartridge.setValue(address, sourceValue)
            in 0xc000u..0xdfffu -> memory[address.toInt() - 0xc000] = sourceValue
            in 0xff80u..0xfffeu -> highMemory[address.toInt() - 0xff80] = sourceValue
            in 0xe000u..0xfdffu -> {
                warn("この領域はRAMのミラーにアクセスするための領域です。この領域への書き込みは任天堂により明示的に禁止されています。")
                memory[address.toInt() - (0xe000+0x2000)] = sourceValue
            }
            in 0xffffu..0xffffu -> interruptEnableRegister = sourceValue
            else -> {
                warn("存在しないIO領域への書き込みです。")
            }
        }
    }

    /**
     * カートリッジの名前を取得する
     *
     * @return カートリッジの名前
     */
    fun getCartridgeName(): String {
        return cartridge.loadingRomName
    }

    fun save() {
        cartridge.writeSaveData()
    }
}
