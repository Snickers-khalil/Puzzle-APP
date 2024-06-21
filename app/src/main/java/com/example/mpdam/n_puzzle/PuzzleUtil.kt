package com.example.mpdam.n_puzzle

import java.util.*
import java.util.Collections.swap
import kotlin.math.abs


class PuzzleUtil {

    companion object {

        private val manhattan: HashMap<Int, Int> = HashMap()

        fun isSolvable(puzzleState: ArrayList<Int>, blankTileMarker: Int): Boolean {
            return countInversions(puzzleState, blankTileMarker) % 2 == 0
        }

        private fun countInversions(puzzleState: ArrayList<Int>, blankTileMarker: Int): Int {
            var numInversions = 0

            for (i in 0 until puzzleState.size - 1) {
                for (j in i + 1 until puzzleState.size) {
                    /* Blank tiles are not considered when counting the number of inversions. */
                    if (!isBlankTile(i, puzzleState, blankTileMarker)
                        && !isBlankTile(j, puzzleState, blankTileMarker)
                        && puzzleState[i] > puzzleState[j]
                    ) {
                        numInversions++
                    }
                }
            }

            return numInversions
        }

        fun swapTiles(puzzleState: ArrayList<Int>, blankTileMarker: Int) {
            var position = 0
            while (isBlankTile(position, puzzleState, blankTileMarker)
                || isBlankTile(position + 1, puzzleState, blankTileMarker)
            ) {
                position++
            }

            swap(puzzleState, position, position + 1)
        }

        private fun isBlankTile(
            position: Int,
            puzzleState: ArrayList<Int>,
            blankTileMarker: Int
        ): Boolean {
            return puzzleState[position] == blankTileMarker
        }

        fun getManhattan(
            puzzleState: ArrayList<Int>,
            numColumns: Int,
            blankTileMarker: Int
        ): Int {
            val hash: Int = Node.hashState(puzzleState)

            if (manhattan[hash] != null) {
                return manhattan[hash]!!
            }

            var sumManhattan = 0
            for (i in 0 until puzzleState.size) {
                if (puzzleState[i] != blankTileMarker) {
                    sumManhattan +=
                        abs(i / numColumns - puzzleState[i] / numColumns) +
                                abs(i % numColumns - puzzleState[i] % numColumns)
                }
            }

            manhattan[hash] = sumManhattan

            return sumManhattan
        }
    }
}