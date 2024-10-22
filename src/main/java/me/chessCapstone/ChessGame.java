package me.chessCapstone;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.geometry.Point2D;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.geometry.Insets;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ChessGame extends Application {
    private static final int TILE_SIZE = 100;
    private static final int BOARD_SIZE = 8;
    private boolean isWhiteTurn = true;

    HashMap<String, Piece> pieces = new HashMap<>();
    private Board boardCurrent;
    private ArrayList<Tile> squaresThreatenedByWhite = new ArrayList<>();
    private ArrayList<Tile> squaresThreatenedByBlack = new ArrayList<>();


    private int initialPieceCoordinateROW;
    private int initialPieceCoordinateCOL;

    private ImageView selectedPiece = null;

    private GridPane gridPane;


    ///stockfish_______________________________________________________stockfish
    private ChessEngine engine;



    /////////////////////////////// GLOBAL VARIABLES ///////////////////////////////
    //variable to add new piece upon promotion, so that board state has unique value
    //if not 'queenwhite' will be made upon promotion instead of "queen3white"
    //problem occurs if multiple pawns promote to queen (ie, "queenwhite" then "queenwhite")
    //thus need global for now
    int globalCountForPromotion = 2;

    static boolean lastMoveWasDoublePawnMove = false;
    static String lastPawnMoved = "";
    static boolean EnPassantPossible = false;
    /////////////////////////////// GLOBAL VARIABLES ///////////////////////////////
    private Map<String, ImageView> imageViewMap = new HashMap<>();
    private Map<String, Integer> positionCounts = new HashMap<>();
    private int[] lastWhiteKingPos = new int[2], lastBlackKingPos = new int[2];
    private King whiteKing,  blackKing;
    private Tile enPassantTile = new Tile(-1, -1);
    private int halfMoveClock = 0, numberOfMoves = 1, stockfishDepth = 5;;
    private boolean playAgainstStockfish = false;



    @Override
    public void start(Stage primaryStage) {
        gridPane = new GridPane();
        boardCurrent = new Board(gridPane);
        setUpPieces(gridPane);

        // 배열 초기화
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (boardCurrent.get(col, row) == null) {
                    boardCurrent.set(col, row, "null");
                }
            }
        }

        //just to initialize kings to their respective variables in the boardCurrent
        initializeGame();

        gridPane.setOnMousePressed(event -> {
            int col = (int) (event.getSceneX() / TILE_SIZE);
            int row = (int) (event.getSceneY() / TILE_SIZE);
            initialPieceCoordinateCOL = col;
            initialPieceCoordinateROW = row;

            String typeOfPiece = boardCurrent.get(col, row);
            Piece piece = pieces.get(typeOfPiece);
            ArrayList<Tile> threatenedSquares;
            calculateThreatenedSquares();

            if ((isWhiteTurn && typeOfPiece.contains("white")) || (!isWhiteTurn && typeOfPiece.contains("black"))) {
                selectedPiece = imageViewMap.get(typeOfPiece);



                if (selectedPiece != null) {
                    // Store the initial mouse position
                    selectedPiece.setUserData(new Point2D(event.getSceneX(), event.getSceneY()));
                    // Bring the selected piece to the front
                    selectedPiece.toFront();
                }

                if(isWhiteTurn) {
                    threatenedSquares = squaresThreatenedByBlack;
                } else {
                    threatenedSquares = squaresThreatenedByWhite;
                }



                piece.highlightValidMoves(boardCurrent.getStiles(), boardCurrent.getBoard(), threatenedSquares, this);

            }
        });

        Scene scene = new Scene(gridPane, TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE);

        scene.setOnMouseDragged(event -> {
            if (selectedPiece != null) {
                Point2D initialPos = (Point2D) selectedPiece.getUserData();
                double deltaX = event.getSceneX() - initialPos.getX();
                double deltaY = event.getSceneY() - initialPos.getY();
                selectedPiece.setTranslateX(deltaX);
                selectedPiece.setTranslateY(deltaY);
            }
        });

        scene.setOnMouseReleased(event -> {
            if (selectedPiece != null) {
                // Reset the translation
                selectedPiece.setTranslateX(0);
                selectedPiece.setTranslateY(0);

                String typeOfPiece = boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW);
                //checking whose turn, in case of wrong click
                if ((isWhiteTurn && typeOfPiece.contains("white")) || (!isWhiteTurn && typeOfPiece.contains("black"))) {

                    if (selectedPiece != null) {

                        selectedPiece.setLayoutX(0);
                        selectedPiece.setLayoutY(0);
                        Piece piece = pieces.get(boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW));
                        boolean validMove = false;
                        ArrayList<Tile> threatenedSquares;
                        boardCurrent.resetTileColor();

                        // convert mouse coordinates to local Gridpane coordinates
                        Point2D localPoint = gridPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                        double x = localPoint.getX();
                        double y = localPoint.getY();

                        int col = (int) (x / TILE_SIZE);
                        int row = (int) (y / TILE_SIZE);

                        if (col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE) {

                            String pieceType = typeOfPiece.replaceAll("\\d", "");

                            //checks if any possible move is valid in regards to check
                            validMove = simulateMoveProtectKing(piece, col, row);

                            if (validMove) {
                                //Check for castling
                                if (pieceType.contains("king") && Math.abs(col - initialPieceCoordinateCOL) == 2) {
                                    King king = (King) piece;
                                    if (king.isCastlingValid(col, boardCurrent.getBoard(), isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite)) {
                                        handleCastling(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row);
                                    } else {
                                        // Invalid castling, return king to original position
                                        gridPane.getChildren().remove(selectedPiece);
                                        gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                                        return;
                                    }
                                }

                                if (pieceType.contains("pawn")) {
                                    handlePawnMove(pieceType, row, initialPieceCoordinateROW, col);
                                }

                                //Move from current position to new spot
                                gridPane.getChildren().remove(selectedPiece);
                                gridPane.add(selectedPiece, col, row);
                                piece.setCol(col);
                                piece.setRow(row);

                                // update boardCurrent
                                String currentPiece = boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW);
                                // String destinationPiece = boardCurrent[col][row];

                                //to capture piece.
                                String toBeRemoved = boardCurrent.get(col, row);
                                if (!toBeRemoved.equals("null")) {
                                    gridPane.getChildren().remove(imageViewMap.get(toBeRemoved));
                                    imageViewMap.remove(toBeRemoved);
                                }

                                boardCurrent.set(initialPieceCoordinateCOL, initialPieceCoordinateROW,"null");
                                boardCurrent.set(col, row, currentPiece);

                                //check and handle pawn promotion
                                if (currentPiece.contains("pawn")) {
                                    boolean isWhite = currentPiece.contains("white");
                                    if ((isWhite && row == 0) || (!isWhite && row == 7)) {
                                        handlePawnPromotion(currentPiece, col, row, isWhite);
                                    }
                                }

                                // Update piece's moved status
                                piece.setMoved(true);

                                if(!isWhiteTurn) {
                                    numberOfMoves++;
                                }

                                if(currentPiece.contains("pawn") || !toBeRemoved.equals("null")) {
                                    halfMoveClock = 0;
                                } else {
                                    halfMoveClock++;
                                }

                                switchTurn();
                                piece.setMoved(true);
                                calculateThreatenedSquares();

                                if (isCheckmate()) {
                                    //Handle checkmate
                                    System.out.println(isWhiteTurn ? "Black wins by checkmate!" : "White wins by checkmate!");
                                }

                            }else {
                                //if move invalid, return to last position
                                gridPane.getChildren().remove(selectedPiece);
                                gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                            }
                        } else {
                            //if move outside the board, return to last position
                            gridPane.getChildren().remove(selectedPiece);
                            gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                        }

                        //reset initial coordinate
                        initialPieceCoordinateROW = -1;
                        initialPieceCoordinateCOL = -1;

                        //printBoardState();
                        calculateThreatenedSquares();
                        selectedPiece = null; // Reset selected piece

                    }


                }
            }
            System.out.println(boardCurrent.boardToFEN(pieces, isWhiteTurn, enPassantTile, halfMoveClock, numberOfMoves));
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("Chess Game");
        primaryStage.show();

        //Show game mode selection popup
        showGameModeSelectionPopup(primaryStage);






        ///STOCKFISH_________- ___________--    ___________________________STOCKFISH

        //change this to try windows version of stockfish

        try {
            System.out.println("Attempting to initialize chess engine...");

            initializeStockfish();

            System.out.println("Chess engine initialized successfully.");


        } catch (IOException e) {
            System.err.println("Failed to initialize chess engine: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error initializing chess engine: " + e.getMessage());
            e.printStackTrace();
        }

        primaryStage.setOnCloseRequest(event -> {
            event.consume(); // Prevent the window from closing immediately
            shutdown();
        });
    }



    private void shutdown() {

        System.out.println("Exiting application...");
        Platform.exit();
        System.exit(0);
    }

    private void switchTurn() {

        isWhiteTurn = !isWhiteTurn;

        playMoveSound();
        //Updates King's square for check (red square)
        updateCheckStatus();

        if (checkForThreefoldRepetition()) {
            handleThreefoldRepetition();
        }
        if(isStalemate()) {
            handleStalemate();
        }



        if (!isWhiteTurn && playAgainstStockfish) {

            if (engine != null) {

                makeEngineMove();
                //printBoardState();
                calculateThreatenedSquares();
                //System.out.println("peices: " + pieces);
                if (isCheckmate()) {
                    System.out.println("Black wins by checkmate!");
                    //Handle end of game
                }
                switchTurn(); //Switch back to white's turn
            } else {
                System.err.println("Cannot make engine move: Chess engine is not initialized!");
            }
        } else {

            if (isCheckmate()) {
                System.out.println((isWhiteTurn ? "Black" : "White") + " wins by checkmate!");

            }
        }
    }

    private void makeEngineMove() {
        if (engine == null) {
            System.err.println("Chess engine is not initialized!");
            return;
        }

        try {
            String fen = boardToFEN();
            engine.sendCommand("position fen " + fen);
            engine.sendCommand("go depth " + stockfishDepth); //Use the selected depth

            String bestMove = getBestMoveFromEngine();
            applyEngineMove(bestMove);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    ///fen rnbkqbnr/pppppppp/8/8/8/4P3/PPPP1PPP/RNBQKBNR b KQkq - 0 1
//lowercase = black, uppercase = white
//rnbkqbnr: Black's back rank
//pppppppp: Black's pawns
//8: An entire empty rank
//8: Another empty rank
//8: A third empty rank
//4P3: Four empty squares, White pawn, three empty squares
//PPPP1PPP: White's pawns with one space (where the pawn moved from)
//RNBQKBNR: White's back rank
    private String boardToFEN() {
        StringBuilder fen = new StringBuilder();
        int emptyCount = 0;

        //Piece placement
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                String piece = boardCurrent.get(col, row);
                if (piece.equals("null")) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(pieceToFenChar(piece));
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
                emptyCount = 0;
            }
            if (row < BOARD_SIZE - 1) {
                fen.append("/");
            }
        }

        //Active color
        fen.append(isWhiteTurn ? " w " : " b ");

        //Castling availability
        String castling = getCastlingRights();
        fen.append(castling.isEmpty() ? "-" : castling).append(" ");

//Castling Availability (KQkq):
//Indicates which sides can still castle.
//K: White can castle kingside
//Q: White can castle queenside
//k: Black can castle kingside
//q: Black can castle queenside
//If no castling is available, this would be -.


        //En passant target square
        fen.append(getEnPassantSquare()).append(" ");


        //En Passant Target Square (-):
        //Indicates if an en passant capture is possible.
        // - means no en passant is possible.
        //If a pawn had just moved two squares, this would show
        //the square "behind" the pawn (e.g., e3 for a white
        //pawn that just moved to e4).


        // Halfmove clock and fullmove number
        fen.append("0 1");
        //Used for the fifty-move rule (game can be claimed as
        //drawn if no pawn has moved and no piece has
        //been captured in the last 50 moves).

//Halfmove Clock:
//Counts the number of halfmoves since the last pawn move or capture.
//Used to enforce the fifty-move rule in chess.
//Resets to 0 when a pawn moves or a piece is captured.


//Fullmove Number:
//Represents the number of completed full turns in the game.
//fen.append("0 1");), always setting 0 and 1
//For accurate FEN,  need to implement counters that track these values




        return fen.toString();
    }

    private char pieceToFenChar(String piece) {
        char fenChar = switch (piece.replaceAll("\\d", "").replace("white", "").replace("black", "")) {
            case "king" -> 'k';
            case "queen" -> 'q';
            case "rook" -> 'r';
            case "bishop" -> 'b';
            case "knight" -> 'n';
            case "pawn" -> 'p';
            default -> '.';
        };
        return piece.contains("white") ? Character.toUpperCase(fenChar) : fenChar;
    }

    private boolean hasKingMoved(String color) {
        King king = (King) pieces.get("king1" + color);
        return king != null && king.hasMoved();
    }

    private boolean hasRookMoved(String color, String side) {
        int rookCol = side.equals("kingside") ? 7 : 0;
        int rookRow = color.equals("white") ? 7 : 0;
        Piece rook = pieces.get(boardCurrent.get(rookCol, rookRow));
        return !(rook instanceof Rook) || rook.hasMoved();
    }

    private String getCastlingRights() {
        StringBuilder rights = new StringBuilder();
        if (!hasKingMoved("white") && !hasRookMoved("white", "kingside")) rights.append("K");
        if (!hasKingMoved("white") && !hasRookMoved("white", "queenside")) rights.append("Q");
        if (!hasKingMoved("black") && !hasRookMoved("black", "kingside")) rights.append("k");
        if (!hasKingMoved("black") && !hasRookMoved("black", "queenside")) rights.append("q");
        return rights.toString();
    }

    //might not work
    private String getEnPassantSquare() {
        if (lastMoveWasDoublePawnMove) {
            //Determine the en passant square based on the last move
            //This is a simplified version; you might need to adjust based on your implementation
            int col = lastPawnMoved.charAt(4) - '0';
            int row = lastPawnMoved.contains("white") ? 5 : 2;
            return "" + (char)('a' + col) + row;
        }
        return "-";
    }

    private String getBestMoveFromEngine() {
        try {
            String line;
            //Keep reading lines from the engine output
            while ((line = engine.readLine()) != null) {
                // Check if the line starts with "bestmove"
                if (line.startsWith("bestmove")) {
                    //Split the line by whitespace and return the second word
                    //The format is typically "bestmove e2e4 ponder e7e5"
                    //So we're extracting just the "e2e4" part
                    return line.split("\\s+")[1];
                }
            }
        } catch (IOException e) {
            //If there's an error reading from the engine, print the stack trace
            e.printStackTrace();
        }
        //If we've read all lines without finding a best move, return null
        return null;
    }

    private void applyEngineMove(String move) {
        try {
            System.out.println("Applying engine move: " + move);

            int startCol = move.charAt(0) - 'a';
            int startRow = '8' - move.charAt(1);
            int endCol = move.charAt(2) - 'a';
            int endRow = '8' - move.charAt(3);

            String movingPiece = boardCurrent.get(startCol, startRow);
            String capturedPiece = boardCurrent.get(endCol, endRow);

            //Handle captures
            if (!capturedPiece.equals("null")) {
                Platform.runLater(() -> {
                    ImageView capturedPieceView = imageViewMap.get(capturedPiece);
                    gridPane.getChildren().remove(capturedPieceView);
                    imageViewMap.remove(capturedPiece);
                });
            }




            //Check for pawn promotion
            boolean isPromotion = move.length() == 5 &&
                    movingPiece.contains("pawn") &&
                    (endRow == 0 || endRow == 7) &&
                    "qrbn".indexOf(move.charAt(4)) != -1;

            if (isPromotion) {
                char promotionPiece = move.charAt(4);
                handleEnginePawnPromotion(movingPiece, endCol, endRow, promotionPiece);

                boardCurrent.set(startCol, startRow,"null");

            } else {

                handleSpecialMoves(movingPiece, startCol, startRow, endCol, endRow, move.length() == 5 ? move.charAt(4) : ' ');
                final ImageView movingPieceView = imageViewMap.get(movingPiece);
                Platform.runLater(() -> {
                    gridPane.getChildren().remove(movingPieceView);
                    gridPane.add(movingPieceView, endCol, endRow);
                });

                // Update boardCurrent
                boardCurrent.set(endCol, endRow, movingPiece);
                boardCurrent.set(startCol, startRow,"null");
                // Update piece's moved status
                Piece piece = pieces.get(movingPiece);
                if (piece != null) {
                    piece.setMoved(true);
                    piece.setCol(endCol);
                    piece.setRow(endRow);
                }
            }

            //Update game state
            calculateThreatenedSquares();

        } catch (Exception e) {
            System.err.println("Error applying engine move: " + move);
            e.printStackTrace();
        }
    }

    private void updateCheckStatus() {
        boolean whiteInCheck = whiteKing.isInCheck(squaresThreatenedByBlack);
        boolean blackInCheck = blackKing.isInCheck(squaresThreatenedByWhite);

        System.out.println("White in check: " + whiteInCheck);
        System.out.println("Black in check: " + blackInCheck);

        //Update white king's tile
        updateKingTileColor(whiteKing, whiteInCheck, lastWhiteKingPos);

        //Update black king's tile
        updateKingTileColor(blackKing, blackInCheck, lastBlackKingPos);
    }


    //when a king is in check, the tile it was on last is red
    private void updateKingTileColor(King king, boolean isInCheck, int[] lastPos) {
        int col = king.getCol();
        int row = king.getRow();

        //Restore the color of the last position if it's different from the current position
        if (lastPos[0] != row || lastPos[1] != col) {
            restoreOriginalColor(lastPos[0], lastPos[1]);
        }

        StackPane kingTile = boardCurrent.getStiles()[row][col];

        if (isInCheck) {
            kingTile.setStyle("-fx-background-color: RED;");
        } else {
            restoreOriginalColor(row, col);
        }

        //Update the last position
        lastPos[0] = row;
        lastPos[1] = col;
    }
    //when a king is not in check, the tile it was on last is white or gray
    private void restoreOriginalColor(int row, int col) {
        StackPane tile = boardCurrent.getStiles()[row][col];
        boolean isLightSquare = (row + col) % 2 == 0;
        tile.setStyle(isLightSquare ? "-fx-background-color: WHITE;" : "-fx-background-color: GRAY;");
    }




    ///stockfish_______________________________________________________stockfish


    //stockfish__________helpermethods____________________________________stockfish




    private void handleSpecialMoves(String movingPiece, int startCol, int startRow, int endCol, int endRow, char promotionPiece) {
        String pieceType = movingPiece.replaceAll("\\d", "").replace("white", "").replace("black", "");

        if (pieceType.equals("pawn") && (endRow == 0 || endRow == 7)) {
            handleEnginePawnPromotion(movingPiece, endCol, endRow, promotionPiece);
        } else
        if (pieceType.equals("king") && Math.abs(endCol - startCol) == 2) {
            handleCastlingStockfish(startCol, startRow, endCol, endRow);
        } else if (pieceType.equals("pawn") && startCol != endCol && boardCurrent.get(endCol, endRow).equals("null")) {
            handleEnPassant(startCol, startRow, endCol, endRow);
        }

        //Update last move for en passant
        lastMoveWasDoublePawnMove = pieceType.equals("pawn") && Math.abs(endRow - startRow) == 2;
        if (lastMoveWasDoublePawnMove) {
            lastPawnMoved = movingPiece;
        }
    }

    private void handleEnginePawnPromotion(String currentPiece, int col, int row, char promotionPiece) {
        Platform.runLater(() -> {
            // Remove the current pawn image
            ImageView pawnView = imageViewMap.get(currentPiece);
            gridPane.getChildren().remove(pawnView);
            imageViewMap.remove(currentPiece);

            // Determine the new piece type
            String newPieceType = switch (promotionPiece) {
                case 'q' -> "queen";
                case 'r' -> "rook";
                case 'b' -> "bishop";
                case 'n' -> "knight";
                default -> "queen"; // Default to queen if something unexpected happens
            };

            globalCountForPromotion++;
            //Create new piece
            String newPieceName = newPieceType + globalCountForPromotion + "black";
            Piece promotedPiece = createPiece(newPieceType, "black");
            ImageView promotedPieceView = promotedPiece.getPiece();

            pieces.put(newPieceName, promotedPiece);

            //Add new piece
            gridPane.add(promotedPieceView, col, row);

            boardCurrent.set(col, row, newPieceName);
            promotedPiece.setCol(col);
            promotedPiece.setRow(row);
            promotedPiece.setMoved(true);

            imageViewMap.put(newPieceName, promotedPieceView);


        });
    }

    private void handleCastlingStockfish(int startCol, int startRow, int endCol, int endRow) {
        int rookStartCol = endCol > startCol ? 7 : 0;
        int rookEndCol = endCol > startCol ? endCol - 1 : endCol + 1;

        String rookPiece = boardCurrent.get(rookStartCol, startRow);
        ImageView rookView = imageViewMap.get(rookPiece);

        gridPane.getChildren().remove(rookView);
        gridPane.add(rookView, rookEndCol, startRow);

        boardCurrent.set(rookStartCol, startRow,"null");
        boardCurrent.set(rookEndCol, startRow, rookPiece);

        Piece rook = pieces.get(rookPiece);
        if (rook != null) {
            rook.setMoved(true);
        }
    }

    private void handleEnPassant(int startCol, int startRow, int endCol, int endRow) {
        int capturedPawnRow = startRow;
        String capturedPawn = boardCurrent.get(endCol, capturedPawnRow);
        ImageView capturedPawnView = imageViewMap.get(capturedPawn);

        gridPane.getChildren().remove(capturedPawnView);
        imageViewMap.remove(capturedPawn);
        boardCurrent.set(endCol, capturedPawnRow, "null");
    }



