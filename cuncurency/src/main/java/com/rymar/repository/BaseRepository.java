package com.rymar.repository;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BaseRepository {

  protected static final ExecutorService executorService = Executors.newFixedThreadPool(5);
  protected static final String URL = "jdbc:postgresql://localhost:5432/postgres";
  protected static final String USER = "postgres";
  protected static final String PASS = "postgres";

  private static String INIT_SQL = """
                    CREATE TABLE IF NOT EXISTS website (id SERIAL PRIMARY KEY,hits INT);
                    TRUNCATE website;
                    INSERT INTO website (hits) VALUES (9);""";

  public static void setInitSql(String initSql) {
    INIT_SQL = initSql;
    executorService.execute(
        () -> {
          try (Connection tx1 = DriverManager.getConnection(URL, USER, PASS)) {
            PreparedStatement ps = tx1.prepareStatement(INIT_SQL);
            ps.execute();
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });
  }
}
