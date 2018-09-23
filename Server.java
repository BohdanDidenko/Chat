import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**Сервер - создает серверное сокетное соединение
 * @version 1.6
 * @author Bogdan Didenko
 */

public class Server {

    // Ключ - имя клиента
    // Значением - соединение с клиентом
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    /** MAIN **/
    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт сервера: ");
        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt()))
        {
            ConsoleHelper.writeMessage("Сервер запущен");

            //Слушает и принимает входящие сокетные соединения
            while (true) {
                Socket socket = serverSocket.accept();

                //Создает и запускает новый поток Handler, передавая в конструктор сокет
                if (socket != null) new Handler(socket).start();
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage(e.getMessage());
        }
    }

    //Отправляет сообщение message по всем соединениям из connectionMap
    public static void sendBroadcastMessage(Message message) {
        for (Connection connection : connectionMap.values()) {
            try {
                connection.send(message);
            }
            catch (IOException e) {
                ConsoleHelper.writeMessage("Сообщение не отправлено");
            }
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Установленно соединение с адресом " + socket.getRemoteSocketAddress());
            String userName = "";

            try (Connection connection = new Connection(socket)) {
                ConsoleHelper.writeMessage("Подключение к порту: " + connection.getRemoteSocketAddress());

                //Метод, реализующий рукопожатие с клиентом, возвращает имя нового клиента
                userName = serverHandshake(connection);

                //Уведомление всех участников чата о новом пользователе
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));

                //Уведомление нового пользователя о действующих участниках
                sendListOfUsers(connection, userName);

                //Запуск главного цикла обработки сообщений сервером
                serverMainLoop(connection, userName);


            } catch (ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Ошибка при обмене данными с удаленным адресом");
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Ошибка при обмене данными с удаленным адресом");
            }

            //После того как все исключения обработаны, удаляем запись из connectionMap
            //и отправлялем сообщение остальным пользователям
            if (userName != null) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {

            while (true) {
                // Формирование и отправка запроса имени пользователя
                connection.send(new Message(MessageType.NAME_REQUEST));

                // Ответ клиента
                Message message = connection.receive();

                // Проверка, что запрос с именем пользователя получен
                if (message.getType() == MessageType.USER_NAME) {

                    //Проверяем, что именя из ответа не пустое
                    if (message.getData() != null && !message.getData().isEmpty()) {

                        // и пользователь с таким именем еще не подключен
                        if (connectionMap.get(message.getData()) == null) {

                            // Добавление нового пользователя и соединение с ним в connectionMap
                            connectionMap.put(message.getData(), connection);

                            // Отправка клиенту сообщения, что его имя принято
                            connection.send(new Message(MessageType.NAME_ACCEPTED));

                            // Возврат полученного имени в качестве возвращаемого значения
                            return message.getData();
                        }
                    }
                }
            }
        }

        //Отправка новому участнику информации об остальных участниках чата
        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for (String key : connectionMap.keySet()) {
                if (!key.equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, key));
                }
            }
        }

        //Главный цикл обработки сообщений сервером
        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException{
            while(true){
                Message message = connection.receive();
                if(message != null && message.getType() == MessageType.TEXT) {
                    sendBroadcastMessage(new Message(message.getType(), userName + ": " + message.getData()));
                }
                else ConsoleHelper.writeMessage("Ошибка!");
            }
        }
    }
}
