package com.example.mpdam.n_puzzle

data class Node(
    val puzzleStatePair: StatePair,
    val parent: Node?,
    var g: Int,
    private var h: Int
) {

    companion object {

        fun hashState(puzzleState: ArrayList<Int>): Int {
            var hash = 0
            for (tile in puzzleState) {
                hash = hash * 10 + tile
            }

            return hash
        }
    }

    fun getF(): Int {
        return g + h
    }

    override fun equals(other: Any?): Boolean {
        return this.puzzleStatePair.puzzleState == (other as Node).puzzleStatePair.puzzleState
    }

    override fun hashCode(): Int {
        return hashState(puzzleStatePair.puzzleState)
    }
}