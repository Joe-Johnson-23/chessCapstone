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

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
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

    //Secret mode activation for play against Stockfish (stockfish vs stockfish) : shift + 7
    private boolean secretMode = false;
    private static final KeyCombination SECRET_COMBO = new KeyCodeCombination(
            KeyCode.DIGIT7, KeyCombination.SHIFT_DOWN
    );



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
                piece.highlightValidMoves(boardCurrent.getStiles(), boardCurrent.getBoard(), threatenedSquares, this);

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
            //Print the current board in FEN format
            System.out.println(boardCurrent.boardToFEN(pieces, isWhiteTurn, enPassantTile, halfMoveClock, numberOfMoves));
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
            event.consume();
            shutdown();
        });


        scene.setOnKeyPressed(event -> {
            if (SECRET_COMBO.match(event)) {
                secretMode = !secretMode;
                String message = secretMode ? "Secret Mode Activated: Stockfish vs Stockfish"
                        : "Secret Mode Deactivated";



                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Secret Mode");
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
                if (secretMode && isWhiteTurn) {

                    makeEngineMove();
                    switchTurn();
                }
            }
        });
    }



    private void shutdown() {

        System.out.println("Exiting application...");
        Platform.exit();
        System.exit(0);
    }

    private void switchTurn() {



        isWhiteTurn = !isWhiteTurn;
        //boardCurrent.printBoardState();
        playMoveSound();


        calculateThreatenedSquares();
        updateCheckStatus();

        if(checkForDraw()){
            return;
        };

        if (isCheckmate()) {
            System.out.println(isWhiteTurn ? "Black wins by checkmate!" : "White wins by checkmate!");
            return;
        }



        if (playAgainstStockfish) {
            if (!isWhiteTurn || secretMode) {
                if (engine != null) {
                    makeDelayedMove(() -> {
                        makeEngineMove();
                        switchTurn();
                    });
                } else {
                    System.err.println("Cannot make engine move: Chess engine is not initialized!");
                }
            }
        } else if (!isWhiteTurn && playAgainstSimpleAI) {
            makeDelayedMove(() -> {
                makeSimpleAIMove();
                switchTurn();
            });
        } else {


        }
    }

    private void makeDelayedMove(Runnable action) {
        Platform.runLater(() -> {
            PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
            pause.setOnFinished(event -> action.run());
            pause.play();
        });
    }


    private void makeEngineMove() {


        if (engine == null) {
            System.err.println("Chess engine is not initialized!");
            return;
        }

        try {
            //current board state to FEN notation
            String fen = boardCurrent.boardToFEN(pieces, isWhiteTurn, enPassantTile, halfMoveClock, numberOfMoves);
            //commands to Stockfish engine
            engine.sendCommand("position fen " + fen);

            //different depth for secret mode
            int depth = stockfishDepth;
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

    private String getBestMoveFromEngine() {
        try {
            String line;
            while ((line = engine.readLine()) != null) {
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

                //Piece movement
                Platform.runLater(() -> {
                    //Create and configure the transition
                    TranslateTransition transition = new TranslateTransition(Duration.millis(500), movingPieceView);
                    transition.setFromX(0);
                    transition.setFromY(0);
                    transition.setToX((endCol - startCol) * TILE_SIZE);
                    transition.setToY((endRow - startRow) * TILE_SIZE);

                    //Play the transition
                    transition.play();

                    //After the transition completes, update the piece's position
                    transition.setOnFinished(e -> {
                        gridPane.getChildren().remove(movingPieceView);
                        gridPane.add(movingPieceView, endCol, endRow);
                        movingPieceView.setTranslateX(0);
                        movingPieceView.setTranslateY(0);
                    });
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
        calculateThreatenedSquares();
        boolean whiteInCheck = whiteKing.isInCheck(squaresThreatenedByBlack);
        boolean blackInCheck = blackKing.isInCheck(squaresThreatenedByWhite);

        // System.out.println("White in check: " + whiteInCheck);
        // System.out.println("Black in check: " + blackInCheck);

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

        String rookPiece = moveRook(startRow, rookStartCol, rookEndCol);

        Piece rook = pieces.get(rookPiece);
        if (rook != null) {
            rook.setMoved(true);
        }
    }

    private void handleEnPassant(int startCol, int startRow, int endCol, int endRow) {
        String capturedPawn = boardCurrent.get(endCol, startRow);
        ImageView capturedPawnView = imageViewMap.get(capturedPawn);

        gridPane.getChildren().remove(capturedPawnView);
        imageViewMap.remove(capturedPawn);
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
        ArrayList<Tile> threatenedSquares = isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite;

        // First check if king is in check
        if (!currentKing.isInCheck(threatenedSquares)) {
            return false;
        }

        // Check if any piece (including the king) can make a legal move
        if (!hasNoLegalMoves()) {  // This was the issue - we were returning false when there were no legal moves
            return false;  // If there ARE legal moves, it's not checkmate
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

        //System.out.println("Stockfish path: " + stockfishPath);

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

    private boolean checkForDraw() {
        boolean draw = checkForThreefoldRepetition() || halfMoveClock == 50 || isStalemate();
        if(draw) {
            handleDraw();
        }
        return draw;
    }

    private boolean checkForThreefoldRepetition() {
        String positionKey = getPositionKey();
        int count = positionCounts.getOrDefault(positionKey, 0) + 1;
        positionCounts.put(positionKey, count);
        return count >= 3;
    }

    //Stalemate ---------------------------
    private boolean isStalemate() {
        if (hasNoLegalMoves()) {
            King currentKing = isWhiteTurn ? whiteKing : blackKing;
            ArrayList<Tile> threatenedSquares = isWhiteTurn ? squaresThreatenedByBlack : squaresThreatenedByWhite;
            return !currentKing.isInCheck(threatenedSquares);
        }
        return false;
    }

    private void handleDraw() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Draw");
            alert.setHeaderText(null);
            alert.setContentText("DRAW!");
            alert.showAndWait();
            handleGameEnd("Better luck next time!");
        });
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


    //Stalemate ---------------------------




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
                primaryStage.close();
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
            Media sound = new Media(Objects.requireNonNull(getClass().getResource(soundFile)).toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();


        } catch (Exception e) {
            System.err.println("Error playing move sound: " + e.getMessage());

        }
    }





    // customAI ---------------------------------------
    // simple chess ai
    private void makeSimpleAIMove() {
        List<Move> legalMoves = getAllLegalMoves(false);
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        // int bestScore = Integer.MAX_VALUE;
        System.out.println("\n=== AI Move Evaluation ===");

        for (Move move : legalMoves) {
            // Save current board state
            String capturedPiece = boardCurrent.get(move.endCol, move.endRow);

            // Make move
            boardCurrent.set(move.endCol, move.endRow, move.piece);
            boardCurrent.set(move.startCol, move.startRow, "null");

            // Evaluate position after move
            int score = simpleAI.evaluatePosition(boardCurrent, move);

            // Print move details
            System.out.printf("Move: %s from (%d,%d) to (%d,%d) - Score: %d %s%n",
                    move.piece,
                    move.startCol, move.startRow,
                    move.endCol, move.endRow,
                    score,
                    score > bestScore ? " (New Best!)" : "");

            if (!capturedPiece.equals("null")) {
                System.out.println("  -> Captures: " + capturedPiece);
            }

            // Restore board state
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

        applyMove(bestMove);
    }



    //   method to get all legal moves
    private List<Move> getAllLegalMoves(boolean forWhite) {
        List<Move> legalMoves = new ArrayList<>();

        for (int startRow = 0; startRow < BOARD_SIZE; startRow++) {
            for (int startCol = 0; startCol < BOARD_SIZE; startCol++) {
                String piece = boardCurrent.get(startCol, startRow);
                if (!piece.equals("null") && piece.contains(forWhite ? "white" : "black")) {
                    for (int endRow = 0; endRow < BOARD_SIZE; endRow++) {
                        for (int endCol = 0; endCol < BOARD_SIZE; endCol++) {
                            if (simulateMoveProtectKing(pieces.get(piece), endCol, endRow)) {
                                legalMoves.add(new Move(startCol, startRow, endCol, endRow, piece));
                            }
                        }
                    }
                }
            }
        }

        return legalMoves;
    }

    // method to apply a move
    private void applyMove(Move move) {
        String capturedPiece = boardCurrent.get(move.endCol, move.endRow);

        // Handle capture
        if (!capturedPiece.equals("null")) {
            ImageView capturedPieceView = imageViewMap.get(capturedPiece);
            gridPane.getChildren().remove(capturedPieceView);
            imageViewMap.remove(capturedPiece);
        }

        // Move the piece
        ImageView movingPieceView = imageViewMap.get(move.piece);
        gridPane.getChildren().remove(movingPieceView);
        gridPane.add(movingPieceView, move.endCol, move.endRow);

        // Update board state
        boardCurrent.set(move.startCol, move.startRow, "null");
        boardCurrent.set(move.endCol, move.endRow, move.piece);

        // Update piece position
        Piece piece = pieces.get(move.piece);
        piece.setCol(move.endCol);
        piece.setRow(move.endRow);
        piece.setMoved(true);
    }

    // customAI ---------------------------------------






    public static void main(String[] args) {

        launch(args);
    }


}
