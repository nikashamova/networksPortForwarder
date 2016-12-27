import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class ClosableForwarder {
    public final static int MAX_LENGTH = 1024;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private InetSocketAddress address;

    public static void main(String[] args) {
        try {
            new ClosableForwarder(80, "nsu.ru.", 2222).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ClosableForwarder(int lPort, String lHostName, int rPort) throws IOException {
        try {
            selector = Selector.open();
            address = new InetSocketAddress(lHostName, lPort);
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(rPort)); //
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        SelectionKey key;
        while (true) {
            try {
                int noOfKeys;
                noOfKeys = selector.select();
                if (noOfKeys == 0) {
                    continue;
                }
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();

                    if (!key.isValid()) {
                        iterator.remove();
                        continue;
                    }
                    if (key.isAcceptable()) {
                        forwardAccept();
                    }
                    if (key.isConnectable()) {
                        forwardConnect(key);
                    }
                    if (key.isWritable()) {
                        forwardWrite(key);
                    }
                    if (key.isReadable()) {
                        forwardRead(key);
                    }
                    iterator.remove();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CancelledKeyException e) {
                /*if (key != null) {
                    key.channel().close();
                }
                if (((key != null ? key.attachment() : null)) != null) {
                    ((ClosableAttachedInfo) key.attachment()).setChannelClosed(true);
                }
                if (((key != null ? key.attachment() : null)) != null) {
                    ((ClosableAttachedInfo) ((ClosableAttachedInfo) key.attachment()).getAssociatedKey().attachment()).setAssociatedChannelClosed(true);
                }*/

            }
        }
    }

    private void forwardAccept() throws IOException {
        ByteBuffer bufferToClient = ByteBuffer.allocate(MAX_LENGTH);
        ByteBuffer bufferToServer = ByteBuffer.allocate(MAX_LENGTH);

        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);

        SocketChannel serverChannel = SocketChannel.open();
        serverChannel.configureBlocking(false);

        SelectionKey clientSelectionKey;
        SelectionKey serverSelectionKey;

        if (serverChannel.connect(address)) {
            serverSelectionKey = serverChannel.register(selector, SelectionKey.OP_READ);
            clientSelectionKey = clientChannel.register(selector, SelectionKey.OP_READ);
        } else {
            serverSelectionKey = serverChannel.register(selector, SelectionKey.OP_CONNECT);
            clientSelectionKey = clientChannel.register(selector, 0);
        }

        ClosableAttachedInfo clientAttached = new ClosableAttachedInfo(clientChannel, serverChannel, bufferToClient, bufferToServer, serverSelectionKey);
        ClosableAttachedInfo serverAttached = new ClosableAttachedInfo(serverChannel, clientChannel, bufferToServer, bufferToClient, clientSelectionKey);

        clientSelectionKey.attach(clientAttached);
        serverSelectionKey.attach(serverAttached);
    }

    private void forwardConnect(SelectionKey key) throws IOException {
        ClosableAttachedInfo attached = (ClosableAttachedInfo) key.attachment();
        attached.getChannel().finishConnect();
        int ops = 0;
        if (attached.getWriteBuffer().hasRemaining()) {
            ops = SelectionKey.OP_WRITE;
        }
        ops |= SelectionKey.OP_READ;
        key.interestOps(ops);
    }

    private void forwardRead(SelectionKey key) throws IOException {
        ClosableAttachedInfo attached = (ClosableAttachedInfo) key.attachment();
        ByteBuffer readBuffer = attached.getReadBuffer();

        int read;
        read = attached.getChannel().read(readBuffer);

        if (read < 0){
            attached.getChannel().close();
            ((ClosableAttachedInfo)attached.getAssociatedKey().attachment()).getChannel().close();
           /* attached.setChannelClosed(true);
            ((ClosableAttachedInfo) attached.getAssociatedKey().attachment()).setAssociatedChannelClosed(true);*/
            return;
        }
        if (!readBuffer.hasRemaining()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
        }
        //if (!attached.isAssociatedChannelClosed())
        attached.getAssociatedKey().interestOps(attached.getAssociatedKey().interestOps() | SelectionKey.OP_WRITE);

    }

    private void forwardWrite(SelectionKey key) throws IOException {

        ClosableAttachedInfo attached = (ClosableAttachedInfo) key.attachment();
        ByteBuffer writeBuffer = attached.getWriteBuffer();

        try {
            writeBuffer.flip();
            attached.getChannel().write(writeBuffer);
            writeBuffer.compact();

        } catch (IOException e) {
            attached.getChannel().close();
            //if (!attached.isAssociatedChannelClosed())
                attached.getAssociatedChannel().close();
            return;
        }
        //if (!writeBuffer.hasRemaining()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

        //}
        attached.getAssociatedKey().interestOps(attached.getAssociatedKey().interestOps() | SelectionKey.OP_READ);
    }
}
