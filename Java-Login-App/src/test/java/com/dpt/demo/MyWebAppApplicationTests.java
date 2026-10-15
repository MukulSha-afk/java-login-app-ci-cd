package com.dpt.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the complete application context against the isolated in-memory H2
 * database (test profile). Verifies that every Spring bean - security filter
 * chain, controllers, services, repository, transaction manager - wires up
 * without contacting any external system.
 */
@SpringBootTest
@ActiveProfiles("test")
class MyWebAppApplicationTests {

	@Test
	void contextLoads() {
	}

}
