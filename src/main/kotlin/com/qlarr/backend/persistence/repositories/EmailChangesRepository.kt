package com.qlarr.backend.persistence.repositories

import com.qlarr.backend.persistence.entities.EmailChangesEntity
import org.springframework.data.repository.ListCrudRepository
import java.util.*

interface EmailChangesRepository : ListCrudRepository<EmailChangesEntity, UUID>
