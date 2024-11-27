package me.chessCapstone;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;



/**
 * Stockfish
 * (Requirement 4.1.0, 4.1.1)
 */
/**
 * Manages communication with the Stockfish engine,
 * handling engine initialization,
 * command sending, and output reading through buffered streams
 */
public class ChessEngine {
    //Process to run the Stockfish engine
    private Process engineProcess;
    //Reader to get output from the engine
    private BufferedReader processReader;
    //Writer to send commands to the engine
    private BufferedWriter processWriter;
    //Executor service for running background tasks
    private ExecutorService executor;
    //Queue to store engine output
    // The outputQueue is a BlockingQueue that acts as a buffer
    // between the background thread reading from Stockfish and
    // the main thread wanting to get responses.
    private BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();

    public ChessEngine(String stockfishRelativePath) throws IOException {
        //Get the current project directory
        String projectDir = System.getProperty("user.dir");
        //Construct the path to the resources folder
        Path resourcesPath = Paths.get(projectDir, "src", "main", "resources", "stockfish");
        //Resolve the full path to the Stockfish executable
        Path stockfishPath = resourcesPath.resolve(stockfishRelativePath);



        //Create a File object for the Stockfish executable
        File stockfishFile = stockfishPath.toFile();
        //Check if the Stockfish file exists
        if (!stockfishFile.exists()) {
            throw new FileNotFoundException("Stockfish binary not found at: " + stockfishPath);
        }
        //Ensure the Stockfish file is executable
        if (!stockfishFile.canExecute()) {
            stockfishFile.setExecutable(true);
        }

        //Start the Stockfish process
        //Use ProcessBuilder with command list
        ProcessBuilder pb = new ProcessBuilder(stockfishPath.toString());
        engineProcess = pb.start();
        //Set up input reader from the engine
        processReader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
        //Set up output writer to the engine
        processWriter = new BufferedWriter(new OutputStreamWriter(engineProcess.getOutputStream()));
        //Create a single-threaded executor for background tasks
        executor = Executors.newSingleThreadExecutor();
        //Start reading output from the engine in the background
        startReadingOutput();
    }


    /**
     * Stockfish start reading output
     * (Requirement 4.1.2)
     */
    //StartReadingOutput, handleOutput, readLine
    //Prevents blocking: The background thread can continuously read Stockfish's output without waiting for the main thread
    //Provides thread safety: The BlockingQueue safely handles communication between the background and main threads
    //Buffers responses: If Stockfish sends multiple lines of output, they're queued up until the main thread is ready to read them
    private void startReadingOutput() {
        //Submit a task to the executor to continuously read engine output
        executor.submit(() -> {
            try {
                String line;

                //Read lines from the engine until the stream ends
                while ((line = processReader.readLine()) != null) {
                    System.out.println("startReadingOutput "+line);
                    handleOutput(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * handle output from startReadingOutput
     * (Requirement 4.1.3)
     */
    private void handleOutput(String output) {
        //Add the engine output to the queue
        System.out.println("handleOutput: "+output);
        //Adds element to queue
        //Non-blocking operation
        //Returns immediately
        outputQueue.offer(output);
    }


    /**
     * readLine called by getBestMoveFromEngine in ChessGame
     * (Requirement 4.1.4)
     */
    public String readLine() throws IOException {
        try {
            //Try to get a line from the output queue, waiting up to 5 seconds
            //Tries to retrieve and remove head of queue
            //Blocking operation with timeout
            //Waits up to specified time for element
            return outputQueue.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //If interrupted, re-interrupt the thread and throw an IOException
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for engine output", e);
        }
    }

    /**
     * sendCommand- Writes commands to the engine from makeEngineMove in ChessGame
     * (Requirement 4.1.5)
     */
    public void sendCommand(String command) throws IOException {
        //Print the command being sent (for debugging)
        System.out.println("Sending: " + command);
        //Write the command to the engine, followed by a newline
        processWriter.write(command + "\n");
        //Ensure the command is sent immediately
        processWriter.flush();
    }


    public void quit() throws IOException, InterruptedException {
        //Send the quit command to the engine
        sendCommand("quit");
        //Wait for the engine process to terminate
        engineProcess.waitFor();
        //Shut down the executor service
        executor.shutdown();
        //Wait up to 5 seconds for the executor to finish
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }


}

