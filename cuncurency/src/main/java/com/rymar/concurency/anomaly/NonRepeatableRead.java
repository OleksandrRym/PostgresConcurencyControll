package com.rymar.concurency.anomaly;

import static java.lang.Thread.sleep;

import com.rymar.repository.BaseRepository;
import java.sql.*;
import java.util.concurrent.Future;
import lombok.SneakyThrows;

///
/// Демонстрація NonRepeatable read аномалії
/// "TX 1 при однакових SELECT має різні значення"
///
public class NonRepeatableRead extends BaseRepository {

  private static final String SELECT_SQL = "SELECT * from examples";
  private static final String INSERT_SQL = "UPDATE examples SET x = 10 WHERE x = 5";
  private static final String INIT_SQL = """
                  CREATE TABLE IF NOT EXISTS examples (
                      id SERIAL PRIMARY KEY,
                      x INT
                  );
                  TRUNCATE examples;
                  INSERT INTO examples (x) VALUES (5);
              """;


  @SneakyThrows
  public static void main(String[] args) {
    setupBaseRepo();
    initSqlTask();

    Connection tx1 = getConnection(Connection.TRANSACTION_READ_COMMITTED);
    Connection tx2 = getConnection(Connection.TRANSACTION_READ_COMMITTED);

    executorService.execute(() -> {runTx1(tx1);});
    executorService.execute(() -> {runTx2(tx2);});
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
  private static void runTx1(Connection tx1) {
    System.out.println("TX1: START");

    PreparedStatement s1 = tx1.prepareStatement(SELECT_SQL);
    ResultSet rs1 = s1.executeQuery();
    printResult(rs1);
    printSnapshot(tx1);
    sleep(5000);

    ResultSet rs2 = s1.executeQuery();
    printResult(rs2);
    printSnapshot(tx1);
    tx1.commit();

    System.out.println("TX1: Commit");
  }

  @SneakyThrows
  private static void runTx2(Connection tx) {
    sleep(1000);

    System.out.println("TX2: START");

    PreparedStatement statement = tx.prepareStatement(INSERT_SQL);
    statement.execute();
    tx.commit();

    System.out.println("TX2: Commit");
  }

  @SneakyThrows
  private static void printSnapshot(Connection tx) {
    Statement stmt = tx.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT txid_current_snapshot();");
    if (rs.next()) {
      String snapshot = rs.getString(1);
      System.out.println("Snapshot: " + snapshot);
    }
  }

  @SneakyThrows
  private static void printResult(ResultSet resultSet) {
    while (resultSet.next()) {
      int id = resultSet.getInt("id");
      int x = resultSet.getInt("x");
      System.out.print("id = " + id + ", x = " + x +"; ");
    }
  }
}
