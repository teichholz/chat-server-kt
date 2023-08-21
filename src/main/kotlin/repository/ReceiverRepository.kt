package repository

import model.tables.records.ReceiverRecord

/**
 * T: Type of the id
 */
interface ReceiverRepository : Repository<ReceiverRecord, Int> {
}