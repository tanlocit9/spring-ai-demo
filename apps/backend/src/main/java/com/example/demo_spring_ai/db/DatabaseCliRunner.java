package com.example.demo_spring_ai.db;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCliRunner implements ApplicationRunner {

	private final DataSource dataSource;
	private final boolean resetEnabled;

	public DatabaseCliRunner(DataSource dataSource, @Value("${demo.database.reset-enabled:false}") boolean resetEnabled) {
		this.dataSource = dataSource;
		this.resetEnabled = resetEnabled;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		var sourceArgs = Arrays.asList(args.getSourceArgs());
		if (sourceArgs.size() != 2 || !"db".equals(sourceArgs.get(0))) {
			return;
		}

		switch (sourceArgs.get(1)) {
			case "init" -> runScript("db/schema.sql");
			case "seed" -> runScript("db/seed.sql");
			case "reset" -> reset();
			default -> throw new IllegalArgumentException("Unknown database command: " + sourceArgs.get(1));
		}
	}

	private void reset() throws Exception {
		if (!resetEnabled) {
			throw new IllegalStateException("db reset is only available when demo.database.reset-enabled=true, such as with the demo profile.");
		}
		executeSql("DROP TABLE IF EXISTS pto_records CASCADE; DROP TABLE IF EXISTS reports CASCADE; DROP TABLE IF EXISTS employees CASCADE; DROP TYPE IF EXISTS pto_status; DROP TYPE IF EXISTS pto_type; DROP TYPE IF EXISTS report_type;");
		runScript("db/schema.sql");
		runScript("db/seed.sql");
	}

	private void runScript(String path) throws Exception {
		try (var connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(connection, new ClassPathResource(path));
		}
	}

	private void executeSql(String sql) throws Exception {
		try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
			for (String command : sql.split(";")) {
				if (!command.isBlank()) {
					statement.execute(command.getBytes(StandardCharsets.UTF_8).length > 0 ? command : "");
				}
			}
		}
	}
}
