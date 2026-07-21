package aura.music;

public class Launcher {
    private static final int PORT = 45678;

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        try {
            // Attempt to be the primary instance
            java.net.ServerSocket serverSocket = new java.net.ServerSocket(PORT, 10, java.net.InetAddress.getByName("127.0.0.1"));
            
            // Spawn a daemon thread to listen for future instances passing arguments
            Thread listenerThread = new Thread(() -> {
                while (true) {
                    try {
                        java.net.Socket clientSocket = serverSocket.accept();
                        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream()));
                        String arg = in.readLine();
                        if (arg != null && !arg.isEmpty()) {
                            Main.handleExternalArgument(arg);
                        }
                        clientSocket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (java.net.BindException e) {
            // Another instance is already running
            System.out.println("AuraMusic is already running. Forwarding arguments to primary instance...");
            if (args.length > 0) {
                try {
                    java.net.Socket socket = new java.net.Socket("127.0.0.1", PORT);
                    java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
                    out.println(String.join(" ", args));
                    socket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            System.exit(0);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                System.setProperty("javafx.macosx.windowScale", "1.0");
                System.setProperty("prism.allowhidpi", "true");
                System.setProperty("javafx.visualDpi", "120");
            }
        } catch (SecurityException e) {
        }
        // Start the main application
        Main.main(args);
    }
}
