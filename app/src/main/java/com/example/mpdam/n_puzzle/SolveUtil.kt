package com.example.mpdam.n_puzzle

import java.util.*
import java.util.Collections.swap
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class SolveUtil {

    companion object {
        private const val MAX_NUM_CHILDREN = 4
        private const val FRONTIER_INITIAL_CAPACITY = 11
        private val childPositions: ArrayList<ArrayList<Int>> = getChildPositions()

        fun solve(
            puzzleStatePair: StatePair,
            goalPuzzleState: ArrayList<Int>,
            numColumns: Int,
            blankTileMarker: Int
        ): Stack<StatePair>? {
            val puzzleState: ArrayList<Int> = puzzleStatePair.puzzleState
            val blankTilePos: Int = puzzleStatePair.blankTilePos

            val frontier: PriorityQueue<Node> =
                PriorityQueue(FRONTIER_INITIAL_CAPACITY, NodeByF())
            val frontierMap: HashMap<Int, Node> = HashMap()

            val explored: HashSet<ArrayList<Int>> = HashSet()

            val startNode = Node(
                StatePair(puzzleState, blankTilePos),
                null,
                0,
                PuzzleUtil.getManhattan(puzzleState, numColumns, blankTileMarker)
            )
            frontier.add(startNode)
            frontierMap[startNode.hashCode()] = startNode

            while (frontier.isNotEmpty()) {

                val node: Node = frontier.poll()!!

                if (isSolved(node.puzzleStatePair.puzzleState, goalPuzzleState)) {
                    return backtrackPath(node)
                }

                explored.add(node.puzzleStatePair.puzzleState)

                val childNodes: ArrayList<Node> = getChildNodes(
                    node,
                    node.puzzleStatePair.blankTilePos,
                    numColumns,
                    blankTileMarker
                )

                for (child in childNodes) {
                    val childHash: Int = child.hashCode()
                    val childInFrontier: Node? = frontierMap[childHash]

                    if (childInFrontier == null && child.puzzleStatePair.puzzleState !in explored) {
                        frontier.add(child)
                        frontierMap[childHash] = child
                    } else if (childInFrontier != null && childInFrontier.getF() > child.getF()) {
                        frontier.remove(childInFrontier)
                        frontier.add(child)
                        frontierMap[childHash] = child
                    }
                }
            }

            return null
        }

        fun isSolved(puzzleState: ArrayList<Int>, goalPuzzleState: ArrayList<Int>): Boolean {
            return puzzleState == goalPuzzleState
        }

        private fun backtrackPath(node: Node): Stack<StatePair> {
            val path: Stack<StatePair> = Stack()
            var current: Node? = node

            while (current != null) {
                path.push(current.puzzleStatePair)
                current = current.parent
            }

            return path
        }

        private fun getChildNodes(
            node: Node,
            blankTilePos: Int,
            numColumns: Int,
            blankTileMarker: Int
        ): ArrayList<Node> {
            val childNodes: ArrayList<Node> = ArrayList(MAX_NUM_CHILDREN)

            for (position in childPositions[blankTilePos]) {
                val childState: ArrayList<Int> = ArrayList(node.puzzleStatePair.puzzleState.size)

                for (tile in node.puzzleStatePair.puzzleState) {
                    childState.add(tile)
                }

                swap(childState, position, blankTilePos)

                childNodes.add(
                    Node(
                        StatePair(childState, position),
                        node,
                        node.g + 1,
                        PuzzleUtil.getManhattan(childState, numColumns, blankTileMarker)
                    )
                )
            }

            return childNodes
        }


        private fun getChildPositions(): ArrayList<ArrayList<Int>> {
            return arrayListOf(
                arrayListOf(1, 3),
                arrayListOf(0, 2, 4),
                arrayListOf(1, 5),
                arrayListOf(0, 4, 6),
                arrayListOf(1, 3, 5, 7),
                arrayListOf(2, 4, 8),
                arrayListOf(3, 7),
                arrayListOf(4, 6, 8),
                arrayListOf(5, 7)
            )
        }
    }
}