package com.rymar.concurency;

import com.rymar.repository.BaseRepository;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/// DELETE is not working (in this scenario)
/// When a transaction starts, it works with a snapshot of the data (snapshot makes by statement).
/// TX2 takes a snapshot where hits = 9,
/// so it does not see the updated value (hits = 10) from TX1,
/// and therefore DELETE affects 0 rows.

public class ReadCommited extends BaseRepository {
  public static void main(String[] args) throws Exception {
    Connection tx1 = DriverManager.getConnection(URL, USER, PASS);
    Connection tx2 = DriverManager.getConnection(URL, USER, PASS);
    tx1.setAutoCommit(false);
    tx2.setAutoCommit(false);

    executorService.execute(
        () -> {
          try {
            System.out.println("\nTX1: UPDATE hits = hits + 1");
            PreparedStatement ps = tx1.prepareStatement("UPDATE website SET hits = hits + 1");
            ps.executeUpdate();
            Thread.sleep(3000); // wait T2.finish
            System.out.println("TX1: COMMIT");
            tx1.commit();
          } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
          }
        });

    executorService.execute(
        () -> {
          try {
            System.out.println("TX2: DELETE");
            Thread.sleep(1000); // Wait T1.start
            PreparedStatement ps = tx2.prepareStatement("DELETE FROM website WHERE hits = 10");
            ps.executeUpdate();
            System.out.println("TX2: DELETE END");
            tx2.commit();
          } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
          }
        });

    executorService.execute(
        () -> {
          try {
            Thread.sleep(5_000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        });
  }
}
