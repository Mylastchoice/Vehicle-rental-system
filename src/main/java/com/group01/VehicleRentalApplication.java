package com.group01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VehicleRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicleRentalApplication.class, args);
        System.out.println("\n====================================================");
        System.out.println("  UNG DUNG DA CHAY! BAM VAO LINK DE MO WEB:");
        System.out.println("  --> Trang chu:   http://localhost:8080");
        System.out.println("  --> Trang Admin: http://localhost:8080/admin/bookings");
        System.out.println("====================================================\n");
    }
}