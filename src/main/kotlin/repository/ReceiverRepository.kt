package repository

import model.tables.records.ReceiverRecord

/**
 * T: Type of the id
 */
interface ReceiverRepository<T> : Repository<ReceiverRecord, T> {
}