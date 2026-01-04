package com.cloudshop.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Order Service Application
 * 
 * TODO: Will be implemented in Weeks 3-6
 * 
 * AWS Services to be integrated:
 * - DynamoDB: Order storage
 * - SQS: Order processing queue
 * - SNS: Order notifications
 * - Lambda: Inventory updates
 */
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
