package simpledb.materialize;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.plan.Planner;
import simpledb.query.*;
import simpledb.server.SimpleDB;

/* This is a version of the StudentMajor program that
 * accesses the SimpleDB classes directly (instead of
 * connecting to it as a JDBC client).
 *
 * These kind of programs are useful for debugging
 * your changes to the SimpleDB source code.
 */

public class AggregateTest {
    public static void main(String[] args) {
        try {
            // analogous to the driver
            SimpleDB db = new SimpleDB("studentdb");

            // analogous to the connection
            Transaction tx  = db.newTx();
            Planner planner = db.planner();

            // analogous to the statement
            String qry = "select count(sid), majorid from student";
            Plan p = planner.createQueryPlan(qry, tx);

            // analogous to the result set
            Scan s = p.open();

            System.out.println("Majorid");
            while (s.next()) {
                String majorid = s.getString("majorid"); //SimpleDB stores field names
                System.out.println(majorid);
            }
            s.close();
            tx.commit();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}
