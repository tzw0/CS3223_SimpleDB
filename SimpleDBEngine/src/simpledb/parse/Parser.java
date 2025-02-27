package simpledb.parse;

import java.util.*;

import simpledb.controller.Setting;
import simpledb.query.*;
import simpledb.record.*;
import simpledb.materialize.*;

/**
 * The SimpleDB parser.
 *
 * @author Edward Sciore
 */
public class Parser {
    private Lexer lex;

    public Parser(String s) {
        lex = new Lexer(s);
    }

// Methods for parsing predicates, terms, expressions, constants, and fields

    public String field() {
        return lex.eatId();
    }

    public String aggregate() {
        return lex.eatAggregate();
    }

    public Constant constant() {
        if (lex.matchStringConstant())
            return new Constant(lex.eatStringConstant());
        else
            return new Constant(lex.eatIntConstant());
    }

    public Expression expression() {
        if (lex.matchId())
            return new Expression(field());
        else
            return new Expression(constant());
    }

    public CondOp condOperator() {
        return new CondOp(lex.eatCondOp());
    }

    public Term term() {
        Expression lhs = expression();
        CondOp condOp = condOperator();
        Expression rhs = expression();

        return new Term(lhs, condOp, rhs);
    }

    public Predicate predicate() {
        Predicate pred = new Predicate(term());
        if (lex.matchKeyword("and")) {
            lex.eatKeyword("and");
            pred.conjoinWith(predicate());
        }
        return pred;
    }

    //set some stuff in simple ij
    public void setting() {
        lex.eatKeyword("setting");
        Setting.getInstance().setJoinMode(lex.eatStringConstant());
    }

// Methods for parsing queries

    public QueryData query() {
        lex.eatKeyword("select");
        boolean isDistinct = distinct();
        List<String> fields = new ArrayList<>();
        List<AggregationFn> aggregates = new ArrayList<>();

        while (true) {
            String field;
            if (lex.matchAggregate()) {
                String aggregate = aggregate();
                lex.eatDelim('(');
                field = field();
                lex.eatDelim(')');
                aggregates.add(getAggregateFn(aggregate, field));
            } else {
                field = field();
            }
            fields.add(field);

            if (!lex.matchDelim(','))
                break;
            lex.eatDelim(',');
        }

        lex.eatKeyword("from");
        Collection<String> tables = tableList();
        Predicate pred = new Predicate();
        if (lex.matchKeyword("where")) {
            lex.eatKeyword("where");
            pred = predicate();
        }
        LinkedHashMap<String, Boolean> orderByFields = new LinkedHashMap<>();
        if (lex.matchKeyword("order")) {
            lex.eatKeyword("order");
            lex.eatKeyword("by");
            orderByFields = orderByList();
        }
        List<String> groupByFields = new ArrayList<>();
        if (lex.matchKeyword("group")) {
            lex.eatKeyword("group");
            lex.eatKeyword("by");
            groupByFields = groupByList();
        }
//        System.out.println(isDistinct);
        return new QueryData(isDistinct, fields, aggregates, tables, pred, orderByFields, groupByFields);
    }

    private boolean distinct() {
        if (lex.matchKeyword("distinct")) {
            lex.eatKeyword("distinct");
            return true;
        }
        return false;
    }

    private List<String> selectList() {
        List<String> list = new ArrayList<>();
        list.add(field());
        if (lex.matchDelim(',')) {
            lex.eatDelim(',');
            list.addAll(selectList());
        }
        return list;
    }

    private AggregationFn getAggregateFn(String aggregate, String field) {
        AggregationFn aggr = null;
        switch (aggregate.toLowerCase()) {
            case "avg": {
                aggr = new AvgFn(field);
                break;
            }
            case "count": {
                aggr = new CountFn(field);
                break;
            }
            case "max": {
                aggr = new MaxFn(field);
                break;
            }
            case "min": {
                aggr = new MinFn(field);
                break;
            }
            case "sum": {
                aggr = new SumFn(field);
                break;
            }
        }

        return aggr;
    }

    private LinkedHashMap<String, Boolean> orderByList() {
        LinkedHashMap<String, Boolean> orderByFields = new LinkedHashMap<>();
        String field = field();
        boolean isAsc = true;
        if (lex.matchKeyword("asc")) {
            lex.eatKeyword("asc");
        } else if (lex.matchKeyword("desc")) {
            lex.eatKeyword("desc");
            isAsc = false;
        }

        orderByFields.put(field, isAsc);
        if (lex.matchDelim(',')) {
            lex.eatDelim(',');
            orderByFields.putAll(orderByList());
        }
        return orderByFields;
    }

