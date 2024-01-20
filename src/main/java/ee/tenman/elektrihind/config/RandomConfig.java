package ee.tenman.elektrihind.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class RandomConfig {
	
	@Bean
	public Random randomGenerator() {
		return new Random();
	}
}
