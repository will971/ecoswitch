package com.example.springbootapp.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.springbootapp.model.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserServiceTest {

	@Autowired
	private UserService userService;

	@Test
	void shouldRegisterAndLoginUser() {
		String email = "new_user@saas.com";
		String name = "Test User";
		String password = "securepassword";

		AppUser registered = userService.register(email, name, password);
		assertNotNull(registered);
		assertEquals(email, registered.getEmail());

		AppUser loggedIn = userService.login(email, password);
		assertNotNull(loggedIn);
		assertEquals(name, loggedIn.getName());
	}

	@Test
	void shouldThrowExceptionWhenRegisteringDuplicateEmail() {
		String email = "dup@saas.com";
		userService.register(email, "Dup One", "pass");

		assertThrows(IllegalArgumentException.class, () -> userService.register(email, "Dup Two", "pass"));
	}

	@Test
	void shouldThrowExceptionWhenPasswordIsIncorrect() {
		String email = "wrong_pass@saas.com";
		userService.register(email, "User", "correct_password");

		assertThrows(IllegalArgumentException.class, () -> userService.login(email, "incorrect_password"));
	}

	@Test
	void shouldUpsertGoogleUser() {
		String email = "google@saas.com";
		String name = "Google User";

		// Upsert first time
		AppUser user1 = userService.upsertGoogleUser(email, name);
		assertNotNull(user1);
		assertEquals(email, user1.getEmail());

		// Upsert second time should return the same user
		AppUser user2 = userService.upsertGoogleUser(email, name);
		assertEquals(user1.getId(), user2.getId());
	}
}
