/*
 * This file is generated by jOOQ.
 */
package model;


import java.util.Arrays;
import java.util.List;

import model.tables.MessageJ;
import model.tables.ReceiverJ;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Chat extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>chat</code>
     */
    public static final Chat CHAT = new Chat();

    /**
     * The table <code>chat.message</code>.
     */
    public final MessageJ MESSAGE = MessageJ.MESSAGE;

    /**
     * The table <code>chat.receiver</code>.
     */
    public final ReceiverJ RECEIVER = ReceiverJ.RECEIVER;

    /**
     * No further instances allowed
     */
    private Chat() {
        super("chat", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            MessageJ.MESSAGE,
            ReceiverJ.RECEIVER
        );
    }
}
