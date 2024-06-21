package com.example.mpdam.n_puzzle


enum class SolveStatus(val successMessageId: Int) {

    USER_SOLVED(R.string.user_solved),

    FEWEST_MOVES(R.string.fewest_score),

    FASTEST_TIME(R.string.fastest_score),

    FEWEST_AND_FASTEST(R.string.high_score),

    COMPUTER_SOLVED(R.string.computer_solved);
}