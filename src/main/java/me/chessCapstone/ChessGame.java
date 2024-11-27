package me.chessCapstone;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ChessGame extends Application {

    private ArrayList<Tile> squaresThreatenedByWhite = new ArrayList<>(), squaresThreatenedByBlack = new ArrayList<>(); //List of tiles threatened by white and black pieces
    private HashMap<String, Piece> pieces = new HashMap<>(); //Map containing all chess pieces by type

    private Board boardCurrent; //Current state of chessboard
    private ImageView selectedPiece = null; //Currently selected piece being moved
    private GridPane gridPane;  //GridPane used to render the chessboard
    private static final int TILE_SIZE = 100, BOARD_SIZE = 8;   //Size of each tile and the board dimensions
    private int initialPieceCoordinateROW, initialPieceCoordinateCOL;   //Initial coordinates of the selected piece
    private boolean isWhiteTurn = true; //Tracks whether it is white's turn

    ///stockfish_______________________________________________________stockfish
    private ChessEngine engine; //Stockfish chess engine



    /////////////////////////////// GLOBAL VARIABLES ///////////////////////////////
    //variable to add new piece upon promotion, so that board state has unique value
    //if not 'queenwhite' will be made upon promotion instead of "queen3white"
    //problem occurs if multiple pawns promote to queen (ie, "queenwhite" then "queenwhite")
    //thus need global for now
    int globalCountForPromotion = 2;    //Counter to create unique piece ID's for pawn promotion(special rules)


    static boolean lastMoveWasDoublePawnMove = false;   //Variable for En passant(special rules)
    static String lastPawnMoved = "";   //Variable for En passant(special rules)
    static boolean EnPassantPossible = false;   //Variable for En passant(special rules)
    /////////////////////////////// GLOBAL VARIABLES ///////////////////////////////
    private Map<String, ImageView> imageViewMap = new HashMap<>();  //Maps for associating piece IDs with images
    private Map<String, Integer> positionCounts = new HashMap<>();   //Maps for associating piece IDs with counting piece positions
    private int[] lastWhiteKingPos = new int[2], lastBlackKingPos = new int[2]; //Last positions of both white and black kings for check validation
    private King whiteKing,  blackKing; //Both white and black king
    private Tile enPassantTile = new Tile(-1, -1);  //En passant tile
    private int halfMoveClock = 0, numberOfMoves = 1, stockfishDepth = 5;;  //StockFish
    private boolean playAgainstStockfish = false;   //Indicate whether or not to play against stockfish

    private Stage primaryStage;

    private boolean playAgainstSimpleAI = false;    //Indicate whether or not to play against simple AI
    private customAi simpleAI;



    //Main entry for the JavaFx application.
    //Initializes the chess game and sets up the GUI
    //The primary stage for this application, onto which the application scene is set
    @Override
    public void start(Stage primaryStage) {
        //Primary stage to global variable
        this.primaryStage = primaryStage;
        //Initialize the GridPane for chess board
        gridPane = new GridPane();
        //Create the chess board state GridPane
        boardCurrent = new Board(gridPane);
        //Initialize and place pieces on the board
        setUpPieces(gridPane);

        //Initialize empty tiles on the board
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (boardCurrent.get(col, row) == null) {
                    boardCurrent.set(col, row, "null");
                }
            }
        }

        //Just to initialize kings to their respective variables in the boardCurrent
        initializeGame();

        //Mouse pressed : selects a piece and highlight its valid moves
        gridPane.setOnMousePressed(event -> {
            //Calculate tile coordinates based on mouse position
            int col = (int) (event.getSceneX() / TILE_SIZE);
            int row = (int) (event.getSceneY() / TILE_SIZE);
            initialPieceCoordinateCOL = col;
            initialPieceCoordinateROW = row;

            //Get the type of piece at the selected tile
            String typeOfPiece = boardCurrent.get(col, row);
            Piece piece = pieces.get(typeOfPiece);
            ArrayList<Tile> threatenedSquares;
            //Update threatened squares for both sides
            calculateThreatenedSquares();

            //Verify if the selected piece belongs to the current player
            if ((isWhiteTurn && typeOfPiece.contains("white")) || (!isWhiteTurn && typeOfPiece.contains("black"))) {
                selectedPiece = imageViewMap.get(typeOfPiece);


                //If a piece is selected, store the initial mouse position and bring the piece to the front
                if (selectedPiece != null) {
                    //Store the initial mouse position
                    selectedPiece.setUserData(new Point2D(event.getSceneX(), event.getSceneY()));
                    //Bring the selected piece to the front
                    selectedPiece.toFront();
                }

                //Check if the piece belongs to the current players turn
                if(isWhiteTurn) {
                    threatenedSquares = squaresThreatenedByBlack;
                } else {
                    threatenedSquares = squaresThreatenedByWhite;
                }


                //Highlight the valid moves for the selected piece
                piece.highlightValidMoves(gridPane, boardCurrent.getBoard(), threatenedSquares, this);

            }
        });

        //Create the amin scene with the chess board
        Scene scene = new Scene(gridPane, TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE);

        //Mouse dragged : moves the selected piece
        scene.setOnMouseDragged(event -> {
            if (selectedPiece != null) {
                //Calucate the drag distance based on initial mouse position
                Point2D initialPos = (Point2D) selectedPiece.getUserData();
                double deltaX = event.getSceneX() - initialPos.getX();
                double deltaY = event.getSceneY() - initialPos.getY();
                //Apply the piece's position
                selectedPiece.setTranslateX(deltaX);
                selectedPiece.setTranslateY(deltaY);
            }
        });

        //Mouse released : places the piece and updates the board
        scene.setOnMouseReleased(event -> {
            if (selectedPiece != null) {
                //Reset the translation
                selectedPiece.setTranslateX(0);
                selectedPiece.setTranslateY(0);

                //Get the type of piece at the original location
                String typeOfPiece = boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW);
                //Checking whose turn, in case of wrong click
                if ((isWhiteTurn && typeOfPiece.contains("white")) || (!isWhiteTurn && typeOfPiece.contains("black"))) {

                    if (selectedPiece != null) {

                        //Reset teh layout position of the piece
                        selectedPiece.setLayoutX(0);
                        selectedPiece.setLayoutY(0);
                        //Find the piece from the map using its type
                        Piece piece = pieces.get(boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW));
                        boolean validMove = false;
                        ArrayList<Tile> threatenedSquares;

                        //Reset the board tile colors
                        gridPane.getChildren().removeIf(node -> node instanceof Circle);
                        // Reset the board tile colors
                        boardCurrent.resetTileColor();

                        //Convert mouse coordinates to local Grid pane coordinates
                        Point2D localPoint = gridPane.sceneToLocal(event.getSceneX(), event.getSceneY());
                        double x = localPoint.getX();
                        double y = localPoint.getY();

                        //Determine the destination of column and row
                        int col = (int) (x / TILE_SIZE);
                        int row = (int) (y / TILE_SIZE);

                        //Check if the move is within the board boundaries
                        if (col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE) {

                            //Remove digits from the piece type to get its name
                            String pieceType = typeOfPiece.replaceAll("\\d", "");

                            //Checks if any possible move is valid in regard to check
                            validMove = simulateMoveProtectKing(piece, col, row);

                            if (validMove) {
                                //Check for castling(special moves)
                                if (pieceType.contains("king") && Math.abs(col - initialPieceCoordinateCOL) == 2) {
                                    King king = (King) piece;
                                    if (king.isCastlingValid(col, boardCurrent.getBoard(), isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite)) {
                                        handleCastling(initialPieceCoordinateCOL, initialPieceCoordinateROW, col, row);
                                    } else {
                                        //Invalid castling, return king to original position
                                        gridPane.getChildren().remove(selectedPiece);
                                        gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                                        return;
                                    }
                                }

                                //Handle pawn special movement "pawn promotion"
                                if (pieceType.contains("pawn")) {
                                    handlePawnMove(pieceType, row, initialPieceCoordinateROW, col);
                                }

                                //Move from current position to new spot
                                gridPane.getChildren().remove(selectedPiece);
                                gridPane.add(selectedPiece, col, row);
                                //Update the piece's column
                                piece.setCol(col);
                                //update the piece's row
                                piece.setRow(row);

                                //Update board state
                                String currentPiece = boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW);

                                //To capture piece.
                                String toBeRemoved = boardCurrent.get(col, row);
                                if (!toBeRemoved.equals("null")) {
                                    //Remove captured piece image
                                    gridPane.getChildren().remove(imageViewMap.get(toBeRemoved));
                                    //Remove captured piece from the map
                                    imageViewMap.remove(toBeRemoved);
                                }

                                //Clear old position
                                boardCurrent.set(initialPieceCoordinateCOL, initialPieceCoordinateROW,"null");
                                //Set new position
                                boardCurrent.set(col, row, currentPiece);

                                //Check and handle pawn promotion
                                if (currentPiece.contains("pawn")) {
                                    boolean isWhite = currentPiece.contains("white");
                                    if ((isWhite && row == 0) || (!isWhite && row == 7)) {
                                        handlePawnPromotion(currentPiece, col, row, isWhite);
                                    }
                                }

                                //Update piece's moved status
                                piece.setMoved(true);

                                //Increment move counter based on turn and piece type
                                if(!isWhiteTurn) {
                                    numberOfMoves++;
                                }

                                if(currentPiece.contains("pawn") || !toBeRemoved.equals("null")) {
                                    //Reset half-move clock for pawn move or capture
                                    halfMoveClock = 0;
                                } else {
                                    //Increment half-move clock for other moves
                                    halfMoveClock++;
                                }
                                piece.setMoved(true);
                                //Calculate threatened squares
                                calculateThreatenedSquares();



                                System.out.println("\n\nHuman MOVE: " + (isWhiteTurn ? "White" : "Black"));
                                printGridPaneContents();

                                // Print the current board in FEN format using ternary operator
                                System.out.println((isWhiteTurn ? "White" : "Black") + " move in FEN notation: " +
                                        boardCurrent.boardToFEN(pieces, isWhiteTurn, enPassantTile, halfMoveClock, numberOfMoves));



                                //Switch turns
                                switchTurn();

                            }else {
                                //If move invalid, return to last position
                                gridPane.getChildren().remove(selectedPiece);
                                gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                            }
                        } else {
                            //If move outside the board, return to last position
                            gridPane.getChildren().remove(selectedPiece);
                            gridPane.add(selectedPiece, initialPieceCoordinateCOL, initialPieceCoordinateROW);
                        }

                        //Reset initial coordinate
                        initialPieceCoordinateROW = -1;
                        initialPieceCoordinateCOL = -1;

                        //Calculate the threatened squares
                        calculateThreatenedSquares();
                        selectedPiece = null; //Reset selected piece

                    }


                }
            }

        });

        //Set up the primary stage
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chess Game");
        primaryStage.show();


        //Show game mode selection popup
        showGameModeSelectionPopup(primaryStage);






        ///STOCKFISH_________- ___________--    ___________________________STOCKFISH

        //change this to try windows version of stockfish

        /**
         * initialize engine
         * (Requirement 4.2.1)
         */
        try {
            System.out.println("Attempting to initialize chess engine...");
            //Initialize the Stockfish engine based on OS type
            initializeStockfish();
            System.out.println("Chess engine initialized successfully.");

        } catch (IOException e) {
            //Handle file system related errors (Stockfish binary not found)
            System.err.println("Failed to initialize chess engine: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            //Handle other unexpected errors during initialization
            System.err.println("Unexpected error initializing chess engine: " + e.getMessage());
            e.printStackTrace();
        }

        //Handle shutdown properly
        primaryStage.setOnCloseRequest(event -> {
            //prevent further event handling
            event.consume();
            shutdown();
        });



        //First event handler: Controls move speed adjustments (stockfish delay between moves)
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            //Check for speed increase key ('=' key)
            if (INCREASE_SPEED.match(event)) {
                //Increase delay with upper limit of 2.0 seconds
                pauseDuration = Math.min(2.0, pauseDuration + 0.1);
                showSpeedAlert("Increased");
            }
            //Check for speed decrease key ('-' key)
            else if (DECREASE_SPEED.match(event)) {
                //Decrease delay with lower limit of 0.1 seconds
                pauseDuration = Math.max(0.1, pauseDuration - 0.1);
                showSpeedAlert("Decreased");
            }
        });

    //Second event handler: Controls secret mode (Stockfish vs Stockfish)
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            //Check for secret mode key combination (Shift + 7)
            if (SECRET_COMBO.match(event)) {
                //Toggle secret mode state
                secretMode = !secretMode;

                //Prepare appropriate message based on mode state
                String message = secretMode ?
                        "Secret Mode Activated: Stockfish vs Stockfish" :
                        "Secret Mode Deactivated";

                //Create and show alert dialog
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Secret Mode");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();

                //When mode is activated stockfish needs to make move
                //immediately to avoid current switch turn logic
                //from forcing one last manual white move
                if (secretMode && isWhiteTurn) {
                    makeEngineMove();
                    switchTurn();
                }
            }
        });
    }



    /**
     * shutdown
     * (Requirement 2.1.0)
     */
    private void shutdown() {

        //Terminate program upon exiting window
        System.out.println("Exiting application...");

        Platform.exit();
        //Clean up JavaFX resources
        System.exit(0);
        //Terminate JVM
    }

    /**
     * Switch turn
     * (Requirement 2.0.0)
     */
    private void switchTurn() {
        //Toggle between white and black's turn
        isWhiteTurn = !isWhiteTurn;
        System.out.println(isWhiteTurn ? "White's turn" : "Black's turn");

        playMoveSound();

        //Update board state
        calculateThreatenedSquares();
        //Recalculate squares under attack
        updateCheckStatus();

        if (isCheckmate()) {
            System.out.println(isWhiteTurn ? "Black wins by checkmate!" : "White wins by checkmate!");
            return;  //Game ends
        }
        if(checkForDraw()){
            return;  //Game ends
        };



        //Handle computer moves if playing against AI
        if (playAgainstStockfish) {
            //Make Stockfish move if it's black's turn or if in secret mode (Stockfish vs Stockfish)
            if (!isWhiteTurn || secretMode) {
                if (engine != null) {
                    //Add delay before AI move for better user experience
                    makeDelayedMove(() -> {
                        makeEngineMove();
                        // Make Stockfish's move
                        switchTurn();
                    });
                } else {
                    System.err.println("Cannot make engine move: Chess engine is not initialized!");
                }
            }
        } else if (!isWhiteTurn && playAgainstSimpleAI) {
            //Make simple AI move if it's black's turn
            makeDelayedMove(() -> {
                makeSimpleAIMove();
                switchTurn();
            });
        } else {
            /**
             * 1 v 1
             * (Requirement 4.0.0)
             */
            //If neither AI condition is met, wait for human player input
            //The game continues through mouse event handlers
        }
    }



    /**
     * Stockfish integration in ChessGame
     * (Requirement 4.2.0)
     */
    /**
     * makeEngineMove
     * (Requirement 4.2.2)
     */
    //Handles making a move using the Stockfish chess engine
    //Converts current board state to FEN, gets best move from engine, and applies it
    private void makeEngineMove() {

        //Check if engine is properly initialized
        if (engine == null) {
            System.err.println("Chess engine is not initialized!");
            return;
        }

        // Check for checkmate/stalemate before asking engine for moves
        if (isCheckmate()) {
            System.out.println("Game Over - " + (isWhiteTurn ? "Black" : "White") + " wins by checkmate!");
            return;
        }
        if (checkForDraw()) {
            System.out.println("Game Over - Draw!");
            return;
        }
        try {
            //current board state to FEN notation (Forsyth–Edwards Notation)
            String fen = boardCurrent.boardToFEN(pieces, isWhiteTurn, enPassantTile, halfMoveClock, numberOfMoves);
            //send current board position to Stockfish engine
            engine.sendCommand("position fen " + fen);

            //different depth set by difficulty chosen by user
            int depth = stockfishDepth;
            //different depth for secret mode
            if (secretMode) {
                depth = isWhiteTurn ? stockfishDepth + 5 : stockfishDepth ; // Make black slightly weaker
            }
            engine.sendCommand("go depth " + depth);

            //get then apply the calculated move
            String bestMove = getBestMoveFromEngine();
            applyEngineMove(bestMove);

        } catch (IOException e) {
            System.err.println("Error communicating with chess engine: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * getBestMoveFromEngine
     * (Requirement 4.2.3)
     */
    private String getBestMoveFromEngine() {
        try {
            String line;
            //Keep reading lines from engine output until we find the best move
            while ((line = engine.readLine()) != null) {
                //find line that starts with "bestmove"
                if (line.startsWith("bestmove")) {
                    //Split the line by whitespace and return the second word
                    //"bestmove g1f3 ponder xxxx"
                    //thus, get the "g1f3"
                    return line.split("\\s+")[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //if no best move, return null
        return null;
    }


    /**
     * applyEngineMove
     * (Requirement 4.2.4)
     */
    //Applies a move calculated by the Stockfish engine to the chess board
    //Handles piece movement, captures, promotions, and special moves
    private void applyEngineMove(String move) {
        System.out.println("Applying engine move: " + move);
        try {
            int startCol = move.charAt(0) - 'a';
            int startRow = '8' - move.charAt(1);
            int endCol = move.charAt(2) - 'a';
            int endRow = '8' - move.charAt(3);


            String movingPiece = boardCurrent.get(startCol, startRow);
            String capturedPiece = boardCurrent.get(endCol, endRow);


            //Synchronization aid that allows threads to wait until a set of operations completes (CountDownLatch)
            //Constructor takes initial count
            // countDown() decrements the count
            // await() blocks until count reaches zero
            // To ensure move completion before proceeding
            CountDownLatch moveLatch = new CountDownLatch(1);



            // Handle captures first
            if (!capturedPiece.equals("null")) {
                //Ensures code runs on JavaFX Application Thread (runLater) think a queue
                //Required for all UI updates in JavaFX (JavaFX has a single UI thread)
                //Prevents threading issues when modifying UI elements
                Platform.runLater(() -> {
                    //Ensures thread-safe access to the GridPane (synchronized)
                    //Only one thread can execute synchronized block at a time (think locks and keys)
                    //Prevents concurrency issues
                    synchronized (gridPane) {
                        ImageView capturedPieceView = imageViewMap.get(capturedPiece);
                        if (capturedPieceView != null) {
                            gridPane.getChildren().remove(capturedPieceView);
                            imageViewMap.remove(capturedPiece);
                        }
                        boardCurrent.set(endCol, endRow, "null");
                    }
                });
            }

            //Check for pawn promotion
            boolean isPromotion = move.length() == 5 &&
                    movingPiece.contains("pawn") &&
                    (endRow == 0 || endRow == 7);

            if (isPromotion) {
                char promotionPiece = move.charAt(4);
                Platform.runLater(() -> {
                    synchronized (gridPane) {
                        handleEnginePawnPromotion(movingPiece, endCol, endRow, promotionPiece);
                        boardCurrent.set(startCol, startRow, "null");
                        moveLatch.countDown();
                    }
                });
            } else {
                //Handle regular moves and other special moves
                Platform.runLater(() -> {
                    synchronized (gridPane) {
                        try {
                            //Handle castling and en passant first
                            handleSpecialMoves(movingPiece, startCol, startRow, endCol, endRow,
                                    move.length() == 5 ? move.charAt(4) : ' ');

                            //Then handle the regular move
                            ImageView movingPieceView = imageViewMap.get(movingPiece);

                            //Create and play transition animation
                            // Set up animation that shows it moving from old square to new square
                            TranslateTransition transition = new TranslateTransition(Duration.millis(500), movingPieceView);
                            //Calculate starting position relative to final position
                            //If moving from e2 to e4, this might be (0, -200) meaning "start 2 squares up"
                            transition.setFromX((startCol - endCol) * TILE_SIZE);
                            transition.setFromY((startRow - endRow) * TILE_SIZE);
                            //Set ending position to (0,0) in the new grid cell
                            transition.setToX(0);
                            transition.setToY(0);

                            //Move the piece in the GridPane first
                            gridPane.getChildren().remove(movingPieceView);
                            gridPane.add(movingPieceView, endCol, endRow);

                            //Reset translation after animation completes
                            transition.setOnFinished(e -> {
                                movingPieceView.setTranslateX(0);
                                movingPieceView.setTranslateY(0);
                            });
                            //Start moving the piece
                            transition.play();


                            //Update board state
                            boardCurrent.set(endCol, endRow, movingPiece);
                            boardCurrent.set(startCol, startRow, "null");

                            //Update piece data
                            Piece piece = pieces.get(movingPiece);
                            if (piece != null) {
                                piece.setCol(endCol);
                                piece.setRow(endRow);
                                piece.setMoved(true);
                            }

                            calculateThreatenedSquares();
                            updateCheckStatus();
                            isCheckmate();
                            checkForDraw();
                        } catch (Exception e) {
                            System.err.println("Error during move: " + e.getMessage());
                        } finally {
                            moveLatch.countDown();
                        }
                    }
                });
            }


        } catch (Exception e) {
            System.err.println("Error applying engine move: " + move);
            e.printStackTrace();
        }
    }


    /**
     * Update check status
     * (Requirement 2.3.0)
     */
    private void updateCheckStatus() {
        //Update which squares are under attack by each side
        calculateThreatenedSquares();

        //Check if either king is under attack
        boolean whiteInCheck = whiteKing.isInCheck(squaresThreatenedByBlack);
        boolean blackInCheck = blackKing.isInCheck(squaresThreatenedByWhite);

        //To verify check status
         System.out.println("White in check: " + whiteInCheck);
         System.out.println("Black in check: " + blackInCheck);

        //Update the king's tile (red if in check)
        updateKingTileColor(whiteKing, whiteInCheck, lastWhiteKingPos);
        updateKingTileColor(blackKing, blackInCheck, lastBlackKingPos);
    }



    /**
     * Update king tile color
     * (Requirement 2.4.0)
     */
    //when a king is in check its tile is turned red
    private void updateKingTileColor(King king, boolean isInCheck, int[] lastPos) {
        //Get current king position
        int col = king.getCol();
        int row = king.getRow();

        //If king has moved, restore the original color of its previous tile
        if (lastPos[0] != row || lastPos[1] != col) {
            restoreOriginalColor(lastPos[0], lastPos[1]);
        }

        //Get tile where king currently is
        StackPane kingTile = boardCurrent.getStiles()[row][col];

        if (isInCheck) {
            kingTile.setStyle("-fx-background-color: RED;");
        } else {
            restoreOriginalColor(row, col);
        }

        //Store current position as the last known position
        lastPos[0] = row;
        lastPos[1] = col;
    }


    /**
     * restore tile color when king escapes check
     * (Requirement 2.4.1)
     */
    //when a king is not in check, the tile it was on last is white or gray
    private void restoreOriginalColor(int row, int col) {
        StackPane tile = boardCurrent.getStiles()[row][col];
        //Calculate if this should be a light or dark square
        boolean isLightSquare = (row + col) % 2 == 0;
        //set the color of the tile to correct color
        tile.setStyle(isLightSquare ? "-fx-background-color: WHITE;" : "-fx-background-color: GRAY;");
    }




    ///stockfish_______________________________________________________stockfish


    //stockfish__________helpermethods____________________________________stockfish

    /**
     * handleSpecialMoves
     * (Requirement 4.2.5)
     */
    //Handles special moves for the chess engine (Stockfish)
    private void handleSpecialMoves(String movingPiece, int startCol, int startRow, int endCol, int endRow, char promotionPiece) {
        //Strip numbers and color from piece name to get base piece type (e.g., "pawn1white" -> "pawn")
        String pieceType = movingPiece.replaceAll("\\d", "").replace("white", "").replace("black", "");


        if (pieceType.equals("pawn") && (endRow == 0 || endRow == 7)) {
            handleEnginePawnPromotion(movingPiece, endCol, endRow, promotionPiece);
        } else if (pieceType.equals("king") && Math.abs(endCol - startCol) == 2) {
            handleCastlingStockfish(startCol, startRow, endCol, endRow);
        } else if (pieceType.equals("pawn") && startCol != endCol && boardCurrent.get(endCol, endRow).equals("null")) {
            handleEnPassantStockfish(startCol, startRow, endCol, endRow);
        }

        //Update last move for future en passants
        lastMoveWasDoublePawnMove = pieceType.equals("pawn") && Math.abs(endRow - startRow) == 2;
        if (lastMoveWasDoublePawnMove) {
            lastPawnMoved = movingPiece;
        }
    }

    /**
     * handle engine pawn promotion
     * (Requirement 4.2.6)
     */
    //Handles pawn promotion for the chess engine (Stockfish)
    private void handleEnginePawnPromotion(String currentPiece, int col, int row, char promotionPiece) {
        //Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            //Remove the existing pawn from the board and data structures
            ImageView pawnView = imageViewMap.get(currentPiece);
            gridPane.getChildren().remove(pawnView);
            imageViewMap.remove(currentPiece);

            //Determine piece color from the current piece
            String color = currentPiece.contains("white") ? "white" : "black";
            //Determine the new piece type
            String newPieceType = switch (promotionPiece) {
                case 'q' -> "queen";
                case 'r' -> "rook";
                case 'b' -> "bishop";
                case 'n' -> "knight";
                default -> "queen"; // Default to queen if something unexpected happens
            };

            //Increment global counter to ensure unique piece IDs
            globalCountForPromotion++;

              //Create new piece
            String newPieceName = newPieceType + globalCountForPromotion + color;
            Piece promotedPiece = createPiece(newPieceType, color);
            ImageView promotedPieceView = promotedPiece.getPiece();

            //Add new piece to pieces map
            pieces.put(newPieceName, promotedPiece);

            //Add new piece
            gridPane.add(promotedPieceView, col, row);

            //Update board state and piece properties
            boardCurrent.set(col, row, newPieceName);
            promotedPiece.setCol(col);
            promotedPiece.setRow(row);
            promotedPiece.setMoved(true);

            imageViewMap.put(newPieceName, promotedPieceView);


        });
    }



    /**
     * handle castling for stockfish
     * (Requirement 4.2.9)
     */
    //Handles castling moves for the Stockfish engine
    private void handleCastlingStockfish(int startCol, int startRow, int endCol, int endRow) {
        //Determine if this is kingside (right) or queenside (left) castling
        int rookStartCol = endCol > startCol ? 7 : 0;

        //Calculate where the rook should end up
        int rookEndCol = endCol > startCol ? endCol - 1 : endCol + 1;


        //Move rook to its new position
        String rookPiece = moveRook(startRow, rookStartCol, rookEndCol);

        // Mark rook as moved to prevent future castling with this rook
        Piece rook = pieces.get(rookPiece);
        if (rook != null) {
            rook.setMoved(true);
        }
    }

    /**
     * handle en passant for stockfish
     * (Requirement 4.2.11)
     */
    // Handles en passant captures for the Stockfish engine
    private void handleEnPassantStockfish(int startCol, int startRow, int endCol, int endRow) {
        // Get the pawn being captured
        String capturedPawn = boardCurrent.get(endCol, startRow);
        ImageView capturedPawnView = imageViewMap.get(capturedPawn);

        //Remove the captured pawn from the visual board and image mapping
        gridPane.getChildren().remove(capturedPawnView);
        imageViewMap.remove(capturedPawn);
        //Update the board state to show empty square
        boardCurrent.set(endCol, startRow, "null");
    }


//END OF stockfish_______________________________________________________stockfish helper methods




    //Handles the promotion of a pawn when it reaches the final row
    private void handlePawnPromotion(String currentPiece, int col, int row, boolean isWhite) {
        //List of promotion options available to the player
        List<String> promotionOptions = Arrays.asList("Queen", "Rook", "Bishop", "Knight");
        //Create a dialog box to allow the player to choose the piece for promotion
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Queen", promotionOptions);
        //Title of the dialog box
        dialog.setTitle("Pawn promotion");
        //No header text
        dialog.setHeaderText(null);
        //Pawn promotion selection text
        dialog.setContentText("Choose piece you want to promote:");

        //Show the dialog and wait for the player to make a selection
        Optional<String> result = dialog.showAndWait();
        //If the player makes a selection for promotion, proceed.
        result.ifPresent(choice -> {
            //Remove the current pawn image
            ImageView pawnView = imageViewMap.get(currentPiece);
            //Remove the pawn's image from the gird
            gridPane.getChildren().remove(pawnView);
            //Remove the pawn from the map
            imageViewMap.remove(currentPiece);

            //Increment the global counter for unique piece identification
            globalCountForPromotion = globalCountForPromotion + 1;
            //Create new piece
            //Convert choice to lowercase("Queen" -> "queen")
            String newPieceType = choice.toLowerCase();
            //New name
            String newPieceName = newPieceType + globalCountForPromotion+ (isWhite ? "white" : "black");
            //Determine the color f the new piece
            String newColor = (isWhite ? "white" : "black");
            //Create the promotion piece
            Piece promotedPiece = createPiece(newPieceType, isWhite ? "white" : "black");
            //Get the image for the new promotion piece
            ImageView promotedPieceView = promotedPiece.getPiece();


            //Add the new promotion piece to the piece map
            pieces.put(promotedPiece.getType() +globalCountForPromotion+ newColor, promotedPiece);


            //Add the piece image to the board
            gridPane.add(promotedPieceView, col, row);
            //Update the board state with the new promotion piece
            boardCurrent.set(col, row, newPieceName);
            //Set the new piece's column
            promotedPiece.setCol(col);
            //Set the new piece's row
            promotedPiece.setRow(row);
            //Update the image view map with the new promotion piece
            imageViewMap.put(newPieceName, promotedPieceView);
        });
    }

    //This method calculates the squares that are being threatened.
    private void calculateThreatenedSquares() {

        //Reset both ArrayLists before adding new squares.
        squaresThreatenedByWhite.clear();
        squaresThreatenedByBlack.clear();

        //Iterate through the board to find all Pieces remaining.
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                
                //Pass the String key into the Pieces HashMap.
                Piece piece = pieces.get(boardCurrent.get(col, row));
                
                //If the key returns a piece, we calculate the squares it currently threatens.
                if (piece != null) {
                    //Place the returned ArrayList into threatenedSquares.
                    ArrayList<Tile> threatenedSquares = piece.findThreatenedSquares(boardCurrent.getBoard());

                    //Append threatenedSquares to the appropriate ArrayList, given color.
                    if (piece.getColor().equals("white")) {
                        squaresThreatenedByWhite.addAll(threatenedSquares);
                    } else {
                        squaresThreatenedByBlack.addAll(threatenedSquares);
                    }
                }
            }
        }
    }

    //This method instantiates and places the needed Pieces on the board.
    private void setUpPieces(GridPane gridPane) {

        //Array of pieces each side begins with, all in order.
        String[] pieceList = {"rook1", "knight1", "bishop1", "queen1", "king1", "bishop2", "knight2", "rook2",
                "pawn0", "pawn1", "pawn2", "pawn3", "pawn4", "pawn5", "pawn6", "pawn7"};

        //Array of colors.
        String[] colors = {"black", "white"};

        int row = 0, col = 0;

        //Iterate through the colors Array.
        for (String color : colors) {

            //Iterate through the pieceList Array in order to instantiate all the Pieces needed.
            for (String piece : pieceList) {

                //If column equals 8, that means we've reached the end of the column.
                if (col == 8) {
                    //If row equals 1, that means that all White Pieces have been instantiated.
                    //We must set row to 7 to begin instantiating Black Pieces.
                    if (row == 1) {
                        row = 7;
                    } 
                    //If row equals 7, we just finished the first row of Black Pieces.
                    //We must go to row 6 to instantiate the final row of Black Pieces.
                    else if (row == 7) {
                        row--;
                    }
                    //Otherwise, we simply increment the row.
                    //This case occurs when we are done instantiating the first row of White Pieces.
                    else {
                        row++;
                    }
                    //Column must be reset in order to continue iteration.
                    col = 0;
                }

                //If the end of a row has not been reached, a new Piece is instantiated.
                Piece nextPiece = createPiece(piece, color);
                
                //The key for the Piece is created based on which Piece, what number as well as what color it is.
                String typeColor = piece + color;
                
                //Place the new Piece in the pieces HashMap for easy access.
                pieces.put(typeColor, nextPiece);
                
                //Place the String key for the Piece in the board.
                boardCurrent.set(col, row, typeColor);

                //Set the column and row in the Piece object.
                assert nextPiece != null;
                nextPiece.setCol(col);
                nextPiece.setRow(row);

                //Add the ImageView of the Piece to the gridpane.
                gridPane.add(nextPiece.getPiece(), col, row);

                //Add the ImageView of the Piece to a HashMap.
                imageViewMap.put(typeColor, nextPiece.getPiece());

                //Increment the column to continue iteration.
                col++;
            }
        }
    }

    //This method instantiates a new Piece object.
    private Piece createPiece(String type, String color) {

        //Removes the appended digit.
        //The digit occurs due to the need to differentiate between multiple Pieces.
        //Ex. pawn1, pawn 2, etc.
        type = type.replaceAll("\\d", "");

        //Creates the object based on the type and color.
        return switch (type) {
            case "king" -> new King(color);
            case "queen" -> new Queen(color);
            case "knight" -> new Knight(color);
            case "bishop" -> new Bishop(color);
            case "rook" -> new Rook(color);
            case "pawn" -> new Pawn(color);
            //If the given String does not match any case, null is returned and no object is created.
            default -> null;
        };
    }

    //Handles the castling move(special rules) for a King and its associated Rook
    private void handleCastling(int startCol, int startRow, int endCol, int endRow) {
        //Determine if the castling is king-side or queen-side based on the king's final position
        boolean isKingSide = endCol > startCol;
        //Determine if the starting and ending columns for the rook involved in castling
        //Rook starts at column 7 for king-side, column 0 for queen-side
        int rookStartCol = isKingSide ? 7 : 0;
        //Rook ends next to the king
        int rookEndCol = isKingSide ? endCol - 1 : endCol + 1;

        // Move rook
        String rookPiece = moveRook(startRow, rookStartCol, rookEndCol);

        //Update rook's position and moved status
        Rook rook = (Rook) pieces.get(rookPiece);
        rook.handleCastlingMove(rookEndCol);
    }

    //Moves a rook from its starting position to its new position during a castling(special rules)
    private String moveRook(int startRow, int rookStartCol, int rookEndCol) {
        //Get the name of the rook piece at its starting position
        String rookPiece = boardCurrent.get(rookStartCol, startRow);
        //Find the imageView for the rook from the image view map
        ImageView rookView = imageViewMap.get(rookPiece);
        //Remove the rook's image from its current position on the gird
        gridPane.getChildren().remove(rookView);
        //Add the rook's image from its current position on the grid
        gridPane.add(rookView, rookEndCol, startRow);
        //Update the board state to reflect the rook's new position
        //Clear the rook's old position
        boardCurrent.set(rookStartCol, startRow,"null");
        //Set the rook's new position
        boardCurrent.set(rookEndCol, startRow, rookPiece);
        //Return the name of the rook piece
        return rookPiece;
    }

    //Sets up references to both kings and calculates initial threatened squares
    private void initializeGame() {

        whiteKing = (King) pieces.get("king1white");
        blackKing = (King) pieces.get("king1black");
        calculateThreatenedSquares();
    }

    /**
     * simulate moves that protect king (see if move is valid)
     * (Requirement 2.7.0)
     */
    //Simulates a move to check if it would leave the king in check
    //Also validates if the move is legal
    public boolean simulateMoveProtectKing(Piece piece, int endCol, int endRow) {

        //Store piece's current position
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

        //Recalculate which squares are under attack after the move
        calculateThreatenedSquares();

        //Check for check
        boolean kingInCheck = (isWhiteTurn ? whiteKing : blackKing).isInCheck(
                isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite);

        //Revert move
        boardCurrent.set(startCol, startRow, movingPiece);
        boardCurrent.set(endCol, endRow, capturedPiece);
        piece.setCol(startCol);
        piece.setRow(startRow);

        //Recalculate threatened squares for original position
        calculateThreatenedSquares();

        //Return true if the move is legal, false otherwise
        return !kingInCheck && piece.isValidMove(endCol, endRow, boardCurrent.getBoard(),
                isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite);
    }


    //Checkmate occurs when the king is in check and there are no legal moves
    private boolean isCheckmate() {
        //Get the king and threatened squares based on whose turn it is
        King currentKing = isWhiteTurn ? whiteKing : blackKing;
        ArrayList<Tile> threatenedSquares = isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite;

        // First check if king is in check
        if (!currentKing.isInCheck(threatenedSquares)) {
            return false;
        }

        //If there are any legal moves, it's not checkmate
        if (!hasNoLegalMoves()) {
            return false;
        }

        //It's checkmate, show popup
        Platform.runLater(() -> {
            //Determine winner based on whose turn it was
            String winningColor = isWhiteTurn ? "Black" : "White";
            //popup for checkmate
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Checkmate");
            alert.setHeaderText(null);
            alert.setContentText("CHECKMATE! " + winningColor + " wins!");
            alert.showAndWait();

            handleGameEnd("Great Game!");
        });

        return true;
    }





    //Handles special pawn moves including double moves and en passant captures
    private void handlePawnMove(String pieceType, int row, int initialPieceCoordinateROW, int col) {
        // Check for double pawn move
        if (Math.abs(row - initialPieceCoordinateROW) == 2) {
            lastMoveWasDoublePawnMove = true;
            //Store the pawn that made the double move
            lastPawnMoved = boardCurrent.get(initialPieceCoordinateCOL, initialPieceCoordinateROW);
            //Set the en passant target square
            enPassantTile.setCol(lastPawnMoved.charAt(4) - '0');
            enPassantTile.setRow(lastPawnMoved.contains("white") ? 3 : 5);
        } else {
            //If not a double move, reset en passant flags
            lastMoveWasDoublePawnMove = false;
            enPassantTile.setCol(-1);
            enPassantTile.setRow(-1);
        }

        //Check for en passant capture
        if (EnPassantPossible) {
            int capturedPawnRow = pieceType.contains("white") ? row + 1 : row - 1;

            //Remove the captured pawn from the board
            boardCurrent.set(col, capturedPawnRow, "null");

            //Remove the captured pawn's image from GUI
            ImageView capturedPawnView = imageViewMap.get(lastPawnMoved);
            if (capturedPawnView != null) {
                gridPane.getChildren().remove(capturedPawnView);
                imageViewMap.remove(lastPawnMoved);
            }
        }
    }


    /**
     * intitialize stockfish path
     * (Requirement 4.2.7)
     */
    //Initializes the path to the Stockfish chess engine executable
//Attempts to load from resources first, then falls back to project directory
    private Path initializeStockfishPath(String stockfishRelativePath) throws IOException {
        //First, try to load from resources
        InputStream inputStream = getClass().getResourceAsStream("/" + stockfishRelativePath);
        if (inputStream != null) {
            // Create temporary directory for Stockfish
            Path tempDir = Files.createTempDirectory("stockfish");
            Path tempFile = tempDir.resolve("stockfish");


            //Copy the Stockfish binary to the temporary directory
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            //Make the file executable
            tempFile.toFile().setExecutable(true);

            return tempFile;
        }

        //If not found in resources, try to find it in the project directory
        String projectDir = System.getProperty("user.dir");
        //Construct path to resources directory
        Path resourcesPath = Paths.get(projectDir, "src", "main", "resources");
        Path stockfishPath = resourcesPath.resolve(stockfishRelativePath);

        //Verify file exists and is executable
        File stockfishFile = stockfishPath.toFile();
        if (!stockfishFile.exists()) {
            throw new FileNotFoundException("Stockfish binary not found at: " + stockfishPath);
        }
        if (!stockfishFile.canExecute()) {
            stockfishFile.setExecutable(true);
        }

        return stockfishPath;
    }


    /**
     * intitialize stockfish
     * (Requirement 4.2.8)
     */
    //Initializes the Stockfish chess engine based on the operating system
    //Selects appropriate binary version and sets up the engine
    private void initializeStockfish() throws IOException {
        //Determine which Stockfish binary to use based on OS
        String stockfishRelativePath;
        //Check if running on Windows
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
            //Using M1/Apple Silicon optimized version
            stockfishRelativePath = "stockfish-macos-m1-apple-silicon";
        }

        //Get the full path to the Stockfish binary
        Path stockfishPath = initializeStockfishPath(stockfishRelativePath);

        //System.out.println("Stockfish path: " + stockfishPath);
        //Initialize the chess engine with the selected binary
        engine = new ChessEngine(stockfishPath.toString());
    }




    //Screens to select game mode and difficulty
    private void showGameModeSelectionPopup(Stage primaryStage) {
        try {
            //Initialize the Stockfish chess engine
            initializeStockfish();
        } catch (IOException e) {
            System.err.println("Failed to initialize Stockfish: " + e.getMessage());
            // Optionally, show an error dialog to the user
        }
        //Crate the new popup stage for the game mode selection
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(primaryStage);
        popupStage.setTitle("Game Setup");

        //Create VBox layout
        VBox vbox = new VBox(20);
        //Center align
        vbox.setAlignment(Pos.CENTER);
        //Add padding around the VBox
        vbox.setPadding(new Insets(30));

        //Game mode selection
        //Group to ensure only one mode is selected
        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton oneVsOneButton = createModeButton("1 vs 1", modeGroup);
        ToggleButton stockfishButton = createModeButton("Play against Stockfish", modeGroup);
        ToggleButton simpleAIButton = createModeButton("Play against Simple AI", modeGroup);
        //Arrange game mode selection
        VBox modeBox = new VBox(20, oneVsOneButton, stockfishButton, simpleAIButton);
        //Center align
        modeBox.setAlignment(Pos.CENTER);

        //Difficulty selection (initially hidden)
        ToggleGroup difficultyGroup = new ToggleGroup();
        ToggleButton easyButton = createDifficultyButton("Easy", 1, difficultyGroup);
        ToggleButton mediumButton = createDifficultyButton("Medium", 7, difficultyGroup);
        ToggleButton hardButton = createDifficultyButton("Hard", 10, difficultyGroup);
        //Arrange difficulty selection
        VBox difficultyBox = new VBox(20, easyButton, mediumButton, hardButton);
        difficultyBox.setAlignment(Pos.CENTER);
        difficultyBox.setVisible(false);

        //Add game mode and difficulty selection to the VBox
        vbox.getChildren().addAll(modeBox, difficultyBox);

        //Create a scene for the popup and set it to the popup stage
        Scene popupScene = new Scene(vbox, 300, 400);
        popupStage.setScene(popupScene);

        //Set up button actions
        oneVsOneButton.setOnAction(e -> {
            //Set game mode to "1vs1"
            playAgainstStockfish = false;
            //Close the popup
            popupStage.close();
        });

        stockfishButton.setOnAction(e -> {
            //Set game mode to "Play against Stockfish
            playAgainstStockfish = true;
            playAgainstSimpleAI = false;
            modeBox.setVisible(false);
            //Show difficulty selection
            difficultyBox.setVisible(true);
            popupStage.setTitle("Select Difficulty");
        });

        simpleAIButton.setOnAction(e -> {
            //Set game mode to "Play against Simple AI"
            playAgainstSimpleAI = true;
            playAgainstStockfish = false;
            simpleAI = new customAi(false); // AI plays as black
            popupStage.close();
        });

        //Set action for difficulty selection
        difficultyGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                //Set Stockfish depth based on selected difficulty
                stockfishDepth = (int) newValue.getUserData();
                //Close the popup
                popupStage.close();
            }
        });

        //Show the popup and wait for user
        popupStage.showAndWait();
    }

    //Toggle button for game mode selection
    private ToggleButton createModeButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        //Assign the button to the group
        button.setToggleGroup(group);
        //Set the size of the button
        button.setPrefSize(200, 50);
        //Font
        button.setStyle("-fx-font-size: 16px;");
        return button;
    }

    //Toggle button for difficulty selection
    private ToggleButton createDifficultyButton(String text, int depth, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        //Assign the button to the group
        button.setToggleGroup(group);
        //Set the user data for the difficulty depth
        button.setUserData(depth);
        //Set the size of the button
        button.setPrefSize(200, 50);
        //Font
        button.setStyle("-fx-font-size: 16px;");
        return button;
    }






    // Although a threefold repetition usually occurs after
    // consecutive moves, there is no requirement that the moves
    // be consecutive for a claim to
    // be valid. The rule applies to positions, not moves.

    //draw methods
    //Creates a unique string key representing the current board position
    //Used for detecting threefold repetition
    private String getPositionKey() {
        //Initialize string builder for efficient string concatenation
        StringBuilder sb = new StringBuilder();

        //Append the entire board state
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                sb.append(boardCurrent.get(col, row)).append("|");
            }
        }

        //Append whose turn it is
        //This is necessary because same position with different turn is different state
        sb.append(isWhiteTurn ? "w" : "b");

        return sb.toString();
    }




    //Checks if the current position is a draw
    //Draw conditions: threefold repetition, fifty-move rule, or stalemate
    private boolean checkForDraw() {
        //Check all draw conditions:
        // 1. Same position occurred three times
        // 2. Fifty moves without pawn move or capture
        // 3. Stalemate (no legal moves but not in check)
        boolean draw = checkForThreefoldRepetition() || halfMoveClock == 50 || isStalemate();
        if(draw) {
            handleDraw();
        }
        return draw;
    }


    //Checks if the current position has occurred three times
    //Uses position key to track unique board states
    private boolean checkForThreefoldRepetition() {
        //Get unique string representation of current position
        String positionKey = getPositionKey();
        //Get current count of this position and increment it
        int count = positionCounts.getOrDefault(positionKey, 0) + 1;
        //Update position counter
        positionCounts.put(positionKey, count);
        //Return true if position has occurred three times
        return count >= 3;
    }


    //Checks if the current position is a stalemate
