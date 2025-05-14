package cz.martinzajdlik.recappy_book.config;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class SQLiteDialect extends H2Dialect {

    public SQLiteDialect() {
        super();
        registerFunction("concat", new SQLFunctionTemplate(StandardBasicTypes.STRING, "?1 || ?2"));
    }
}
