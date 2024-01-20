package ee.tenman.elektrihind.apollo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class UserService {
	
	@Getter
	private final List<User> users = new ArrayList<>();
	@Value("${apollo-kino.username-password-pairs}")
	private String usernamePasswordPairs;
	@Resource
	private Random randomGenerator;
	
	@PostConstruct
	public void initializeUsers() {
		if (usernamePasswordPairs == null || usernamePasswordPairs.isEmpty()) {
			log.error("usernamePasswordPairs is empty or null");
			return;
		}
		
		String[] pairs = usernamePasswordPairs.split(";");
		for (String pair : pairs) {
			String[] keyValue = pair.split(":");
			if (keyValue.length == 2) {
				users.add(new User(keyValue[0], keyValue[1]));
			}
		}
	}
	
	public User getUser(String username) {
		return users.stream()
				.filter(user -> user.username().equals(username))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("User not found: " + username));
	}
	
	public User getRandomUser() {
		return users.get(randomGenerator.nextInt(users.size()));
	}
	
	public record User(String username, String password) {
	}
	
}
