package com.matchi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @EnableKafka est activé automatiquement par Spring Boot si spring-kafka est présent
// Si nécessaire, on peut l'ajouter explicitement ici
@SpringBootApplication
public class MatchiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatchiServiceApplication.class, args);
	}

}
