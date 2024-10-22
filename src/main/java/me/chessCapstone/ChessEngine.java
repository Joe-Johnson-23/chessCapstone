package me.chessCapstone;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

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

    private void startReadingOutput() {
        //Submit a task to the executor to continuously read engine output
        executor.submit(() -> {
            try {
                String line;
                //Read lines from the engine until the stream ends
                while ((line = processReader.readLine()) != null) {
                    handleOutput(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleOutput(String output) {
        //Add the engine output to the queue
        outputQueue.offer(output);
    }

    public String readLine() throws IOException {
        try {
            //Try to get a line from the output queue, waiting up to 5 seconds
            return outputQueue.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //If interrupted, re-interrupt the thread and throw an IOException
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for engine output", e);
        }
    }

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

