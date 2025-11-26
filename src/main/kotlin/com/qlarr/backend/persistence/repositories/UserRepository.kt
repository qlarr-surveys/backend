package com.qlarr.backend.persistence.repositories

import com.qlarr.backend.api.user.CountByRoleResponse
import com.qlarr.backend.persistence.entities.UserEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.ListCrudRepository
import java.util.*

interface UserRepository : ListCrudRepository<UserEntity, UUID> {


    fun findAllByDeletedIsFalse(): List<UserEntity>

    fun findByEmailAndDeletedIsFalse(email: String): UserEntity?
    fun findByIdAndDeletedIsFalse(id: UUID): UserEntity?

    /**
     * !!IMPORTANT NOTE!!
     *
     * If you change anything on the {@link com.qlarr.backend.api.user.Roles} enum
     * or on {@link com.qlarr.backend.api.user.CountByRoleResponse} or even at the
     * {@link com.qlarr.backend.persistence.entities.UserEntity} you must update the query!
     * This affects the changes of packages, names, field positions or the enum fields!
     */
    @Query(
            nativeQuery = true,
            value = "select " +
                    "count(*) filter (where cast(roles as varchar) like '%SUPER_ADMIN%') as superAdmin, " +
                    "count(*) filter (where cast(roles as varchar) like '%SURVEY_ADMIN%') as surveyAdmin, " +
                    "count(*) filter (where cast(roles as varchar) like '%SURVEYOR%') as surveyor, " +
                    "count(*) filter (where cast(roles as varchar) like '%ANALYST%') as analyst, " +
                    "count(*) filter (where cast(roles as varchar) like '%SUPERVISOR%') as supervisor" +
                    " from users"
    )
    fun selectRoleCounts(): CountByRoleResponse

}
