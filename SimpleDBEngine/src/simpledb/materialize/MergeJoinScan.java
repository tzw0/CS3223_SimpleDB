package simpledb.materialize;

import simpledb.query.*;

/**
 * The Scan class for the <i>mergejoin</i> operator.
 *
 * @author Edward Sciore
 */
public class MergeJoinScan implements Scan {
    private Scan s1;
    private SortScan s2;
    private String fldname1, fldname2;
    private Constant joinval = null;
    private CondOp condOp;
//    private Constant joinval = new Constant("null");

    /**
     * Create a mergejoin scan for the two underlying sorted scans.
     *
     * @param s1       the LHS sorted scan
     * @param s2       the RHS sorted scan
     * @param fldname1 the LHS join field
     * @param fldname2 the RHS join field
     */
    public MergeJoinScan(Scan s1, SortScan s2, String fldname1, CondOp condOp, String fldname2) {
        this.s1 = s1;
        this.s2 = s2;

        //print s1 n s2 check if sorted according to fields

//       System.out.println(fldname1 + ": ");
//       while (s1.next())
//           System.out.println(s1.getInt(fldname1));
//       s1.close();
//
//       System.out.println("\n" + fldname2 + ": ");
//       while (s2.next())
//           System.out.println(s2.getInt(fldname2));
//       s2.close();

        //________________________________________________

        this.fldname1 = fldname1;
        this.fldname2 = fldname2;
        this.condOp = condOp;
        beforeFirst();
    }

    /**
     * Close the scan by closing the two underlying scans.
     *
     * @see simpledb.query.Scan#close()
     */
    public void close() {
        s1.close();
        s2.close();
    }

    /**
     * Position the scan before the first record,
     * by positioning each underlying scan before
     * their first records.
     *
     * @see simpledb.query.Scan#beforeFirst()
     */
    public void beforeFirst() {
        s1.beforeFirst();
        s2.beforeFirst();
    }

    /**
     * Move to the next record.  This is where the action is.
     * <p>
     * If the next RHS record has the same join value,
     * then move to it.
     * Otherwise, if the next LHS record has the same join value,
     * then reposition the RHS scan back to the first record
     * having that join value.
     * Otherwise, repeatedly move the scan having the smallest
     * value until a common join value is found.
     * When one of the scans runs out of records, return false.
     *
     * @see simpledb.query.Scan#next()
     */
    public boolean next() {
        boolean hasmore2 = s2.next();

        if (hasmore2 && s2.getVal(fldname2).equals(joinval))
            return true;

        boolean hasmore1 = s1.next();
        if (hasmore1 && condOp.evaluate(s1.getVal(fldname1), joinval)) {
            s2.restorePosition();
            return true;
        }
        while (hasmore1 && hasmore2) {
            Constant v1 = s1.getVal(fldname1);
            Constant v2 = s2.getVal(fldname2);

            if (condOp.evaluate(v1, v2)) {
                s2.savePosition();
                joinval = s2.getVal(fldname2);
                return true;
            }

            if (v1.compareTo(v2) <= 0)
                hasmore1 = s1.next();
            else// if (v1.compareTo(v2) > 0)
                hasmore2 = s2.next();
        }
        return false;
    }

    /**
     * Return the integer value of the specified field.
     * The value is obtained from whichever scan
     * contains the field.
     *
     * @see simpledb.query.Scan#getInt(java.lang.String)
     */
    public int getInt(String fldname) {
        if (s1.hasField(fldname))
            return s1.getInt(fldname);
        else
            return s2.getInt(fldname);
    }

    /**
     * Return the string value of the specified field.
     * The value is obtained from whichever scan
     * contains the field.
     *
     * @see simpledb.query.Scan#getString(java.lang.String)
     */
    public String getString(String fldname) {
        if (s1.hasField(fldname))
            return s1.getString(fldname);
        else
            return s2.getString(fldname);
    }

    /**
     * Return the value of the specified field.
     * The value is obtained from whichever scan
     * contains the field.
     *
     * @see simpledb.query.Scan#getVal(java.lang.String)
     */
    public Constant getVal(String fldname) {
        if (s1.hasField(fldname))
            return s1.getVal(fldname);
        else
            return s2.getVal(fldname);
    }

    /**
     * Return true if the specified field is in
     * either of the underlying scans.
     *
     * @see simpledb.query.Scan#hasField(java.lang.String)
     */
    public boolean hasField(String fldname) {
        return s1.hasField(fldname) || s2.hasField(fldname);
    }
}

