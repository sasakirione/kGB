package io

interface IO {
    fun getValue(address: UShort): UByte
    fun setValue(address: UShort, sourceValue: UByte)
    fun refresh(tick: UByte)
}