import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class SocketServer {
    private Optional<Acceptor> acceptor = Optional.empty();
    private ConcurrentMap<Integer,Processor> processors = new ConcurrentHashMap<>();
    private boolean startedProcessingRequests = false;
    private boolean stoppedProcessingRequests = false;
    //startup
        //create acceptor, processor
    public void startup(boolean startProcessingRequests){
        synchronized (this) {
            createProcessorAndAcceptor();
            if(startProcessingRequests){
                startProcessorAndAcceptor();
            }
        }
    }
    private void startProcessorAndAcceptor(){
        synchronized (this){
            if (!startedProcessingRequests) {
                if(acceptor.isPresent()){
                    Acceptor acceptorInner = acceptor.get();
                    acceptorInner.startProcessors();
                }
                startedProcessingRequests = true;
            } else {
               System.out.println("already started");
            }
        }
    }
    private void createProcessorAndAcceptor(){
        EndPoint endPoint = new EndPoint("127.0.0.1", 9090);
        List<Processor> processorsForAcceptor = new ArrayList<>();
        acceptor = Optional.of(new Acceptor(endPoint));
        IntStream.range(0,10).forEach( value ->{
            Processor processor = new Processor();
            processors.putIfAbsent(value,processor);
            processorsForAcceptor.add(processor);
        });
        acceptor.get().addProcessor(processorsForAcceptor,"Test-acceptor");
    }

    //end

}
class Processor implements Runnable {
    private CountDownLatch startupLatch;
    private CountDownLatch shutdownLatch;
    private AtomicBoolean alive = new AtomicBoolean(true);
    private static final int connectionQueueSize = 100;
    private ArrayBlockingQueue<SocketChannel> newConnections;
    private Selector selector;
    private Map<String, SocketChannel> channels;

