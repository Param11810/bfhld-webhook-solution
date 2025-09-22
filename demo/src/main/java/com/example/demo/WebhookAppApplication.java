package com.example.demo;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@SpringBootApplication
public class WebhookAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookAppApplication.class, args);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    public ApplicationRunner runner(WebClient webClient) {
        return args -> {
            Map<String, Object> requestBody = Map.of(
                    "name", "John Doe",
                    "regNo", "REG12347",
                    "email", "john@example.com"
            );

            Map<String, Object> generateResp = webClient.post()
                    .uri("https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (generateResp == null) {
                System.err.println("No response from generateWebhook");
                return;
            }

            System.out.println("generate response: " + generateResp);

            // adjust keys below if API uses different names
            String webhookUrl = String.valueOf(generateResp.get("webhook"));
            String accessToken = String.valueOf(generateResp.get("accessToken"));

            System.out.println("webhookUrl: " + webhookUrl);
            System.out.println("accessToken: " + accessToken);

            // ====== Put ONE of these finalQuery strings (MySQL or PostgreSQL) ======
            // MySQL version (recommended unless told otherwise):
            String finalQuery = "SELECT p.AMOUNT AS SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, d.DEPARTMENT_NAME FROM PAYMENTS p JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID WHERE DAY(p.PAYMENT_TIME) <> 1 AND p.AMOUNT = (SELECT MAX(AMOUNT) FROM PAYMENTS WHERE DAY(PAYMENT_TIME) <> 1);";

            // Postgres version (alternative):
            // String finalQuery = "SELECT p.amount AS salary, e.first_name || ' ' || e.last_name AS name, EXTRACT(YEAR FROM age(current_date, e.dob))::int AS age, d.department_name FROM payments p JOIN employee e ON p.emp_id = e.emp_id JOIN department d ON e.department = d.department_id WHERE EXTRACT(DAY FROM p.payment_time) <> 1 AND p.amount = (SELECT MAX(amount) FROM payments WHERE EXTRACT(DAY FROM payment_time) <> 1);";

            Map<String,String> submitBody = Map.of("finalQuery", finalQuery);

            String submitUrl = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";

            Map<String, Object> submitResp = webClient.post()
                    .uri(submitUrl)
                    .header("Authorization", accessToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(submitBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            System.out.println("Submission response: " + submitResp);
        };
    }
}
