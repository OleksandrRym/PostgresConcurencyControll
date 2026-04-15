package com.rymar.concurency.anomaly;

import static java.lang.Thread.sleep;

import com.rymar.repository.BaseRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Future;
import lombok.SneakyThrows;

/// To demonstrate Phantom Read in TRANSACTION_READ_COMMITTED.
/// REMEMBER: in TRANSACTION_REPEATABLE_READ, this anomaly does not occur.
public class PhantomRead extends BaseRepository {

  private static final String SELECT_SQL = "SELECT COUNT(*) from users";
  private static final String INSERT_SQL = "INSERT INTO users (name) VALUES ('Alex');";
  private static final String INIT_SQL =
      """
    DROP TABLE IF EXISTS users;

    CREATE TABLE users (
        id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        name TEXT NOT NULL
    );
    TRUNCATE TABLE users;
              """;

  @SneakyThrows
  public static void main(String[] args) {
    setupBaseRepo();
    initSqlTask();

    Connection tx1 = getConnection(Connection.TRANSACTION_REPEATABLE_READ);
    Connection tx2 = getConnection(Connection.TRANSACTION_REPEATABLE_READ);

    executorService.execute(
        () -> {
          runTx1(tx1);
        });
    executorService.execute(
        () -> {
          runTx2(tx2);
        });
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

    sleep(5000);

    ResultSet rs2 = s1.executeQuery();
    printResult(rs2);
    tx1.commit();

    System.out.println("TX1: Commit");
  }

  @SneakyThrows
  private static void runTx2(Connection tx2) {
    sleep(1000);

    System.out.println("TX2: START");

    PreparedStatement statement = tx2.prepareStatement(INSERT_SQL);
    statement.execute();
    tx2.commit();
    System.out.println("TX2: Insert");
    System.out.println("TX2: Commit");
  }

  @SneakyThrows
  private static void printResult(ResultSet resultSet) {
    if (resultSet.next()) {
      int count = resultSet.getInt(1);
      System.out.println("Count rows = " + count);
    }
  }
}
