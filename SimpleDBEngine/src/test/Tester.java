package test;
import simpledb.plan.Plan;
import simpledb.query.Scan;
import simpledb.tx.Transaction;
import simpledb.plan.Planner;
import simpledb.server.SimpleDB;

public class Tester {
   public static void main(String[] args) {
      try {
    	 // analogous to the driver
		 SimpleDB db = new SimpleDB("studentdb");
		
		 // analogous to the connection
		 Transaction tx  = db.newTx();
		 Planner planner = db.planner();

         // analogous to the statement
         String qry = "select SName, majorid from STUDENT";
//                    + "set MajorId=10 "
//                    + "where SName > 'art'";
//
         Plan p = planner.createQueryPlan(qry, tx);

         // analogous to the result set
         Scan s = p.open();

         System.out.println("Name\tMajor");
         while (s.next()) {
             String sname = s.getString("sname"); //SimpleDB stores field names
             int dname = s.getInt("majorid"); //in lower case
             System.out.println(sname + "\t" + dname);
         }
		 
//		 String qry = "create index majoridx "
//				 	+ "on STUDENT(MajorID) using hash";
//         int p = planner.executeUpdate(qry, tx);
         
//         if (p > 0) {
//        	 System.out.println("Something changed. Rows affected:" + p);
//         } else {
//        	 System.out.println("Nothing changed. Rows affected:" + p);
//         }
         s.close();
         tx.commit();
         
      }
      catch(Exception e) {
         e.printStackTrace();
      }
   }
}