    private List<String> groupByList() {
        List<String> groupByFields = new ArrayList<>();
        String field = field();

        groupByFields.add(field);
        if (lex.matchDelim(',')) {
            lex.eatDelim(',');
            groupByFields.addAll(groupByList());
        }
        return groupByFields;
    }

    private Collection<String> tableList() {
        Collection<String> L = new ArrayList<String>();
        L.add(lex.eatId());
        if (lex.matchDelim(',')) {
            lex.eatDelim(',');
            L.addAll(tableList());
        }
        return L;
    }

// Methods for parsing the various update commands

    public Object updateCmd() {
        if (lex.matchKeyword("insert"))
            return insert();
        else if (lex.matchKeyword("delete"))
            return delete();
        else if (lex.matchKeyword("update"))
            return modify();
        else
            return create();
    }

    private Object create() {
        lex.eatKeyword("create");
        if (lex.matchKeyword("table"))
            return createTable();
        else if (lex.matchKeyword("view"))
            return createView();
        else
            return createIndex();
    }

// Method for parsing delete commands

    public DeleteData delete() {
        lex.eatKeyword("delete");
        lex.eatKeyword("from");
        String tblname = lex.eatId();
        Predicate pred = new Predicate();
        if (lex.matchKeyword("where")) {
            lex.eatKeyword("where");
            pred = predicate();
        }
        return new DeleteData(tblname, pred);
    }

// Methods for parsing insert commands

    public InsertData insert() {
        lex.eatKeyword("insert");
        lex.eatKeyword("into");
        String tblname = lex.eatId();
        lex.eatDelim('(');
        List<String> flds = fieldList();
        lex.eatDelim(')');
        lex.eatKeyword("values");
        lex.eatDelim('(');
        List<Constant> vals = constList();
        lex.eatDelim(')');
        return new InsertData(tblname, flds, vals);
    }

    private List<String> fieldList() {
        List<String> L = new ArrayList<String>();
        L.add(field());
        if (lex.matchDelim(',')) {
            lex.eatDelim(',');
            L.addAll(fieldList());
        }
        return L;
    }

    private List<Constant> constList() {
        List<Constant> L = new ArrayList<Constant>();
        L.add(constant());
        if (lex.matchDelim(',')) {
            lex.eatDelim(',');
            L.addAll(constList());
        }
        return L;
    }

// Method for parsing modify commands

    public ModifyData modify() {
        lex.eatKeyword("update");
        String tblname = lex.eatId();
        lex.eatKeyword("set");
        String fldname = field();
        lex.eatDelim('=');
        Expression newval = expression();
        Predicate pred = new Predicate();
        if (lex.matchKeyword("where")) {
            lex.eatKeyword("where");
            pred = predicate();
        }
        return new ModifyData(tblname, fldname, newval, pred);
    }

// Method for parsing create table commands

    public CreateTableData createTable() {
        lex.eatKeyword("table");
        String tblname = lex.eatId();
        lex.eatDelim('(');
        Schema sch = fieldDefs();
        lex.eatDelim(')');
        return new CreateTableData(tblname, sch);
    }

    private Schema fieldDefs() {
        Schema schema = fieldDef();
        if (lex.matchDelim(',')) {
            lex.eatDelim(',');
            Schema schema2 = fieldDefs();
            schema.addAll(schema2);
        }
        return schema;
    }

    private Schema fieldDef() {
        String fldname = field();
        return fieldType(fldname);
    }

    private Schema fieldType(String fldname) {
        Schema schema = new Schema();
        if (lex.matchKeyword("int")) {
            lex.eatKeyword("int");
            schema.addIntField(fldname);
        } else {
            lex.eatKeyword("varchar");
            lex.eatDelim('(');
            int strLen = lex.eatIntConstant();
            lex.eatDelim(')');
            schema.addStringField(fldname, strLen);
        }
        return schema;
    }

// Method for parsing create view commands

    public CreateViewData createView() {
        lex.eatKeyword("view");
        String viewname = lex.eatId();
        lex.eatKeyword("as");
        QueryData qd = query();
        return new CreateViewData(viewname, qd);
    }


//  Method for parsing create index commands

    public CreateIndexData createIndex() {
        lex.eatKeyword("index");
        String idxname = lex.eatId();
        lex.eatKeyword("on");
        String tblname = lex.eatId();
        lex.eatDelim('(');
        String fldname = field();
        lex.eatDelim(')');
        lex.eatKeyword("using");
        int idxtype = lex.eatIndexType();

        return new CreateIndexData(idxname, tblname, fldname, idxtype);
    }
}

