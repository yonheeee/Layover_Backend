package com.ssafy.layover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LayoverBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LayoverBackendApplication.class, args);
	}

}
