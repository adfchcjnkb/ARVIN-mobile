package com.arvin.player.util

import org.junit.Assert.assertEquals
import org.junit.Test

class EqualizerBandMapperTest {

    private val tenBands: List<Short> = (0 until 10).map { (it * 100).toShort() } // 0,100,200,...,900

    @Test
    fun `maps ten bands down to five native bands`() {
        val mapped = EqualizerBandMapper.mapToNativeBands(tenBands, 5)
        assertEquals(5, mapped.size)
        // First and last native bands should align with first and last UI bands
        assertEquals(tenBands.first(), mapped.first())
        assertEquals(tenBands.last(), mapped.last())
    }

    @Test
    fun `maps ten bands to exactly ten native bands unchanged`() {
        val mapped = EqualizerBandMapper.mapToNativeBands(tenBands, 10)
        assertEquals(tenBands, mapped)
    }

    @Test
    fun `single native band takes the first ui band`() {
        val mapped = EqualizerBandMapper.mapToNativeBands(tenBands, 1)
        assertEquals(1, mapped.size)
        assertEquals(tenBands.first(), mapped.first())
    }

    @Test
    fun `zero native bands returns empty list`() {
        assertEquals(emptyList<Short>(), EqualizerBandMapper.mapToNativeBands(tenBands, 0))
    }

    @Test
    fun `empty input returns empty list`() {
        assertEquals(emptyList<Short>(), EqualizerBandMapper.mapToNativeBands(emptyList(), 5))
    }
}
