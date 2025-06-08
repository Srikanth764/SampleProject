package com.example.crudapp.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.crudapp.CrudAppApplication; // Main application class

@CucumberContextConfiguration
@SpringBootTest(classes = CrudAppApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class CucumberSpringConfiguration {
    // This class provides the Spring Boot application context for Cucumber tests.
    // webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT (or RANDOM_PORT) is important
    // so the application starts a real web server.
    // DEFINED_PORT will use the port defined in application.properties (or 8080 by default).
}
