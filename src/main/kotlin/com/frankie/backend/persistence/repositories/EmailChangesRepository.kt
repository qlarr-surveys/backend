package com.frankie.backend.persistence.repositories

import com.frankie.backend.persistence.entities.EmailChangesEntity
import org.springframework.data.repository.ListCrudRepository
import java.util.*

interface EmailChangesRepository : ListCrudRepository<EmailChangesEntity, UUID>
