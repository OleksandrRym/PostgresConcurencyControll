package com.rymar.concurency;

import com.rymar.repository.BaseRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.*;


public class RepetableRead extends BaseRepository {

    public static void main(String[] args) throws Exception {

        Connection tx1 = DriverManager.getConnection(URL, USER, PASS);
        Connection tx2 = DriverManager.getConnection(URL, USER, PASS);

        tx1.setAutoCommit(false);
        tx2.setAutoCommit(false);

        tx1.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        tx2.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        executorService.execute(() -> {
            try {
                System.out.println("\nTX1: START");

                PreparedStatement snap1 = tx1.prepareStatement(
                        "SELECT txid_current_snapshot()"
                );
                var rs1 = snap1.executeQuery();
                if (rs1.next()) {
                    System.out.println("TX1 SNAPSHOT: " + rs1.getString(1));
                }

                System.out.println("TX1: UPDATE hits = hits + 1");

                PreparedStatement ps = tx1.prepareStatement(
                        "UPDATE website SET hits = hits + 1"
                );
                ps.executeUpdate();

                Thread.sleep(3000);

                System.out.println("TX1: COMMIT");
                tx1.commit();

            } catch (Exception e) {
                System.out.println("TX1 ERROR: " + e.getMessage());
            }
        });

        executorService.execute(() -> {
            try {
                System.out.println("\nTX2: START");

                Thread.sleep(3000);

                PreparedStatement snap2 = tx2.prepareStatement(
                        "SELECT txid_current_snapshot()"
                );
                var rs2 = snap2.executeQuery();
                if (rs2.next()) {
                    System.out.println("TX2 SNAPSHOT: " + rs2.getString(1));
                }

                System.out.println("TX2: DELETE");

                PreparedStatement ps = tx2.prepareStatement(
                        "DELETE FROM website WHERE hits = 10"
                );

                ps.executeUpdate();

                System.out.println("TX2: DELETE END");

                tx2.commit();

            } catch (Exception e) {
                System.out.println("TX2 ERROR: " + e.getMessage());
            }
        });

        executorService.execute(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}