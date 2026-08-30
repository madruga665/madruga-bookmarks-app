package com.madruga665.bookmarks.di

import android.content.Context
import androidx.room.Room
import com.madruga665.bookmarks.data.local.AppDatabase
import com.madruga665.bookmarks.data.local.BookmarkDao
import com.madruga665.bookmarks.data.local.CollectionDao
import com.madruga665.bookmarks.data.local.MIGRATION_4_5
import com.madruga665.bookmarks.data.local.PairedDeviceDao
import com.madruga665.bookmarks.data.repository.BookmarkRepository
import com.madruga665.bookmarks.data.repository.CollectionRepository
import com.madruga665.bookmarks.data.repository.SettingsRepository
import com.madruga665.bookmarks.data.repository.ThemeRepository
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bookmarks_db"
        )
            .addMigrations(MIGRATION_4_5)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideCollectionDao(db: AppDatabase): CollectionDao = db.collectionDao()

    @Provides
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun providePairedDeviceDao(db: AppDatabase): PairedDeviceDao = db.pairedDeviceDao()

    @Provides
    @Singleton
    fun provideCollectionRepository(
        collectionDao: CollectionDao,
        bookmarkDao: BookmarkDao
    ): CollectionRepository {
        return CollectionRepository(collectionDao, bookmarkDao)
    }

    @Provides
    @Singleton
    fun provideBookmarkRepository(bookmarkDao: BookmarkDao): BookmarkRepository {
        return BookmarkRepository(bookmarkDao)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }

    @Provides
    @Singleton
    fun provideThemeRepository(settingsRepository: SettingsRepository): ThemeRepository {
        return ThemeRepository(settingsRepository)
    }
}
