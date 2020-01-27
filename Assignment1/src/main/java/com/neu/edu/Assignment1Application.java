package com.neu.edu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
@EnableJpaAuditing
@SpringBootApplication
public class Assignment1Application {

	public static void main(String[] args) {
		SpringApplication.run(Assignment1Application.class, args);
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder()
	{
		return new BCryptPasswordEncoder();
	}
}

