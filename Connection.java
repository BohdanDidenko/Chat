import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Класс соединения между клиентом и сервером
 * @version 1.2
 * @author Bogdan Didenko
 */

public class Connection implements Closeable{
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    //Cериализация сообщений message в ObjectOutputStream
    public void send(Message message) throws IOException{
        synchronized (out){
            out.writeObject(message);
        }
    }

    //Десериализация данныех из ObjectInputStream
    public Message receive() throws IOException, ClassNotFoundException{
        synchronized (in){
            Message message = (Message) in.readObject();
            return message;
        }
    }

    //Возвращающает удаленный адрес сокетного соединения
    public SocketAddress getRemoteSocketAddress(){
        return socket.getRemoteSocketAddress();
    }

    //Освобождает ресурсы
    @Override
    public void close() throws IOException {
        out.close();
        in.close();
        socket.close();
    }
}
