package ee.tenman.elektrihind.apollo;

import ee.tenman.elektrihind.apollo.UserService.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
	
	private static final String USERNAME1 = "user1";
	private static final String PASSWORD1 = "password1";
	private static final String USERNAME2 = "user2";
	private static final String PASSWORD2 = "password2";
	private static final String USERNAME_PASSWORD_PAIRS = String.format("%s:%s;%s:%s", USERNAME1, PASSWORD1, USERNAME2, PASSWORD2);
	
	@InjectMocks
	UserService userService;
	
	@Mock
	private Random randomGenerator;
	
	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(userService, "usernamePasswordPairs", USERNAME_PASSWORD_PAIRS);
		userService.initializeUsers();
	}
	
	@Test
	void getUser() {
		User user = userService.getUser(USERNAME1);
		
		assertThat(user.username()).isEqualTo(USERNAME1);
		assertThat(user.password()).isEqualTo(PASSWORD1);
	}
	
	@Test
	void getUsers() {
		List<User> users = userService.getUsers();
		
		assertThat(users).hasSize(2)
				.contains(new User(USERNAME1, PASSWORD1))
				.contains(new User(USERNAME2, PASSWORD2));
	}
	
	@ParameterizedTest
	@CsvSource({
			"0, user1, password1",
			"1, user2, password2"
	})
	void getRandomUser(int randomIndex, String expectedUsername, String expectedPassword) {
		when(randomGenerator.nextInt(2)).thenReturn(randomIndex);
		
		User user = userService.getRandomUser();
		
		assertThat(user.username()).isEqualTo(expectedUsername);
		assertThat(user.password()).isEqualTo(expectedPassword);
	}
}
