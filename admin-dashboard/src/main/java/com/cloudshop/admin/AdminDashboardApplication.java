package com.cloudshop.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Admin Dashboard Application
 * 
 * Simple admin interface for CloudShop management.
 * Deployed on EC2 instance for Week 1-2 AWS learning.
 */
@SpringBootApplication
public class AdminDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminDashboardApplication.class, args);
    }
}
