package com.rymar.repository;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BaseRepository {

  protected static final String URL = "jdbc:postgresql://localhost:5432/postgres";
  protected static final String USER = "postgres";
  protected static final String PASS = "postgres";

  protected static final ExecutorService executorService = Executors.newFixedThreadPool(5);

  static {
    executorService.execute(
        () -> {
          try (Connection tx1 = DriverManager.getConnection(URL, USER, PASS)) {
            PreparedStatement ps =
                tx1.prepareStatement(
                    """
                    CREATE TABLE IF NOT EXISTS website (id SERIAL PRIMARY KEY,hits INT);
                    TRUNCATE website;
                    INSERT INTO website (hits) VALUES (9);""");
            ps.execute();
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });
  }
    protected static void getStateDB() {
        executorService.execute(() -> {
            try (Connection connection = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement statement = connection.prepareStatement("SELECT id, hits FROM website");
                 ResultSet resultSet = statement.executeQuery()) {

                System.out.println("\n===== WEBSITE TABLE STATE =====");

                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    int hits = resultSet.getInt("hits");
                    System.out.printf("| id: %-3d | hits: %-5d |\n", id, hits);
                }
                System.out.println("================================\n");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
