import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ClosableAttachedInfo {
    private SocketChannel channel;
    private SocketChannel associatedChannel;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private SelectionKey associatedKey;
  /*  private boolean isChannelClosed = false;
    private boolean isAssociatedChannelClosed = false;
*/


    public ClosableAttachedInfo(
            SocketChannel channel,
            SocketChannel associatedChannel,
            ByteBuffer readBuffer,
            ByteBuffer writeBuffer,
            SelectionKey associatedKey) {
        this.channel = channel;
        this.associatedChannel = associatedChannel;
        this.readBuffer = readBuffer;
        this.writeBuffer = writeBuffer;
        this.associatedKey = associatedKey;
    }

    public SelectionKey getAssociatedKey() {
        return associatedKey;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public SocketChannel getAssociatedChannel() {
        return associatedChannel;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

   /* public boolean isChannelClosed() {
        return isChannelClosed;
    }

    public void setChannelClosed(boolean channelClosed) {
        isChannelClosed = channelClosed;
    }

    public boolean isAssociatedChannelClosed() {
        return isAssociatedChannelClosed;
    }

    public void setAssociatedChannelClosed(boolean associatedChannelClosed) {
        isAssociatedChannelClosed = associatedChannelClosed;
    }
*/}
