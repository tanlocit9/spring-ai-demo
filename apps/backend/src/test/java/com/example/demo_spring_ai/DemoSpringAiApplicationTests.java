package com.example.demo_spring_ai;

import org.junit.jupiter.api.Test;

class DemoSpringAiApplicationTests {

	@Test
	void applicationClassIsPresent() {
		DemoSpringAiApplication.main(new String[] { "--spring.main.web-application-type=none", "--spring.autoconfigure.exclude=org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration" });
	}

}
