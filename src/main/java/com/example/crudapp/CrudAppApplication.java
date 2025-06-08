package com.example.crudapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the CRUD App.
 * This class serves as the entry point for the Spring Boot application.
 */
@SpringBootApplication
public class CrudAppApplication {

	/**
	 * The main method that starts the Spring Boot application.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(CrudAppApplication.class, args);
	}

}
