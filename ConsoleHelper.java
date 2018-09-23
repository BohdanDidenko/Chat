import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Класс для удобства работы с консолью
 * @version 1.0
 * @author Bogdan Didenko
 */

public class ConsoleHelper {
    private static BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    //Выводит сообщение message в консоль
    public static void writeMessage(String message){
        System.out.println(message);
    }

    //Считывает строку с консоли
    public static String readString(){
        while (true) {
            try {
                return bufferedReader.readLine();
            } catch (IOException e) {
                System.out.println("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
                continue;
            }
        }
    }

    //Возвращает введенное число
    public static int readInt(){
        while (true) {
            try {
                return Integer.parseInt(ConsoleHelper.readString());
            } catch (NumberFormatException e) {
                writeMessage("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");
                continue;
            }
        }
    }
}
