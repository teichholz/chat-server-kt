/*
 * This file is generated by jOOQ.
 */
package model;


import model.tables.MessageJ;
import model.tables.ReceiverJ;
import model.tables.records.MessageRecord;
import model.tables.records.ReceiverRecord;

import org.jooq.ForeignKey;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables in
 * chat.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<MessageRecord> MESSAGE_PKEY = Internal.createUniqueKey(MessageJ.MESSAGE, DSL.name("message_pkey"), new TableField[] { MessageJ.MESSAGE.ID }, true);
    public static final UniqueKey<ReceiverRecord> RECEIVER_NAME_KEY = Internal.createUniqueKey(ReceiverJ.RECEIVER, DSL.name("receiver_name_key"), new TableField[] { ReceiverJ.RECEIVER.NAME }, true);
    public static final UniqueKey<ReceiverRecord> RECEIVER_PKEY = Internal.createUniqueKey(ReceiverJ.RECEIVER, DSL.name("receiver_pkey"), new TableField[] { ReceiverJ.RECEIVER.ID }, true);

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<MessageRecord, ReceiverRecord> MESSAGE__MESSAGE_RECEIVER_FKEY = Internal.createForeignKey(MessageJ.MESSAGE, DSL.name("message_receiver_fkey"), new TableField[] { MessageJ.MESSAGE.RECEIVER }, Keys.RECEIVER_PKEY, new TableField[] { ReceiverJ.RECEIVER.ID }, true);
    public static final ForeignKey<MessageRecord, ReceiverRecord> MESSAGE__MESSAGE_SENDER_FKEY = Internal.createForeignKey(MessageJ.MESSAGE, DSL.name("message_sender_fkey"), new TableField[] { MessageJ.MESSAGE.SENDER }, Keys.RECEIVER_PKEY, new TableField[] { ReceiverJ.RECEIVER.ID }, true);
}
