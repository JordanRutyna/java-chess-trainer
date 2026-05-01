# java-chess-trainer

A Java chess application built from scratch with a bitboard engine, 2-player mode, AI opponent, and an opening repertoire trainer.

## Overview

This project is a full rebuild of an earlier piece-per-class chess implementation, redesigned around a clean three-layer architecture and a bitboard engine verified by perft testing. The goal was to produce something fast, maintainable, and actually useful for studying chess openings.

## Features

- 2-player mode with full legal move highlighting and drag-and-drop piece movement
- Legal move generation verified against standard perft node counts up to depth 5
- PGN notation in the move list with check and checkmate indicators
- Last move highlighting and king-in-check indicator
- Opening repertoire trainer: save any position as a named line with notes, stored in a local JSON file

## Architecture

The project is divided into three layers with strict separation of concerns.

**Core engine** (`src/core/`) handles all chess logic with no UI or I/O dependencies. Board state is represented as 12 bitboards (one per piece type per color), move generation produces fully legal moves using a pseudo-legal filter, and all moves are packed into a single int for efficiency.

**Application layer** (`src/app/`) orchestrates game flow. A `Player` interface allows human and AI players to be swapped freely. The game loop runs on a background thread so the UI stays responsive. `MoveHistory` supports undo via copy-make snapshots.

**UI layer** (`src/ui/`) is pure Swing rendering and input handling. It communicates with the application layer through a listener interface and never touches the core engine directly.

**Openings module** (`src/openings/`) tracks the current move sequence and saves named lines with notes to a JSON file.

## Project Structure

```
java-chess-trainer/
├── src/
│   ├── core/
│   │   ├── BitBoard.java
│   │   ├── GameState.java
│   │   ├── MoveEncoder.java
│   │   ├── MoveGenerator.java
│   │   ├── Evaluator.java
│   │   ├── ZobristHasher.java
│   │   └── Perft.java
│   ├── app/
│   │   ├── Player.java
│   │   ├── HumanPlayer.java
│   │   ├── AIPlayer.java
│   │   ├── GameController.java
│   │   ├── MoveHistory.java
│   │   └── PgnUtil.java
│   ├── openings/
│   │   ├── OpeningLine.java
│   │   ├── OpeningBook.java
│   │   └── OpeningTrainer.java
│   └── ui/
│       ├── MainWindow.java
│       ├── BoardPanel.java
│       ├── InfoPanel.java
│       ├── StartPanel.java
│       ├── OpeningPanel.java
│       └── PieceRenderer.java
├── resources/
│   └── (piece images)
├── data/
│   └── openings.json
├── out/
└── run.sh
```

## Running the Project

From the project root:

```bash
chmod +x run.sh   # first time only
./run.sh
```

This compiles all source files into `out/` and launches the application.

## Perft Results

Move generation is verified against known node counts from the standard starting position:

| Depth | Nodes     |
|-------|-----------|
| 1     | 20        |
| 2     | 400       |
| 3     | 8,902     |
| 4     | 197,281   |
| 5     | 4,865,609 |

## Roadmap

- AI player using minimax with alpha-beta pruning and a transposition table
- Opening trainer playback mode: load a saved line and practice against it
- PGN import and export
- Start menu with player and difficulty selection

## Built With

- Java (no external dependencies)
- Swing for the UI
- Bitboard representation for the chess engine

## Background

This is a ground-up rewrite of three earlier attempts at a Java chess application. Those versions used a piece-per-class architecture that made the AI search impractically slow and the codebase difficult to extend. This version separates concerns cleanly, uses bitboards for performance, and verifies correctness before building any UI.