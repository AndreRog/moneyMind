package com.moneymind.persistence;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

final class PostgresTestContainer {

    static final DSLContext DSL_CTX;

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:15.4");
        POSTGRES.start();

        String url = POSTGRES.getJdbcUrl();
        String user = POSTGRES.getUsername();
        String password = POSTGRES.getPassword();

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(url, user, password);
            DSL_CTX = DSL.using(conn, SQLDialect.POSTGRES);
        } catch (ClassNotFoundException | SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private PostgresTestContainer() {}
}
