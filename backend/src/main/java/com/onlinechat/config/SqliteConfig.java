package com.onlinechat.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.File;

@Configuration
public class SqliteConfig {

    @Value("${sqlite.database-path}")
    private String databasePath;

    @Bean
    @Qualifier("sqliteDataSource")
    public DataSource sqliteDataSource() {
        File dbFile = new File(databasePath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url("jdbc:sqlite:" + databasePath)
                .build();
    }

    @Bean
    @Qualifier("sqliteJdbcTemplate")
    public JdbcTemplate sqliteJdbcTemplate(@Qualifier("sqliteDataSource") DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        initDatabase(jdbcTemplate);
        return jdbcTemplate;
    }

    private void initDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("PRAGMA journal_mode=WAL");
        jdbcTemplate.execute("PRAGMA foreign_keys=ON");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS chat_message (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                message_id BIGINT NOT NULL UNIQUE,
                from_user_id BIGINT NOT NULL,
                to_user_id BIGINT NOT NULL,
                content TEXT,
                message_type TEXT DEFAULT 'TEXT',
                created_at TEXT NOT NULL,
                synced_at TEXT NOT NULL
            )
        """);

        jdbcTemplate.execute("""
            CREATE VIRTUAL TABLE IF NOT EXISTS chat_message_fts USING fts5(
                content,
                content=chat_message,
                content_rowid='id',
                tokenize='unicode61'
            )
        """);

        jdbcTemplate.execute("""
            CREATE TRIGGER IF NOT EXISTS chat_message_ai AFTER INSERT ON chat_message BEGIN
                INSERT INTO chat_message_fts(rowid, content) VALUES (new.id, new.content);
            END
        """);

        jdbcTemplate.execute("""
            CREATE TRIGGER IF NOT EXISTS chat_message_ad AFTER DELETE ON chat_message BEGIN
                INSERT INTO chat_message_fts(chat_message_fts, rowid, content) VALUES ('delete', old.id, old.content);
            END
        """);

        jdbcTemplate.execute("""
            CREATE TRIGGER IF NOT EXISTS chat_message_au AFTER UPDATE ON chat_message BEGIN
                INSERT INTO chat_message_fts(chat_message_fts, rowid, content) VALUES ('delete', old.id, old.content);
                INSERT INTO chat_message_fts(rowid, content) VALUES (new.id, new.content);
            END
        """);
    }
}
