
package com.meetingiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class MeetingIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingIqApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void showApplicationUrl() {
        System.out.println();
        System.out.println("==========================================");
        System.out.println(" MeetingIQ Java Backend is running!");
        System.out.println(" URL: http://localhost:8080");
        System.out.println(" Meetings API: http://localhost:8080/meetings");
        System.out.println("==========================================");
        System.out.println();
    }
}
