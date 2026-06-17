package com.citruschat.citrusmobile.data.repository

import com.citruschat.citrusmobile.core.logging.Logger
import com.citruschat.citrusmobile.data.local.dao.UserDao
import com.citruschat.citrusmobile.data.mapper.toDomain
import com.citruschat.citrusmobile.data.mapper.toEntity
import com.citruschat.citrusmobile.data.user.UserRemoteDataSource
import com.citruschat.citrusmobile.domain.model.User
import com.citruschat.citrusmobile.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl
    @Inject
    constructor(
        private val dao: UserDao,
        private val userRemoteDataSource: UserRemoteDataSource,
        private val logger: Logger,
    ) : UserRepository {
        override fun observeCurrentUser(): Flow<User?> =
            dao
                .observeCurrentUser()
                .map { entity -> entity?.toDomain() }

        override suspend fun searchUsers(query: String): List<User> {
            if (query.isBlank()) return emptyList()
            logger.d(TAG, "Searching users query=$query")
            val users = userRemoteDataSource.searchUsers(query.trim())
            saveUsers(users)
            return users
        }

        override suspend fun saveCurrentUser(user: User) {
            logger.i(TAG, "Saving current user id=${user.id}")
            dao.clearCurrentUserFlag()
            dao.upsert(user.copy(isCurrentUser = true).toEntity())
        }

        override suspend fun saveUsers(users: List<User>) {
            if (users.isEmpty()) return
            logger.d(TAG, "Saving users count=${users.size}")
            dao.upsertAll(users.map { it.toEntity() })
        }

        override suspend fun clearCurrentUser() {
            logger.i(TAG, "Clearing current user flag")
            dao.clearCurrentUserFlag()
        }
    }

private const val TAG = "UserRepository"
