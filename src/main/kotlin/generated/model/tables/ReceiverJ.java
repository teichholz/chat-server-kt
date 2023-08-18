/*
 * This file is generated by jOOQ.
 */
package model.tables;


import java.util.function.Function;

import model.Chat;
import model.Keys;
import model.tables.records.ReceiverRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function2;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ReceiverJ extends TableImpl<ReceiverRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>chat.receiver</code>
     */
    public static final ReceiverJ RECEIVER = new ReceiverJ();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ReceiverRecord> getRecordType() {
        return ReceiverRecord.class;
    }

    /**
     * The column <code>chat.receiver.id</code>.
     */
    public final TableField<ReceiverRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>chat.receiver.name</code>.
     */
    public final TableField<ReceiverRecord, String> NAME = createField(DSL.name("name"), SQLDataType.CLOB, this, "");

    private ReceiverJ(Name alias, Table<ReceiverRecord> aliased) {
        this(alias, aliased, null);
    }

    private ReceiverJ(Name alias, Table<ReceiverRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>chat.receiver</code> table reference
     */
    public ReceiverJ(String alias) {
        this(DSL.name(alias), RECEIVER);
    }

    /**
     * Create an aliased <code>chat.receiver</code> table reference
     */
    public ReceiverJ(Name alias) {
        this(alias, RECEIVER);
    }

    /**
     * Create a <code>chat.receiver</code> table reference
     */
    public ReceiverJ() {
        this(DSL.name("receiver"), null);
    }

    public <O extends Record> ReceiverJ(Table<O> child, ForeignKey<O, ReceiverRecord> key) {
        super(child, key, RECEIVER);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Chat.CHAT;
    }

    @Override
    public Identity<ReceiverRecord, Integer> getIdentity() {
        return (Identity<ReceiverRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ReceiverRecord> getPrimaryKey() {
        return Keys.RECEIVER_PKEY;
    }

    @Override
    public ReceiverJ as(String alias) {
        return new ReceiverJ(DSL.name(alias), this);
    }

    @Override
    public ReceiverJ as(Name alias) {
        return new ReceiverJ(alias, this);
    }

    @Override
    public ReceiverJ as(Table<?> alias) {
        return new ReceiverJ(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public ReceiverJ rename(String name) {
        return new ReceiverJ(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ReceiverJ rename(Name name) {
        return new ReceiverJ(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public ReceiverJ rename(Table<?> name) {
        return new ReceiverJ(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Integer, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super Integer, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super Integer, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
