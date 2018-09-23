
import java.io.Serializable;

/**
 * Данные которые одна сторона отправляет, а вторая принимает
 * @version 1.0
 * @author Bogdan Didenko
 */

public class Message implements Serializable {
    private final MessageType type;
    private final String data;

    public Message(MessageType type) {
        this.type = type;
        data = null;
    }

    public Message(MessageType type, String data) {
        this.type = type;
        this.data = data;
    }

    // MessageType type - содержит тип сообщения
    public MessageType getType() {
        return type;
    }

    // String data - содержит данные сообщения
    public String getData() {
        return data;
    }
}
