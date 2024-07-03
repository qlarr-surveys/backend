package com.frankie.backend.persistence.repositories

import com.frankie.backend.api.user.CountByRoleResponse
import com.frankie.backend.persistence.entities.UserEntity
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
     * If you change anything on the {@link com.frankie.backend.api.user.Roles} enum
     * or on {@link com.frankie.backend.api.user.CountByRoleResponse} or even at the
     * {@link com.frankie.backend.persistence.entities.UserEntity} you must update the query!
     * This affects the changes of packages, names, field positions or the enum fields!
     */
    @Query(
            nativeQuery = true,
            value = "select " +
                    "count(*) filter (where cast(roles as varchar) like '%SUPER_ADMIN%') as superAdmin, " +
                    "count(*) filter (where cast(roles as varchar) like '%SURVEY_ADMIN%') as surveyAdmin, " +
                    "count(*) filter (where cast(roles as varchar) like '%{SURVEYOR,%' or cast(roles as varchar) " +
                    "like '%SURVEYOR}%' or cast(roles as varchar) like '%,SURVEYOR,%') as surveyor from users"
    )
    fun selectRoleCounts(): CountByRoleResponse

}
