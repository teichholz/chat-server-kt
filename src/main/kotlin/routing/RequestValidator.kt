package routing

import arrow.core.raise.either
import io.ktor.server.plugins.requestvalidation.*
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import repository.ReceiverRepository
import repository.transaction

@Single
class RequestValidator : KoinComponent {
    val receiverRepository: ReceiverRepository by inject()

    suspend fun validateSynch(inSynch: InSynch): ValidationResult {
        return transaction {
            either {
                receiverRepository.findByName(inSynch.name)
            }.onRight {
                inSynch.id == it.id && inSynch.name == it.name
            }.fold({ ValidationResult.Invalid("Id does not match with name") }) {
                ValidationResult.Valid
            }
        }
    }

    suspend fun validateNameNotTaken(hasName: HasName): ValidationResult {
        return transaction {
            if (receiverRepository.isNameFree(hasName.name))
                ValidationResult.Valid
            else ValidationResult.Invalid("Name is already taken")
        }
    }
}