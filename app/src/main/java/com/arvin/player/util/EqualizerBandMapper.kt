package com.arvin.player.util

/**
 * Maps a 10-value UI gain array (millibels) onto however many native EQ bands
 * the device actually exposes (often 5 or 6). Pure function — no Android dependencies —
 * so the mapping math can be unit tested without a real Equalizer effect instance.
 */
object EqualizerBandMapper {
    fun mapToNativeBands(tenBandMb: List<Short>, nativeBandCount: Int): List<Short> {
        if (nativeBandCount <= 0 || tenBandMb.isEmpty()) return emptyList()
        return (0 until nativeBandCount).map { i ->
            val sourceIndex = (i * (tenBandMb.size - 1) / (nativeBandCount - 1).coerceAtLeast(1))
                .coerceIn(0, tenBandMb.size - 1)
            tenBandMb[sourceIndex]
        }
    }
}
