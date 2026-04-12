package com.automatization.comunications;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires a reachable MySQL; skipped in CI until we add Mockito-based unit tests")
class ComunicationsApplicationTests {

	@Test
	void contextLoads() {
	}

}