//END OF stockfish_______________________________________________________stockfish helper methods





    private void handlePawnPromotion(String currentPiece, int col, int row, boolean isWhite) {
        List<String> promotionOptions = Arrays.asList("Queen", "Rook", "Bishop", "Knight");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Queen", promotionOptions);
        dialog.setTitle("Pawn promotion");
        dialog.setHeaderText(null);
        dialog.setContentText("Choose piece you want to promote:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(choice -> {
            // remove the current pawn image
            ImageView pawnView = imageViewMap.get(currentPiece);
            gridPane.getChildren().remove(pawnView);
            imageViewMap.remove(currentPiece);

            globalCountForPromotion = globalCountForPromotion + 1;
            //create new piece
            String newPieceType = choice.toLowerCase();
            String newPieceName = newPieceType + globalCountForPromotion+ (isWhite ? "white" : "black");
            String newColor = (isWhite ? "white" : "black");
            Piece promotedPiece = createPiece(newPieceType, isWhite ? "white" : "black");
            ImageView promotedPieceView = promotedPiece.getPiece();


            pieces.put(promotedPiece.getType() +globalCountForPromotion+ newColor, promotedPiece);


            //add new piece
            gridPane.add(promotedPieceView, col, row);
            boardCurrent.set(col, row, newPieceName);
            promotedPiece.setCol(col);
            promotedPiece.setRow(row);
            imageViewMap.put(newPieceName, promotedPieceView);
        });
    }

    private void calculateThreatenedSquares() {
        squaresThreatenedByWhite.clear();
        squaresThreatenedByBlack.clear();

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = pieces.get(boardCurrent.get(col, row));
                if (piece != null) {
                    ArrayList<Tile> threatenedSquares = piece.findThreatenedSquares(boardCurrent.getBoard());
                    if (piece.getColor().equals("white")) {
                        squaresThreatenedByWhite.addAll(threatenedSquares);
                    } else {
                        squaresThreatenedByBlack.addAll(threatenedSquares);
                    }
                }
            }
        }
    }

    private void setUpPieces(GridPane gridPane) {


        String[] pieceList = {"rook1", "knight1", "bishop1", "queen1", "king1", "bishop2", "knight2", "rook2",
                "pawn0", "pawn1", "pawn2", "pawn3", "pawn4", "pawn5", "pawn6", "pawn7"};

        String[] colors = {"black", "white"};

        int row = 0, col = 0;

        for (String color : colors) {

            for (String piece : pieceList) {

                if (col == 8) {
                    if (row == 1) {
                        row = 7;
                    } else if (row == 7) {
                        row--;
                    } else {
                        row++;
                    }
                    col = 0;
                }

                Piece nextPiece = createPiece(piece, color);
                String typeColor = piece + color;
                pieces.put(typeColor, nextPiece);
                boardCurrent.set(col, row, typeColor);
                assert nextPiece != null;
                nextPiece.setCol(col);
                nextPiece.setRow(row);
                gridPane.add(nextPiece.getPiece(), col, row);
                imageViewMap.put(typeColor, nextPiece.getPiece());
                col++;

            }

        }

    }

    private Piece createPiece(String type, String color) {

        type = type.replaceAll("\\d", "");

        return switch (type) {
            case "king" -> new King(color);
            case "queen" -> new Queen(color);
            case "knight" -> new Knight(color);
            case "bishop" -> new Bishop(color);
            case "rook" -> new Rook(color);
            case "pawn" -> new Pawn(color);
            default -> null;
        };
    }

    private void handleCastling(int startCol, int startRow, int endCol, int endRow) {
        boolean isKingSide = endCol > startCol;
        int rookStartCol = isKingSide ? 7 : 0;
        int rookEndCol = isKingSide ? endCol - 1 : endCol + 1;

        // Move rook
        String rookPiece = boardCurrent.get(rookStartCol, startRow);
        ImageView rookView = imageViewMap.get(rookPiece);
        gridPane.getChildren().remove(rookView);
        gridPane.add(rookView, rookEndCol, startRow);
        boardCurrent.set(rookStartCol, startRow,"null");
        boardCurrent.set(rookEndCol, startRow, rookPiece);

        //Update rook's position and moved status
        Rook rook = (Rook) pieces.get(rookPiece);
        rook.handleCastlingMove(rookEndCol);
    }

    private void initializeGame() {

        whiteKing = (King) pieces.get("king1white");
        blackKing = (King) pieces.get("king1black");
        calculateThreatenedSquares();
    }

    public boolean simulateMoveProtectKing(Piece piece, int endCol, int endRow) {
        int startCol = piece.getCol();
        int startRow = piece.getRow();

        //Store the original state
        String movingPiece = boardCurrent.get(startCol, startRow);
        String capturedPiece = boardCurrent.get(endCol, endRow);

        //Simulate move
        boardCurrent.set(endCol, endRow, movingPiece);
        boardCurrent.set(startCol, startRow,"null");
        piece.setCol(endCol);
        piece.setRow(endRow);

        calculateThreatenedSquares();

        //Check for check
        boolean kingInCheck = (isWhiteTurn ? whiteKing : blackKing).isInCheck(
                isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite);

        //Revert move
        boardCurrent.set(startCol, startRow, movingPiece);
        boardCurrent.set(endCol, endRow, capturedPiece);
        piece.setCol(startCol);
        piece.setRow(startRow);

        calculateThreatenedSquares();

        return !kingInCheck && piece.isValidMove(endCol, endRow, boardCurrent.getBoard(),
                isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite);
    }

    private boolean isCheckmate() {
        King currentKing = isWhiteTurn ? whiteKing : blackKing;
        String currentColor = isWhiteTurn ? "white" : "black";
        ArrayList<Tile> threatenedSquares = isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite;

        if (!currentKing.isInCheck(threatenedSquares)) {
            return false;
        }



        //Check if the king can move away
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                if (simulateMoveProtectKing(currentKing, col, row)) {
                    return false; // King can escape, not checkmate
                }
            }
        }

        //Check if any piece can block the check or capture the attacking piece
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                String pieceKey = boardCurrent.get(col, row);
                if (!pieceKey.equals("null") && pieceKey.contains(currentColor)) {
                    Piece piece = pieces.get(pieceKey);
                    if (piece != null) {
                        for (int newCol = 0; newCol < BOARD_SIZE; newCol++) {
                            for (int newRow = 0; newRow < BOARD_SIZE; newRow++) {
                                if (simulateMoveProtectKing(piece, newCol, newRow)) {
                                    return false; // Found a move that prevents checkmate
                                }
                            }
                        }
                    }
                }
            }
        }

        // It's checkmate, show popup
        Platform.runLater(() -> {
            String winningColor = isWhiteTurn ? "Black" : "White";
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Checkmate");
            alert.setHeaderText(null);
            alert.setContentText("CHECKMATE! " + winningColor + " wins!");
            alert.showAndWait();

            handleGameEnd("Great Game!");
        });

        return true;
    }





    private void handlePawnMove(String pieceType, int row, int initialPieceCoordinateROW, int col) {
        // Check for double pawn move (en passant)
        if (Math.abs(row - initialPieceCoordinateROW) == 2) {
            lastMoveWasDoublePawnMove = true;
            lastPawnMoved = boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW);
            enPassantTile.setCol(lastPawnMoved.charAt(4) - '0');
            enPassantTile.setRow(lastPawnMoved.contains("white") ? 3 : 5);
        } else {
            lastMoveWasDoublePawnMove = false;
            enPassantTile.setCol(-1);
            enPassantTile.setRow(-1);
        }

        // Check for en passant capture
        if (EnPassantPossible) {
            int capturedPawnRow = pieceType.contains("white") ? row + 1 : row - 1;

            // Remove the captured pawn from the board
            boardCurrent.set(col, capturedPawnRow, "null");

            // Remove the captured pawn's image from the GUI
            ImageView capturedPawnView = imageViewMap.get(lastPawnMoved);
            if (capturedPawnView != null) {
                gridPane.getChildren().remove(capturedPawnView);
                imageViewMap.remove(lastPawnMoved);
            }
        }
    }

    //Initialize Stockfish engine
    private Path initializeStockfishPath(String stockfishRelativePath) throws IOException {
        //First, try to load from resources
        InputStream inputStream = getClass().getResourceAsStream("/" + stockfishRelativePath);
        if (inputStream != null) {
            Path tempDir = Files.createTempDirectory("stockfish");
            Path tempFile = tempDir.resolve("stockfish");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().setExecutable(true);

            return tempFile;
        }

        //If not found in resources, try to find it in the project directory
        String projectDir = System.getProperty("user.dir");
        Path resourcesPath = Paths.get(projectDir, "src", "main", "resources");
        Path stockfishPath = resourcesPath.resolve(stockfishRelativePath);

        File stockfishFile = stockfishPath.toFile();
        if (!stockfishFile.exists()) {
            throw new FileNotFoundException("Stockfish binary not found at: " + stockfishPath);
        }
        if (!stockfishFile.canExecute()) {
            stockfishFile.setExecutable(true);
        }

        return stockfishPath;
    }


    private void initializeStockfish() throws IOException {
        String stockfishRelativePath;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            //For Windows
            //More compatible: Is a bit slower, but works on the vast majority of computers.
            //stockfish-windows-x86-64-sse41-popcnt.exe
            //Faster: Works on modern computers.
            //stockfish-windows-x86-64-avx2.exe

            //Choose one of these based on your preference and system compatibility
            stockfishRelativePath = "stockfish-windows-x86-64-avx2.exe";
            //or
            //stockfishRelativePath = "stockfish-windows-x86-64-sse41-popcnt.exe";
        } else {
            //For macOS or other systems
            stockfishRelativePath = "stockfish-macos-m1-apple-silicon";
        }

        Path stockfishPath = initializeStockfishPath(stockfishRelativePath);

        System.out.println("Stockfish path: " + stockfishPath);

        engine = new ChessEngine(stockfishPath.toString());
    }


    //Screens to select game mode and difficulty
    private void showGameModeSelectionPopup(Stage primaryStage) {
        try {
            initializeStockfish();
        } catch (IOException e) {
            System.err.println("Failed to initialize Stockfish: " + e.getMessage());
            // Optionally, show an error dialog to the user
        }
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(primaryStage);
        popupStage.setTitle("Game Setup");

        VBox vbox = new VBox(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(30));

        //Game mode selection
        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton oneVsOneButton = createModeButton("1 vs 1", modeGroup);
        ToggleButton stockfishButton = createModeButton("Play against Stockfish", modeGroup);
        VBox modeBox = new VBox(20, oneVsOneButton, stockfishButton);
        modeBox.setAlignment(Pos.CENTER);

        //Difficulty selection (initially hidden)
        ToggleGroup difficultyGroup = new ToggleGroup();
        ToggleButton easyButton = createDifficultyButton("Easy", 1, difficultyGroup);
        ToggleButton mediumButton = createDifficultyButton("Medium", 7, difficultyGroup);
        ToggleButton hardButton = createDifficultyButton("Hard", 10, difficultyGroup);
        VBox difficultyBox = new VBox(20, easyButton, mediumButton, hardButton);
        difficultyBox.setAlignment(Pos.CENTER);
        difficultyBox.setVisible(false);

        vbox.getChildren().addAll(modeBox, difficultyBox);

        Scene popupScene = new Scene(vbox, 300, 400);
        popupStage.setScene(popupScene);

        //Set up button actions
        oneVsOneButton.setOnAction(e -> {
            playAgainstStockfish = false;
            popupStage.close();
        });

        stockfishButton.setOnAction(e -> {
            playAgainstStockfish = true;
            modeBox.setVisible(false);
            difficultyBox.setVisible(true);
            popupStage.setTitle("Select Difficulty");
        });

        difficultyGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                stockfishDepth = (int) newValue.getUserData();
                popupStage.close();
            }
        });

        popupStage.showAndWait();
    }

    private ToggleButton createModeButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setPrefSize(200, 50);
        button.setStyle("-fx-font-size: 16px;");
        return button;
    }

    private ToggleButton createDifficultyButton(String text, int depth, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(group);
        button.setUserData(depth);
        button.setPrefSize(200, 50);
        button.setStyle("-fx-font-size: 16px;");
        return button;
    }






    // Although a threefold repetition usually occurs after
    // consecutive moves, there is no requirement that the moves
    // be consecutive for a claim to
    // be valid. The rule applies to positions, not moves.

    //draw methods
    private String getPositionKey() {
        StringBuilder sb = new StringBuilder();

        //Append the entire board state
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                sb.append(boardCurrent.get(col, row)).append("|");
            }
        }

        //Append whose turn it is
        sb.append(isWhiteTurn ? "w" : "b");

        return sb.toString();
    }

    private boolean checkForThreefoldRepetition() {
        String positionKey = getPositionKey();
        int count = positionCounts.getOrDefault(positionKey, 0) + 1;
        positionCounts.put(positionKey, count);
        return count >= 3;
    }

    private void handleThreefoldRepetition() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Draw");
            alert.setHeaderText(null);
            alert.setContentText("DRAW!");
            alert.showAndWait();
            handleGameEnd("Better luck next time!");
        });
    }

    //Stalemate
    private boolean isStalemate() {
        if (hasNoLegalMoves()) {
            King currentKing = isWhiteTurn ? whiteKing : blackKing;
            ArrayList<Tile> threatenedSquares = isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite;
            return !currentKing.isInCheck(threatenedSquares);
        }
        return false;
    }

    private boolean hasNoLegalMoves() {
        String currentColor = isWhiteTurn ? "white" : "black";

        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                String pieceKey = boardCurrent.get(col, row);
                if (!pieceKey.equals("null") && pieceKey.contains(currentColor)) {
                    Piece piece = pieces.get(pieceKey);
                    if (piece != null) {
                        for (int newCol = 0; newCol < BOARD_SIZE; newCol++) {
                            for (int newRow = 0; newRow < BOARD_SIZE; newRow++) {
                                if (simulateMoveProtectKing(piece, newCol, newRow)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private void handleStalemate() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Stalemate");
            alert.setHeaderText(null);
            alert.setContentText("STALEMATE!");
            alert.showAndWait();
            handleGameEnd("Better luck next time!");
        });
    }


    private void restartApplication(Stage currentStage) {
        Platform.runLater(() -> {
            try {
                ChessGame newInstance = new ChessGame();
                newInstance.start(currentStage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }




    private void handleGameEnd(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText(message + "\nDo you want to play again?");

            ButtonType playAgainButton = new ButtonType("Play Again");
            ButtonType exitButton = new ButtonType("Exit");
            alert.getButtonTypes().setAll(playAgainButton, exitButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == playAgainButton) {
                Stage next = new Stage();
                restartApplication(next);
            } else {
                Platform.exit();
                System.exit(0);
            }
        });
    }


    private void playMoveSound() {
        try {
            String soundFile = "/move_sound.wav";
            Media sound = new Media(getClass().getResource(soundFile).toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();


        } catch (Exception e) {
            System.err.println("Error playing move sound: " + e.getMessage());

        }
    }



    public static void main(String[] args) {

        launch(args);
    }


}
