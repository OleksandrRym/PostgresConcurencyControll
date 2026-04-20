package com.rymar.concurency.internal;

import com.rymar.repository.BaseRepository;
import java.sql.Connection;
import lombok.SneakyThrows;

public class MVCCDemo extends BaseRepository {

  private static final String SELECT_SQL =
      """
            SELECT lp AS "lp_id", t_xmin AS "xmin", t_xmax AS "xmax", t_ctid AS "ctid"
            FROM heap_page_items(get_raw_page('user_balance', 0))
            WHERE lp_flags = 1;
            """;
  private static final String INIT_SQL =
      """
            DROP TABLE IF EXISTS user_balance;
            CREATE TABLE user_balance (id int PRIMARY KEY, amount int);
            CREATE EXTENSION IF NOT EXISTS pageinspect;
            """;

  @SneakyThrows
  public static void main(String[] args) {
    setupBaseRepo();

    setInitSql(INIT_SQL);

    try (Connection conn = HIKARI_POOL.getConnection()) {
      conn.setAutoCommit(true);

      create(conn);
      update(conn);
      delete(conn);
    }

    executorService.shutdown();
  }

  @SneakyThrows
  private static void create(Connection conn) {
    System.out.println("=== Крок 1: CREATE (INSERT) ===");
    System.out.println("Надсилаємо:");
    System.out.println("INSERT INTO user_balance (id, amount) VALUES (1, 100);\n");
    conn.createStatement().execute("INSERT INTO user_balance (id, amount) VALUES (1, 100);");

    printMvccData(conn);
  }

  @SneakyThrows
  private static void update(Connection conn) {
    System.out.println("\n=== Крок 2: UPDATE (Нова версія) ===");
    System.out.println("Надсилаємо:");
    System.out.println("UPDATE user_balance SET amount = 200 WHERE id = 1;\n");
    conn.createStatement().execute("UPDATE user_balance SET amount = 200 WHERE id = 1;");

    printMvccData(conn);
  }

  @SneakyThrows
  private static void delete(Connection conn) {
    System.out.println("\n=== Крок 3: DELETE (Позначаємо xmax) ===");
    System.out.println("Надсилаємо:");
    System.out.println("DELETE FROM user_balance WHERE id = 1;\n");
    conn.createStatement().execute("DELETE FROM user_balance WHERE id = 1;");

    printMvccData(conn);
  }

  @SneakyThrows
  private static void printMvccData(Connection conn) {
    var rs = conn.createStatement().executeQuery(SELECT_SQL);

    System.out.printf("%-6s | %-10s | %-10s | %-10s%n", "LP", "xmin", "xmax", "CTID");
    System.out.println("-".repeat(40));

    int lp, xmin, xmax = 0;
    String ctid;
    while (rs.next()) {
      lp = rs.getInt("lp_id");
      xmin = rs.getInt("xmin");
      xmax = rs.getInt("xmax");
      ctid = rs.getString("ctid");

      System.out.printf("(0, %d) | %-10d | %-10d | %-10s%n", lp, xmin, xmax, ctid);
    }
    System.out.println("-".repeat(40));
  }
}
