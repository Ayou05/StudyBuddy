package com.studybuddy.v2.di

import android.content.Context
import com.studybuddy.v2.data.pb.PbClient
import com.studybuddy.v2.data.repo.PreferencesStore
import com.studybuddy.v2.data.room.PetDao
import com.studybuddy.v2.data.room.SessionDao
import com.studybuddy.v2.data.room.V2Database
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePbClient(): PbClient = PbClient()

    @Provides
    @Singleton
    fun providePreferencesStore(@ApplicationContext context: Context): PreferencesStore =
        PreferencesStore(context)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): V2Database =
        V2Database.build(context)

    @Provides
    fun provideSessionDao(db: V2Database): SessionDao = db.sessionDao()

    @Provides
    fun providePetDao(db: V2Database): PetDao = db.petDao()
}
