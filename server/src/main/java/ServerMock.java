public class ServerMock {
    public static void main(String ... args) throws InterruptedException {
        System.out.println("Server starting...");
        SocketServer server = new SocketServer();
        server.startup(true);
    }
}
