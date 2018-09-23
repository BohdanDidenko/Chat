
import java.io.IOException;
import java.net.Socket;

/**
 * Клиент
 * @version 1.7
 * @author Bogdan Didenko
 */

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    /** MAIN **/
    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Введите адрес сервера: ");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        ConsoleHelper.writeMessage("Введите порт сервера: ");
        return ConsoleHelper.readInt();
    }

    protected String getUserName(){
        ConsoleHelper.writeMessage("Введите имя пользователя: ");
        return ConsoleHelper.readString();
    }

    //Создает и возвращать новый объект класса SocketThread
    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    //Создает новое текстовое сообщение, используя переданный текст и отправляет его серверу через соединение connection
    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Ошибка отправки");
            clientConnected = false;
        }
    }

    /**Создает вспомогательный поток SocketThread, ожидает пока тот установит соединение с сервером, а после этого
     в цикле считывает сообщения с консоли и отправлять их серверу.
     Условием выхода из цикла будет отключение клиента или ввод пользователем команды 'exit'.
     */
    public void run() {
        // Создает новый сокетный поток с помощью метода getSocketThread
        SocketThread socketThread = this.getSocketThread();

        // Помечает созданный поток как daemon, для того, чтобы при выходе
        // из программы вспомогательный поток прервался автоматически.
        socketThread.setDaemon(true);

        // Запускает вспомогательный поток
        socketThread.start();

        // Заставить текущий поток ожидать
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Ошибка");
                return;
            }
        }

        if (this.clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");

            //Считывать сообщения с консоли пока клиент подключен. Если будет введена команда 'exit', то выйди из цикла
            while (this.clientConnected) {
                String letter = ConsoleHelper.readString();
                if (letter.equals("exit")) this.clientConnected = false;
                else this.sendTextMessage(letter);
            }
        }
        else
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
    }

    //Отвечать за поток, устанавливающий сокетное соединение и читающий сообщения сервера
    public class SocketThread extends Thread {

        @Override
        public void run() {
            String serverAddress = getServerAddress();
            int serverPort = getServerPort();
            try {
                // Создай новый объект класса java.net.Socket c запросом сервера и порта 1111
                Socket socket = new Socket(serverAddress,serverPort);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            }
            catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }

        // Выводит текст message в консоль
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        // Выводит в консоль имя нового участника чата
        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("участник " + userName + " присоединился к чату");
        }

        // Выводит в консоль имя участника покинувшего чат
        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("участник " + userName + " покинул чат");
        }

        /** Устанавливает значение поля clientConnected класса Client в соответствии с
         переданным параметром.
         Оповещает (пробуждать ожидающий) основной поток класса Client **/
        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;

            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        // Реализующий "рукопожатие" клиента с сервером
        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                // Получает сообщения, используя соединение connection
                Message message = connection.receive();

                switch (message.getType()) {

                    // Сервер запросил имя
                    case NAME_REQUEST: {

                        // Запрос ввода имени пользователя с помощью метода getUserName()
                        String userName = getUserName();

                        // Отправка сообщения серверу с типом USER_NAME и введенным именем
                        connection.send(new Message(MessageType.USER_NAME, userName));
                        break;
                    }

                    // Сервер принял имя
                    case NAME_ACCEPTED: {

                        // Уведомление главного потока, что сервир принял имя.
                        notifyConnectionStatusChanged(true);
                        return;
                    }

                    // Любой другой ответ
                    default: {
                        throw new IOException("Unexpected MessageType");
                    }
                }
            }
        }

        // Главный цикл обработки сообщений сервера
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {

                // Получения сообщения, используя соединение connection
                Message message = connection.receive();

                switch (message.getType()) {
                    case TEXT:
                        processIncomingMessage(message.getData());
                        break;
                    case USER_ADDED:
                        informAboutAddingNewUser(message.getData());
                        break;
                    case USER_REMOVED:
                        informAboutDeletingNewUser(message.getData());
                        break;
                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }
    }
}
