package com.ieltspath.content.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class WritingChartKeyRenameMigrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static JdbcTemplate sql;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("90").load().migrate();
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        sql = new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    @Test
    void renamesEveryStoredWritingChartPathAndPreservesTheRestOfEachUrl() {
        assertThat(sql.queryForObject("SELECT count(*) FROM content_assets WHERE to_jsonb(content_assets)::text LIKE '%writtings%'", Integer.class))
                .isZero();
        assertThat(sql.queryForList("SELECT id::text || ':' || media_reference FROM content_assets WHERE id IN ("
                        + "'1c7fbf03-1337-4754-a507-d1e89abe65db', '0daa5bcc-248c-5461-6db0-8dedb5468724', "
                        + "'d03370cd-2688-2a76-db3e-f18f6f5cd5f6', 'fcee2dbb-09bb-27ce-a23a-649525f7766d', "
                        + "'4dff3a73-31aa-fc4c-be21-d0988f7a041e') ORDER BY id", String.class))
                .containsExactly(
                        "0daa5bcc-248c-5461-6db0-8dedb5468724:https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writing/lessons/PS-W1-WR-2/ps-w1-wr-2-family-spending.png",
                        "1c7fbf03-1337-4754-a507-d1e89abe65db:https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writing/lessons/PS-W1-WR-1/ps-w1-wr-1-car-ownership.png",
                        "4dff3a73-31aa-fc4c-be21-d0988f7a041e:https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writing/lessons/PS-W1-WR-3/ps-w1-wr-3-marriage-age.png",
                        "d03370cd-2688-2a76-db3e-f18f6f5cd5f6:https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writing/lessons/PS-W5-WR-1/ps-w5-wr-1-park-visitors.png",
                        "fcee2dbb-09bb-27ce-a23a-649525f7766d:https://kndrsdqzbzsaiuuruqno.supabase.co/storage/v1/object/public/media/writing/lessons/PS-W5-WR-2/ps-w5-wr-2-fuel-prices.png");
    }
}