    Processor(){
        this.startupLatch = new CountDownLatch(1);
        this.newConnections = new ArrayBlockingQueue<>(connectionQueueSize);
        try {
            this.selector = Selector.open();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public boolean accept(SocketChannel socketChannel,
                          boolean mayBlock) throws InterruptedException {
        boolean accepted = false;
        if(newConnections.offer(socketChannel)) {
            accepted = true;
        }else if(mayBlock) {
            newConnections.put(socketChannel);
            accepted = true;
        }else {
            accepted = false;
        }
        if (accepted)
            wakeup();
        return accepted;
    }
    private void startupComplete(){
        shutdownLatch = new CountDownLatch(1);
        startupLatch.countDown();
    }
    private void shutdownComplete(){
        shutdownLatch.countDown();
    }
    private void closeAll() throws IOException {
        while (!newConnections.isEmpty()) {
            newConnections.poll().close();
        }
        for (Map.Entry<String, SocketChannel> channel : channels.entrySet()) {
            channel.getValue().close();
        }
        selector.close();
    }

    public boolean isRunning(){
        return this.alive.get();
    }
    @Override
    public void run(){
        startupComplete();
        try {
            while (isRunning()) {
                try {
                    // setup any new connections that have been queued up
                    configureNewConnections();
                    // register any new responses for writing

                } catch(Exception e) {
                    // We catch all the throwables here to prevent the processor thread from exiting. We do this because
                    // letting a processor exit might cause a bigger impact on the broker. This behavior might need to be
                    // reviewed if we see an exception that needs the entire broker to stop. Usually the exceptions thrown would
                    // be either associated with a specific socket channel or a bad request. These exceptions are caught and
                    // processed by the individual methods above which close the failing channel and continue processing other
                    // channels. So this catch block should only ever see ControlThrowables.
                    e.printStackTrace();
                }
            }
        } finally {

            //CoreUtils.swallow(closeAll(), this, Level.ERROR)
            shutdownComplete();
        }

    }
    private String connectionId(Socket socket){
        return socket.getLocalAddress().getHostAddress()+":"+socket.getInetAddress().getHostAddress();
    }
    private void register(String channelId, SocketChannel channel) throws ClosedChannelException {
        SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        this.channels.put(channelId, channel);
    }
    private void configureNewConnections() throws Exception {
            int connectionsProcessed = 0;
            while (connectionsProcessed < connectionQueueSize && !newConnections.isEmpty()) {
                SocketChannel channel = newConnections.poll();
                try {
                    register(connectionId(channel.socket()), channel);
                    connectionsProcessed += 1;
                }catch(Exception e){
                    channel.close();
                    throw new Exception("process fail during configure new connection");
                }
            }

    }
    private void wakeup() {
        this.selector.wakeup();
    }
}

class Acceptor implements Runnable{
    private CountDownLatch startupLatch;
    private CountDownLatch shutdownLatch;
    private EndPoint endPoint;
    private Selector nioSelector;
    private ServerSocketChannel serverChannel;
    private List<Processor> processors;
    private AtomicBoolean processorsStarted;
    private AtomicBoolean alive = new AtomicBoolean(true);

    Acceptor(EndPoint endPoint){
        try {
            this.endPoint = endPoint;
            startupLatch = new CountDownLatch(1);
            nioSelector = Selector.open();
            serverChannel = openSockChannel();
            processors = new ArrayList<>();
            processorsStarted = new AtomicBoolean();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void start(){
        Thread thread = new Thread(this);
        thread.setDaemon(false);
        thread.start();
    }
    public void stop(){
        this.alive.getAndSet(false);
        nioSelector.wakeup();

    }
    private ServerSocketChannel openSockChannel() throws IOException {
        InetSocketAddress address = new InetSocketAddress(this.endPoint.getHost(),this.endPoint.getPort());
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().setReceiveBufferSize(1024);
        try{
            channel.bind(address);
        }catch(SocketException e){
            e.printStackTrace();
        }
        return channel;
    }

    private void startupComplete(){
        shutdownLatch = new CountDownLatch(1);
        startupLatch.countDown();
    }
    private void shutdownComplete(){
        shutdownLatch.countDown();
    }

    public boolean isRunning(){
        return this.alive.get();
    }
    public synchronized void addProcessor(List<Processor> newProcessors, String processorThreadPrefix){
        this.processors.addAll(newProcessors);

    }
    public synchronized void startProcessors(){
        this.processors.stream().forEach( processor -> {
            processor.run();
        });
    }

    private Optional<SocketChannel> accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.socket().setTcpNoDelay(true);
        socketChannel.socket().setKeepAlive(true);
        return Optional.of(socketChannel);
    }
    private boolean assignNewConnection(SocketChannel socketChannel,Processor processor, boolean mayBlock) throws InterruptedException {
        if (processor.accept(socketChannel, mayBlock)) {
            return true;
        }else {
            return false;
        }
    }
    private void startProcessor(){
        this.processors.forEach(processor -> {
            Thread thread = new Thread(processor);
            thread.setDaemon(false);
            thread.start();
        });
    }
    @Override
    public void run() {
        try {
            this.serverChannel.register(nioSelector, SelectionKey.OP_ACCEPT);
            startupComplete();
            int currentProcessorIndex = 0;
            while(isRunning()) {
                try {
                    int ready = nioSelector.select(500);
                    if (ready > 0) {
                        Set<SelectionKey> keys = nioSelector.selectedKeys();
                        Iterator<SelectionKey> iter = keys.iterator();

                        while (iter.hasNext() && isRunning()) {
                            try {
                                SelectionKey key = iter.next();
                                iter.remove();

                                if (key.isAcceptable()) {
                                    SocketChannel socketChannel = accept(key).get();
                                    Processor processor = null;

                                    do {
                                        //retriesLeft -= 1
                                        synchronized(processors){
                                            // adjust the index (if necessary) and retrieve the processor atomically for
                                            // correct behaviour in case the number of processors is reduced dynamically
                                            currentProcessorIndex = currentProcessorIndex % processors.size();
                                            processor = processors.get(currentProcessorIndex);
                                        }
                                        currentProcessorIndex += 1;
                                    } while (!assignNewConnection(socketChannel, processor, 0 == 0));
                                } else {
                                    throw new Exception("Unrecognized key state for acceptor thread ");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

    } catch (ClosedChannelException e) {
            e.printStackTrace();
        }
    }
}

class EndPoint{
    private String connectionString;
    private String host;
    private int port;

    EndPoint(String host, int port){
        this.host = host;
        this.port = port;
    }

    public String getHost(){
        return this.host;
    }
    public int getPort(){
        return this.port;
    }
}