//Stalemate occurs when the player has no legal moves but their king is not in check
    private boolean isStalemate() {
        //First check if there are any legal moves available
        if (hasNoLegalMoves()) {
            //Get the current player's king and threatened squares
            King currentKing = isWhiteTurn ? whiteKing : blackKing;
            ArrayList<Tile> threatenedSquares = isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite;
            //Stalemate occurs when king is NOT in check but has no legal moves
            return !currentKing.isInCheck(threatenedSquares);
        }
        return false;
    }

    //Handles the draw scenario by displaying a popup and ending the game
    private void handleDraw() {
        //Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            //Create and configure alert dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Draw");
            alert.setHeaderText(null);
            alert.setContentText("DRAW!");
            //Show alert and wait for user acknowledgment
            alert.showAndWait();
            //Handle end of game
            handleGameEnd("Better luck next time!");
        });
    }

    //Checks if the current player has any legal moves available
    //Iterates through all pieces and possible destinations to find valid moves
    private boolean hasNoLegalMoves() {
        //Determine which color's pieces to check based on current turn
        String currentColor = isWhiteTurn ? "white" : "black";

        //Iterate through all board positions
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {
                //Get piece at current position
                String pieceKey = boardCurrent.get(col, row);

                //Check if square has a piece of current player's color
                if (!pieceKey.equals("null") && pieceKey.contains(currentColor)) {
                    Piece piece = pieces.get(pieceKey);

                    //If piece exists, check all possible destination squares
                    if (piece != null) {
                        //Try every possible destination square
                        for (int newCol = 0; newCol < BOARD_SIZE; newCol++) {
                            for (int newRow = 0; newRow < BOARD_SIZE; newRow++) {
                                //If any legal move is found, return false (has legal moves)
                                if (simulateMoveProtectKing(piece, newCol, newRow)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        //If no legal moves were found, return true
        return true;
    }


    //Stalemate ---------------------------




    /**
     * restart application
     * (Requirement 2.9.0)
     */
    //Restarts the chess game by creating a new instance
    private void restartApplication(Stage currentStage) {
        //Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                //Create a new instance of ChessGame
                ChessGame newInstance = new ChessGame();
                //Start new game using current stage
                newInstance.start(currentStage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }



    /**
     * Handle Game End
     * (Requirement 2.10.0)
     */
    //Handles the end of game scenario with a popup dialog
    //Offers options to play again or exit the game
    private void handleGameEnd(String message) {
        //Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            //Create and configure the game over dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText(null);
            alert.setContentText(message + "\nDo you want to play again?");

            //Create custom buttons for the dialog
            ButtonType playAgainButton = new ButtonType("Play Again");
            ButtonType exitButton = new ButtonType("Exit");
            alert.getButtonTypes().setAll(playAgainButton, exitButton);

            //Show dialog and wait for user response
            Optional<ButtonType> result = alert.showAndWait();
            //Handle user choice
            if (result.isPresent() && result.get() == playAgainButton) {
                //Close current window
                primaryStage.close();
                //Create new stage for the next game
                Stage next = new Stage();
                //Restart the application with the new stage
                restartApplication(next);
            } else {
                //Exit application if user chose Exit or closed the di
                Platform.exit();
                System.exit(0);
            }
        });
    }


    /**
     * Play Move Sound
     * (Requirement 2.2.0)
     */
    //Plays a sound effect when a chess piece is moved
    //Uses JavaFX Media player to handle sound playback
    //Sound file in resources directory
    private void playMoveSound() {
        try {
            //Path to sound file in resources
            String soundFile = "/move_sound.wav";
            //Create Media object from sound file
            //requireNonNull ensures resource exists
            Media sound = new Media(Objects.requireNonNull(getClass().getResource(soundFile)).toExternalForm());
            //Create and start media player
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();


        } catch (Exception e) {
            //Log error if sound playback fails
            //Game can continue without sound
            System.err.println("Error playing move sound: " + e.getMessage());

        }
    }





    // customAI ---------------------------------------
    // simple chess ai
    //Makes a move for the simple AI by evaluating all possible moves
    //and selecting the one with the highest score
    private void makeSimpleAIMove() {
        //Get all possible legal moves for AI (black pieces)
        List<Move> legalMoves = getAllLegalMovesCustomAI(false);
        Move bestMove = null;
        //Initialize best score to minimum possible value
        int bestScore = Integer.MIN_VALUE;
        // int bestScore = Integer.MAX_VALUE;
        System.out.println("\n=== AI Move Evaluation ===");

        //Evaluate each possible move
        for (Move move : legalMoves) {
            //Store current board state before making test move
            String capturedPiece = boardCurrent.get(move.endCol, move.endRow);

            //Make temporary move on board
            boardCurrent.set(move.endCol, move.endRow, move.piece);
            boardCurrent.set(move.startCol, move.startRow, "null");

            //Calculate position score after move
            int score = simpleAI.evaluatePosition(boardCurrent, move);

            //Print move details
            System.out.printf("Move: %s from (%d,%d) to (%d,%d) - Score: %d %s%n",
                    move.piece,
                    move.startCol, move.startRow,
                    move.endCol, move.endRow,
                    score,
                    score > bestScore ? " (New Best!)" : "");



            //Restore board state
            boardCurrent.set(move.startCol, move.startRow, move.piece);
            boardCurrent.set(move.endCol, move.endRow, capturedPiece);

            // Update best move
            // if (score < bestScore) {
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        System.out.println("\nSelected Move:");
        System.out.printf("Piece: %s from (%d,%d) to (%d,%d) with score: %d%n%n",
                bestMove.piece,
                bestMove.startCol, bestMove.startRow,
                bestMove.endCol, bestMove.endRow,
                bestScore);

        //Apply the best move found
        applyMoveCustomAI(bestMove);
    }



    //Gets all legal moves for either white or black pieces
//Checks every possible move for each piece and validates it
    private List<Move> getAllLegalMovesCustomAI(boolean forWhite) {
        //Initialize list to store all legal moves
        List<Move> legalMoves = new ArrayList<>();
        //Iterate through all squares on the board
        for (int startRow = 0; startRow < BOARD_SIZE; startRow++) {
            for (int startCol = 0; startCol < BOARD_SIZE; startCol++) {
                //Get piece at current square
                String piece = boardCurrent.get(startCol, startRow);
                //Check if square has a piece of the current player's color
                if (!piece.equals("null") && piece.contains(forWhite ? "white" : "black")) {
                    //Check all possible destination squares
                    for (int endRow = 0; endRow < BOARD_SIZE; endRow++) {
                        for (int endCol = 0; endCol < BOARD_SIZE; endCol++) {
                            //Check if move is legal
                            if (simulateMoveProtectKing(pieces.get(piece), endCol, endRow)) {
                                //Add legal move to list
                                legalMoves.add(new Move(startCol, startRow, endCol, endRow, piece));
                            }
                        }
                    }
                }
            }
        }
        //Return list of all legal moves
        return legalMoves;
    }

    //Applies a move to the board, handling both the logical and visual updates
    //Manages piece captures, board state, and GUI updates
    private void applyMoveCustomAI(Move move) {
        //Get piece at destination square
        String capturedPiece = boardCurrent.get(move.endCol, move.endRow);

        //Handle capture, if present
        if (!capturedPiece.equals("null")) {
            //Remove captured piece from GUI
            ImageView capturedPieceView = imageViewMap.get(capturedPiece);
            gridPane.getChildren().remove(capturedPieceView);
            //Remove from piece tracking map
            imageViewMap.remove(capturedPiece);
        }

        //Update GUI for moving piece
        ImageView movingPieceView = imageViewMap.get(move.piece);
        //Remove from old position
        gridPane.getChildren().remove(movingPieceView);
        //Add to new position
        gridPane.add(movingPieceView, move.endCol, move.endRow);

        //Update board state
        boardCurrent.set(move.startCol, move.startRow, "null");
        boardCurrent.set(move.endCol, move.endRow, move.piece);

        // Update piece position
        Piece piece = pieces.get(move.piece);
        piece.setCol(move.endCol);
        piece.setRow(move.endRow);
        piece.setMoved(true);
    }

    // customAI ---------------------------------------

    //debug method
    private void printGridPaneContents() {
        System.out.println("\nGridPane Contents:");



        // Count only ImageView nodes that represent pieces
        long pieceCount = gridPane.getChildren().stream()
                .filter(node -> node instanceof ImageView)
                .count();

        System.out.println("Total pieces: " + pieceCount);

        // Create an 8x8 representation of the board
        String[][] board = new String[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = ".";  // Empty square
            }
        }

        // Fill in the pieces
        for (Node node : gridPane.getChildren()) {
            if (node instanceof ImageView) {
                Integer col = GridPane.getColumnIndex(node);
                Integer row = GridPane.getRowIndex(node);

                // Add null checks
                if (col == null) col = 0;
                if (row == null) row = 0;

                // Find which piece this ImageView represents
                String pieceName = "Unknown";
                for (Map.Entry<String, ImageView> entry : imageViewMap.entrySet()) {
                    if (entry.getValue() == node) {
                        pieceName = entry.getKey();
                        break;
                    }
                }

                board[row][col] = pieceName;
                System.out.println("Piece at [" + col + "," + row + "]: " + pieceName);
            }
        }

        // Print board representation with colors
        System.out.println("\nBoard Layout:");
        System.out.println("   a    b    c    d    e    f    g    h");
        for (int i = 0; i < 8; i++) {
            System.out.print((8-i) + " ");
            for (int j = 0; j < 8; j++) {
                String piece = board[i][j];
                if (piece.equals(".")) {
                    System.out.printf("%-5s", ".");  // Empty square
                } else {
                    // Get first two chars of piece name and color indicator
                    String pieceType = piece.substring(0, Math.min(2, piece.length()));
                    String color = piece.contains("white") ? "W" : "B";
                    System.out.printf("%-5s", pieceType + color); // e.g., "paw" or "knb"
                }
            }
            System.out.println(" " + (8-i));
        }
        System.out.println("   a    b    c    d    e    f    g    h\n");
    }

    //Secret mode activation for play against Stockfish (stockfish vs stockfish) : shift + 7
    private boolean secretMode = false;
    private static final KeyCombination SECRET_COMBO = new KeyCodeCombination(
            KeyCode.DIGIT7, KeyCombination.SHIFT_DOWN
    );


    private double pauseDuration = 1.0; // Default pause duration

    // Add these key combinations (to increase and decrease speed of delay between moves of stockfish)
// '=' key
    private static final KeyCombination INCREASE_SPEED =
            new KeyCodeCombination(KeyCode.EQUALS);
    // '-' key
    private static final KeyCombination DECREASE_SPEED =
            new KeyCodeCombination(KeyCode.MINUS);


    /**
     * Delay Move
     * (Requirement 6.3.0)
     */
    // Used to add delay between AI moves
    private void makeDelayedMove(Runnable action) {
        //Ensure UI updates happen on JavaFX Application Thread
        Platform.runLater(() -> {
            //Create pause transition with current duration setting
            PauseTransition pause = new PauseTransition(Duration.seconds(pauseDuration));
            pause.setOnFinished(event -> action.run());
            pause.play();
        });
    }


    /**
     * show speed alert
     * (Requirement 6.3.1)
     */
    //Displays a temporary alert showing the current move speed setting
    private void showSpeedAlert(String action) {
        //Ensure alert shows on JavaFX Application Thread
        Platform.runLater(() -> {
            //Create and configure alert dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Move Speed");
            alert.setHeaderText(null);
            //Message with current speed setting
            alert.setContentText(action + " move delay: " +
                    String.format("%.1f", pauseDuration) + " seconds");

            //Set up auto-dismiss
            PauseTransition alertDelay = new PauseTransition(Duration.seconds(.4));
            alertDelay.setOnFinished(event -> alert.close());

            //Show alert and start auto-dismiss timer
            alert.show();
            alertDelay.play();
        });
    }
    public static void main(String[] args) {

        launch(args);
    }


}
