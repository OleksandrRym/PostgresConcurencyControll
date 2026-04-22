package com.rymar.concurency.internal;


import com.rymar.repository.BaseRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.Future;
import lombok.SneakyThrows;

public class HOTUpdate extends BaseRepository {

    private static final String INIT_SQL = """
            DROP TABLE IF EXISTS balance;

            CREATE TABLE balance (
                id int PRIMARY KEY,
                amount int
            ) WITH (fillfactor = 70);

            CREATE EXTENSION IF NOT EXISTS pageinspect;

            INSERT INTO balance (id, amount) VALUES (5, 200);
            UPDATE balance SET amount = 500 WHERE id = 5;
            UPDATE balance SET amount = 700 WHERE id = 5;
            """;

  private static final String SELECT_SQL =
      """
            SELECT
                lp AS "lp_id",
                lp_flags AS "flag",
                t_xmin AS "xmin",
                t_xmax AS "xmax",
                (t_infomask2 & 16384) > 0 AS "is_hot_updated",
                (t_infomask2 & 32768) > 0 AS "is_only_tuple",
                t_ctid AS "next_ctid",
                'id: ' || get_byte(t_data, 0) || ', am: ' ||
                (get_byte(t_data, 4) + (get_byte(t_data, 5) << 8)) as "raw_data"
            FROM heap_page_items(get_raw_page('balance', 0))
            WHERE lp_flags != 0;
            """;

    @SneakyThrows
    public static void main(String[] args) {
        setupBaseRepo();
        initSqlTask();

        Connection tx1 = getConnection(Connection.TRANSACTION_READ_COMMITTED);

        executorService.execute(() -> runTx1(tx1));
        executorService.shutdown();
    }

    @SneakyThrows
    private static void initSqlTask() {
        Future<?> initTask = executorService.submit(() -> setInitSql(INIT_SQL));
        initTask.get();
    }

    @SneakyThrows
    private static Connection getConnection(int lvl) {
        Connection tx = HIKARI_POOL.getConnection();
        tx.setAutoCommit(false);
        tx.setTransactionIsolation(lvl);
        return tx;
    }

    @SneakyThrows
    private static void runTx1(Connection tx) {
    System.out.println("=== Частина 1: Стан сторінки з HOT-ланцюжком (ДО VACUUM) ===");
        var ps = tx.prepareStatement(SELECT_SQL);
    printResult(ps.executeQuery());

        tx.commit();

    System.out.println("--- Виконуємо VACUUM balance... ---");
    System.out.println();
    try (Connection vacuumConn = HIKARI_POOL.getConnection()) {
      vacuumConn.setAutoCommit(true);
      vacuumConn.createStatement().execute("VACUUM balance;");
    }

    System.out.println("=== Частина 2: Стан сторінки після PRUNING (ПІСЛЯ VACUUM) ===");
    var ps2 = tx.prepareStatement(SELECT_SQL);
    printResult(ps2.executeQuery());

    tx.commit();
    }

    @SneakyThrows
    private static void printResult(ResultSet resultSet) {
    System.out.printf(
        "%-10s | %-6s | %-8s | %-8s | %-8s | %-10s | %-11s | %-15s%n",
        "CTID(lp)", "Flag", "xmin", "xmax", "HOT_upd", "Only_tuple", "Next_ctid", "Data Content");
    System.out.println("-".repeat(105));

        while (resultSet.next()) {
      int lp = resultSet.getInt("lp_id");
      int flag = resultSet.getInt("flag");
            long xmin = resultSet.getLong("xmin");
            long xmax = resultSet.getLong("xmax");
            boolean hotUpdated = resultSet.getBoolean("is_hot_updated");
            boolean onlyTuple = resultSet.getBoolean("is_only_tuple");
            String nextCtid = resultSet.getString("next_ctid");
            String data = resultSet.getString("raw_data");

      String flagName =
          switch (flag) {
            case 1 -> "NORMAL";
            case 2 -> "REDIR";
            case 3 -> "DEAD";
            default -> "UNUSED";
          };

      System.out.printf(
          "%-10s | %-6s | %-8d | %-8d | %-8s | %-10s | %-11s | %-15s%n",
          "(0, " + lp + ")",
          flagName,
          xmin,
          xmax,
          hotUpdated ? "true" : " ",
          onlyTuple ? "true" : " ",
          nextCtid,
          (flag == 2) ? "[REDIRECTS TO " + nextCtid + "]" : (data != null ? data : "N/A"));
        }
    System.out.println("-".repeat(105) + "\n");
    }
}