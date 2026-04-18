package com.rymar.concurency.anomaly;

import static java.lang.Thread.sleep;

import com.rymar.repository.BaseRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Future;
import lombok.SneakyThrows;

///
/// Демонстрація SerializableAnomaly read аномалії
///
/// Бізнес-логіка: y системі підтримки має мати хоча б 1 активнтй NOC-engineer
/// Для коректної логіки слід використовувати рівень TRANSACTION_SERIALIZABLE
///
public class SerializableAnomaly extends BaseRepository {

  private static final String INIT_SQL =
      """
            DROP TABLE IF EXISTS users;

            CREATE TABLE users (
                id INT PRIMARY KEY,
                active BOOLEAN
            );
 
            INSERT INTO users (id, active) VALUES (1, true);
            INSERT INTO users (id, active) VALUES (2, true);
            """;

  private static final String CHECK_ACTIVE =
      "SELECT COUNT(*) AS cnt FROM users WHERE active = true";

  private static final String DEACTIVATE = "UPDATE users SET active = false WHERE id = ?";

  @SneakyThrows
  public static void main(String[] args) {
    setupBaseRepo();
    initSqlTask();

    Connection tx1 = getConnection(Connection.TRANSACTION_SERIALIZABLE);
    Connection tx2 = getConnection(Connection.TRANSACTION_SERIALIZABLE);
    Connection tx3 = getConnection(Connection.TRANSACTION_SERIALIZABLE);

    executorService.execute(() -> runTx1(tx1));
    executorService.execute(() -> runTx2(tx2));
    executorService.execute(() -> runTx3(tx3));
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
  private static int countActive(Connection tx) {
    PreparedStatement ps = tx.prepareStatement(CHECK_ACTIVE);
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      return rs.getInt("cnt");
    }
    return 0;
  }

  @SneakyThrows
  private static void runTx1(Connection tx) {
    System.out.println("TX1: START");

    int cnt = countActive(tx);
    System.out.println("TX1 sees active Engineers = " + cnt);

    if (cnt > 1) {
      PreparedStatement ps = tx.prepareStatement(DEACTIVATE);
      ps.setInt(1, 1);
      ps.execute();
    }
    sleep(1000);
    tx.commit();

    System.out.println("TX1: COMMIT");
  }

  @SneakyThrows
  private static void runTx2(Connection tx) {
    System.out.println("TX2: START");

    int cnt = countActive(tx);
    System.out.println("TX2 sees active Engineers = " + cnt);

    if (cnt > 1) {
      PreparedStatement ps = tx.prepareStatement(DEACTIVATE);
      ps.setInt(1, 2);
      ps.execute();
    }

    sleep(1000);
    tx.commit();

    System.out.println("TX2: COMMIT");
  }

  @SneakyThrows
  private static void runTx3(Connection tx) {
    sleep(2000);

    System.out.println("TX3: FINAL STATE");
    PreparedStatement ps = tx.prepareStatement("SELECT * FROM users");
    ResultSet rs = ps.executeQuery();
    printResult(rs);

    tx.commit();
  }

  @SneakyThrows
  private static void printResult(ResultSet resultSet) {
    while (resultSet.next()) {
      int id = resultSet.getInt("id");
      boolean active = resultSet.getBoolean("active");
      System.out.println("id=" + id + " active=" + active);
    }
  }
}
