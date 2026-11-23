package com.example.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.abs

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code encoder (Byte mode, ECC Level M, Versions 2..8).
 * Produces deterministic, scannable QR codes for UPI payment URIs without external dependencies.
 */
object QrCodeGenerator {

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val totalCodewords: Int,
        val ecPerBlock: Int,
        val group1Blocks: Int,
        val group1Data: Int,
        val group2Blocks: Int,
        val group2Data: Int,
        val remainderBits: Int,
        val alignments: IntArray
    )

    private val VERSIONS = listOf(
        VersionSpec(2, 25, 44, 16, 1, 28, 0, 0, 7, intArrayOf(6, 18)),
        VersionSpec(3, 29, 70, 26, 1, 44, 0, 0, 7, intArrayOf(6, 22)),
        VersionSpec(4, 33, 100, 18, 2, 32, 0, 0, 7, intArrayOf(6, 26)),
        VersionSpec(5, 37, 134, 24, 2, 43, 0, 0, 7, intArrayOf(6, 30)),
        VersionSpec(6, 41, 172, 16, 4, 27, 0, 0, 7, intArrayOf(6, 34)),
        VersionSpec(7, 45, 196, 18, 4, 31, 0, 0, 0, intArrayOf(6, 22, 38)),
        VersionSpec(8, 49, 242, 22, 2, 38, 2, 39, 0, intArrayOf(6, 24, 42))
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if (x and 0x100 != 0) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    fun generateQrBitmap(content: String, pixelSize: Int = 512): ImageBitmap {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val spec = VERSIONS.firstOrNull { v ->
            val totalDataBytes = v.group1Blocks * v.group1Data + v.group2Blocks * v.group2Data
            // 4-bit mode + 8-bit count = 12 bits (1.5 bytes) -> 2 bytes overhead conservative
            bytes.size + 2 <= totalDataBytes
        } ?: VERSIONS.last()

        val totalDataCodewords = spec.group1Blocks * spec.group1Data + spec.group2Blocks * spec.group2Data
        val payloadBytes = bytes.copyOf(minOf(bytes.size, totalDataCodewords - 2))

        // Build bitstream: Mode (0100), Byte Count (8 bits for V1..V9), Data, Terminator, Pad
        val bits = ArrayList<Int>(totalDataCodewords * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        appendBits(0b0100, 4)
        appendBits(payloadBytes.size, 8)
        for (b in payloadBytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }
        val maxDataBits = totalDataCodewords * 8
        val terminator = minOf(4, maxDataBits - bits.size)
        repeat(terminator) { bits.add(0) }
        while (bits.size % 8 != 0) bits.add(0)
        var padIndex = 0
        while (bits.size < maxDataBits) {
            appendBits(if (padIndex % 2 == 0) 0xEC else 0x11, 8)
            padIndex++
        }

        val dataCodewords = IntArray(totalDataCodewords) { idx ->
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bits[idx * 8 + b]
            }
            v
        }

        // Split into blocks & compute Reed-Solomon EC codewords
        val dataBlocks = ArrayList<IntArray>()
        val ecBlocks = ArrayList<IntArray>()
        var offset = 0
        repeat(spec.group1Blocks) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.group1Data)
            offset += spec.group1Data
            dataBlocks.add(block)
            ecBlocks.add(computeReedSolomon(block, spec.ecPerBlock))
        }
        repeat(spec.group2Blocks) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.group2Data)
            offset += spec.group2Data
            dataBlocks.add(block)
            ecBlocks.add(computeReedSolomon(block, spec.ecPerBlock))
        }

        // Interleave data and EC codewords
        val finalBits = ArrayList<Int>(spec.totalCodewords * 8 + spec.remainderBits)
        fun appendCodeword(cw: Int) {
            for (i in 7 downTo 0) finalBits.add((cw ushr i) and 1)
        }
        val maxBlockLen = maxOf(spec.group1Data, spec.group2Data)
        for (i in 0 until maxBlockLen) {
            for (b in dataBlocks) {
                if (i < b.size) appendCodeword(b[i])
            }
        }
        for (i in 0 until spec.ecPerBlock) {
            for (b in ecBlocks) {
                appendCodeword(b[i])
            }
        }
        repeat(spec.remainderBits) { finalBits.add(0) }

        val n = spec.size
        val baseModules = Array(n) { IntArray(n) { -1 } } // -1 = unset, 0 = white, 1 = black
        val isFunction = Array(n) { BooleanArray(n) }

        fun setFunction(r: Int, c: Int, black: Boolean) {
            baseModules[r][c] = if (black) 1 else 0
            isFunction[r][c] = true
        }

        fun drawFinder(top: Int, left: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = top + dr
                    val c = left + dc
                    if (r in 0 until n && c in 0 until n) {
                        val inOuter = dr in 0..6 && dc in 0..6
                        val black = inOuter && (
                            dr == 0 || dr == 6 || dc == 0 || dc == 6 ||
                                (dr in 2..4 && dc in 2..4)
                            )
                        setFunction(r, c, black)
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, n - 7)
        drawFinder(n - 7, 0)

        // Timing patterns
        for (i in 8 until n - 8) {
            val black = (i % 2 == 0)
            setFunction(6, i, black)
            setFunction(i, 6, black)
        }

        // Alignment patterns
        for (rCenter in spec.alignments) {
            for (cCenter in spec.alignments) {
                if (isFunction[rCenter][cCenter]) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val black = maxOf(abs(dr), abs(dc)) != 1
                        setFunction(rCenter + dr, cCenter + dc, black)
                    }
                }
            }
        }

        // Dark module
        setFunction(n - 8, 8, true)

        // Reserve format info areas
        for (i in 0..8) {
            if (i != 6) {
                isFunction[8][i] = true
                isFunction[i][8] = true
            }
        }
        for (i in 0..7) {
            isFunction[8][n - 1 - i] = true
        }
        for (i in 0..6) {
            isFunction[n - 1 - i][8] = true
        }

        // Version info for V7+
        if (spec.version >= 7) {
            val verBits = computeVersionBits(spec.version)
            for (i in 0 until 18) {
                val black = ((verBits ushr i) and 1) == 1
                val a = n - 11 + (i % 3)
                val b = i / 3
                setFunction(a, b, black)
                setFunction(b, a, black)
            }
        }

        // Place data bits in zig-zag order
        var bitIdx = 0
        var col = n - 1
        var upward = true
        while (col > 0) {
            if (col == 6) col--
            for (rowOffset in 0 until n) {
                val r = if (upward) n - 1 - rowOffset else rowOffset
                for (dc in 0..1) {
                    val c = col - dc
                    if (!isFunction[r][c]) {
                        val bit = if (bitIdx < finalBits.size) finalBits[bitIdx++] else 0
                        baseModules[r][c] = bit
                    }
                }
            }
            col -= 2
            upward = !upward
        }

        // Evaluate all 8 masks and choose lowest penalty
        var bestMatrix = baseModules
        var bestPenalty = Int.MAX_VALUE
        for (mask in 0 until 8) {
            val candidate = Array(n) { r ->
                IntArray(n) { c ->
                    if (isFunction[r][c]) baseModules[r][c]
                    else baseModules[r][c] xor (if (maskBit(mask, r, c)) 1 else 0)
                }
            }
            applyFormatBits(candidate, n, mask)
            val penalty = scorePenalty(candidate, n)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMatrix = candidate
            }
        }

        // Render crisp QR bitmap with 2-module quiet zone
        val quiet = 2
        val totalModules = n + quiet * 2
        val scale = maxOf(4, pixelSize / totalModules)
        val bmpDim = totalModules * scale
        val pixels = IntArray(bmpDim * bmpDim)
        val white = android.graphics.Color.WHITE
        val black = android.graphics.Color.BLACK
        pixels.fill(white)

        for (r in 0 until n) {
            for (c in 0 until n) {
                if (bestMatrix[r][c] == 1) {
                    val startY = (r + quiet) * scale
                    val startX = (c + quiet) * scale
                    for (dy in 0 until scale) {
                        val rowBase = (startY + dy) * bmpDim + startX
                        for (dx in 0 until scale) {
                            pixels[rowBase + dx] = black
                        }
                    }
                }
            }
        }

        val bitmap = Bitmap.createBitmap(bmpDim, bmpDim, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, bmpDim, 0, 0, bmpDim, bmpDim)
        return bitmap.asImageBitmap()
    }

    private fun maskBit(mask: Int, r: Int, c: Int): Boolean = when (mask) {
        0 -> (r + c) % 2 == 0
        1 -> r % 2 == 0
        2 -> c % 3 == 0
        3 -> (r + c) % 3 == 0
        4 -> (r / 2 + c / 3) % 2 == 0
        5 -> (r * c) % 2 + (r * c) % 3 == 0
        6 -> ((r * c) % 2 + (r * c) % 3) % 2 == 0
        else -> ((r + c) % 2 + (r * c) % 3) % 2 == 0
    }

    private fun applyFormatBits(matrix: Array<IntArray>, n: Int, mask: Int) {
        // ECC Level M = 00
        val data = mask and 0x7
        var rem = data shl 10
        for (i in 4 downTo 0) {
            if (((rem ushr (i + 10)) and 1) != 0) {
                rem = rem xor (0x537 shl i)
            }
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        for (i in 0..5) {
            val v = (bits ushr i) and 1
            matrix[8][i] = v
        }
        matrix[8][7] = (bits ushr 6) and 1
        matrix[8][8] = (bits ushr 7) and 1
        matrix[7][8] = (bits ushr 8) and 1
        for (i in 9..14) {
            matrix[14 - i][8] = (bits ushr i) and 1
        }

        for (i in 0..7) {
            matrix[n - 1 - i][8] = (bits ushr i) and 1
        }
        for (i in 8..14) {
            matrix[8][n - 15 + i] = (bits ushr i) and 1
        }
    }

    private fun computeVersionBits(version: Int): Int {
        var rem = version shl 12
        for (i in 5 downTo 0) {
            if (((rem ushr (i + 12)) and 1) != 0) {
                rem = rem xor (0x1F25 shl i)
            }
        }
        return (version shl 12) or rem
    }

    private fun computeReedSolomon(data: IntArray, ecCount: Int): IntArray {
        var gen = intArrayOf(1)
        for (i in 0 until ecCount) {
            val next = IntArray(gen.size + 1)
            val root = GF_EXP[i]
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], root)
            }
            gen = next
        }
        val rem = IntArray(ecCount)
        for (d in data) {
            val factor = d xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecCount - 1)
            rem[ecCount - 1] = 0
            for (i in 0 until ecCount) {
                rem[i] = rem[i] xor gfMul(gen[i + 1], factor)
            }
        }
        return rem
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun scorePenalty(m: Array<IntArray>, n: Int): Int {
        var penalty = 0
        for (r in 0 until n) {
            var runLen = 1
            for (c in 1 until n) {
                if (m[r][c] == m[r][c - 1]) runLen++
                else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        for (c in 0 until n) {
            var runLen = 1
            for (r in 1 until n) {
                if (m[r][c] == m[r - 1][c]) runLen++
                else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        for (r in 0 until n - 1) {
            for (c in 0 until n - 1) {
                val v = m[r][c]
                if (v == m[r][c + 1] && v == m[r + 1][c] && v == m[r + 1][c + 1]) {
                    penalty += 3
                }
            }
        }
        return penalty
    }
}
