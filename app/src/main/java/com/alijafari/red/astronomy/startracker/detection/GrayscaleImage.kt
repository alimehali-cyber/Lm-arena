package com.alijafari.red.astronomy.startracker.detection

/**
 * Pure data structure for grayscale image, no Android dependency.
 * Holds pixel intensities as FloatArray (0..255 or any range, but typically 0..255).
 * Row-major: index = y * width + x
 */
class GrayscaleImage(
    val width: Int,
    val height: Int,
    val data: FloatArray
) {
    init {
        require(width > 0 && height > 0) { "width and height must be >0" }
        require(data.size == width * height) { "data size ${data.size} != ${width*height}" }
    }

    fun get(x: Int, y: Int): Float {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0f
        return data[y * width + x]
    }

    fun set(x: Int, y: Int, value: Float) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        data[y * width + x] = value
    }

    fun add(x: Int, y: Int, delta: Float) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        data[y * width + x] += delta
    }

    fun fill(value: Float) {
        data.fill(value)
    }

    fun copy(): GrayscaleImage {
        return GrayscaleImage(width, height, data.clone())
    }

    /** Clip values to [min, max] */
    fun clip(min: Float, max: Float) {
        for (i in data.indices) {
            data[i] = data[i].coerceIn(min, max)
        }
    }

    /** Compute min, max, mean */
    fun stats(): ImageStats {
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        var sum = 0.0
        for (v in data) {
            if (v < min) min = v
            if (v > max) max = v
            sum += v
        }
        return ImageStats(min, max, (sum / data.size).toFloat())
    }

    companion object {
        fun create(width: Int, height: Int, initial: Float = 0f): GrayscaleImage {
            return GrayscaleImage(width, height, FloatArray(width * height) { initial })
        }
    }
}

data class ImageStats(val min: Float, val max: Float, val mean: Float)
