import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 12345;
    private Map<String, Player> players = new HashMap<>();
    private Set<Position> treasures = new HashSet<>();

    public static void main(String[] args) {
        new GameServer().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            // Add some treasures to the game map for testing
            treasures.add(new Position(2, 2));
            treasures.add(new Position(5, 7));
            treasures.add(new Position(9, 3));

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastGameState() {
        for (String playerName : players.keySet()) {
            Player player = players.get(playerName);
            broadcast("UPDATE: PLAYER " + playerName + " " + player.getPosition().x + " " + player.getPosition().y);
        }
        for (Position treasure : treasures) {
            broadcast("UPDATE: TREASURE " + treasure.x + " " + treasure.y);
        }
    }

    private void broadcast(String message) {
        for (Player player : players.values()) {
            player.getOut().println(message);
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private String playerName;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Welcome! Enter your name:");
                playerName = in.readLine();
                Player player = new Player(playerName, new Position(0, 0));
                player.setOut(out);
                players.put(playerName, player);
                broadcast(playerName + " has joined the game!");

                broadcastGameState();

                String input;
                while ((input = in.readLine()) != null) {
                    handleCommand(input, player);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (playerName != null) {
                    players.remove(playerName);
                    broadcast(playerName + " has left the game.");
                    broadcastGameState();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleCommand(String command, Player player) {
            switch (command.toUpperCase()) {
                case "UP":
                    player.move(0, -1);
                    break;
                case "DOWN":
                    player.move(0, 1);
                    break;
                case "LEFT":
                    player.move(-1, 0);
                    break;
                case "RIGHT":
                    player.move(1, 0);
                    break;
                case "DIG":
                    if (treasures.remove(player.getPosition())) {
                        broadcast(player.getName() + " found a treasure!");
                    } else {
                        out.println("No treasure here!");
                    }
                    break;
                default:
                    out.println("Invalid command!");
            }
            broadcastGameState();
        }
    }
}

class Player {
    private String name;
    private Position position;
    private PrintWriter out;

    public Player(String name, Position position) {
        this.name = name;
        this.position = position;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public PrintWriter getOut() {
        return out;
    }

    public Position getPosition() {
        return position;
    }

    public void move(int dx, int dy) {
        position.move(dx, dy);
    }

    public String getName() {
        return name;
    }
}

class Position {
    int x, y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
        x = Math.max(0, Math.min(9, x));
        y = Math.max(0, Math.min(9, y));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
