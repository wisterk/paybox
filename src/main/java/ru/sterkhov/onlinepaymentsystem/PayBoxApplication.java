package ru.sterkhov.onlinepaymentsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа в приложение онлайн-платёжной системы.
 */
@SpringBootApplication
public class PayBoxApplication {

    /**
     * Запускает Spring Boot приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(PayBoxApplication.class, args);
    }
}
