import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GameServer {
    private static final int PORT = 12345;
    private static final int GRID_SIZE = 10;  // Assuming a 10x10 grid
    private static final String TREASURE = "T"; // Treasure symbol
    private static final String EMPTY = ".";   // Empty space symbol

    private static List<Player> players = new ArrayList<>();
    private static Map<Player, PrintWriter> playerWriters = new HashMap<>();

    private static final Random random = new Random();

    private static int treasureX;
    private static int treasureY;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT + "...");

            // Generate random treasure location
            treasureX = random.nextInt(GRID_SIZE);
            treasureY = random.nextInt(GRID_SIZE);
            System.out.println("Treasure is located at: (" + treasureX + ", " + treasureY + ")");

            while (true) {
                // waiting for a client to connect (a play to join the game)
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");

                // Create a new player and a thread to handle this player
                Player player = new Player(clientSocket);
                players.add(player);
                new Thread(new ClientHandler(player)).start();
            }
        } catch (IOException e) {
            e.printStackTrace(); // Consider replacing with a logging framework
        }
    }

    // Handle communication with each player
    private static class ClientHandler implements Runnable {
        private Player player;

        public ClientHandler(Player player) {
            this.player = player;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(player.socket.getInputStream()));
                PrintWriter out = new PrintWriter(player.socket.getOutputStream(), true);
                playerWriters.put(player, out);

                // Greet the player
                out.println("Welcome! Enter your name:");
                String playerName = in.readLine();
                player.setName(playerName);
                System.out.println("Player named " + playerName + " joined!");

                // Start broadcasting game state
                broadcastGameState();

                String command;
                while ((command = in.readLine()) != null) {
                    // Handle the player's command
                    handleCommand(command, player);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Handle player commands (UP, DOWN, LEFT, RIGHT, DIG)
        private void handleCommand(String command, Player player) {
            System.out.println("<" + player.getName() + ">" + " sends command: " + command.toUpperCase());

            switch (command.toUpperCase()) {
                case "UP":
                    player.moveUp();
                    break;
                case "DOWN":
                    player.moveDown();
                    break;
                case "LEFT":
                    player.moveLeft();
                    break;
                case "RIGHT":
                    player.moveRight();
                    break;
                case "DIG":
                    if (player.getX() == treasureX && player.getY() == treasureY) {
                        broadcastTreasureFound(player);
                    } else {
                        player.sendMessage("No treasure here. Keep searching!");
                    }
                    break;
                default:
                    player.sendMessage("Invalid command. Use UP, DOWN, LEFT, RIGHT, or DIG.");
                    break;
            }

            // Broadcast the updated game state after each command
            broadcastGameState();
        }
    }

    // Broadcast game state to all players
    private static void broadcastGameState() {
        // Get the current timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Build the game state message
        StringBuilder gameState = new StringBuilder("\nCurrent Game State (");

        // Include the timestamp in the message
        gameState.append(timestamp).append(") \n");

        int playersCount = players.size();
        gameState.append(playersCount);
        if (playersCount <= 1) {
            gameState.append(" active player.\n");

        } else {
            gameState.append(" active players.\n");
        }

        // Show player positions
        for (Player player : players) {
            gameState.append(player.getName())
                    .append(" is at position: ")
                    .append("(").append(player.getX()).append(", ").append(player.getY()).append(")\n");
        }

        // Send the game state to all players
        for (PrintWriter writer : playerWriters.values()) {
            writer.println(gameState.toString());
        }
    }

    // Broadcast the event when a player finds the treasure
    private static void broadcastTreasureFound(Player player) {
        String message = player.getName() + " has found the treasure!";
        for (PrintWriter writer : playerWriters.values()) {
            writer.println(message);
        }
        System.out.println(message);
    }

    // Player class to handle player-specific data
    private static class Player {
        private String name;
        private int x, y;
        private Socket socket;

        public Player(Socket socket) {
            this.socket = socket;
            this.x = random.nextInt(GRID_SIZE);  // Random starting position
            this.y = random.nextInt(GRID_SIZE);  // Random starting position
        }

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        // Move player in different directions
        public void moveUp() {
            if (y > 0) y--;
        }

        public void moveDown() {
            if (y < GRID_SIZE - 1) y++;
        }

        public void moveLeft() {
            if (x > 0) x--;
        }

        public void moveRight() {
            if (x < GRID_SIZE - 1) x++;
        }

        // Send a message to the player
        public void sendMessage(String message) {
            PrintWriter out = playerWriters.get(this);
            if (out != null) {
                out.println(message);
            }
        }
    }
}
