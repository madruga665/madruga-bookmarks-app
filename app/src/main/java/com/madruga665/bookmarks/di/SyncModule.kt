package com.madruga665.bookmarks.di

import com.madruga665.bookmarks.data.local.BookmarkDao
import com.madruga665.bookmarks.data.local.CollectionDao
import com.madruga665.bookmarks.data.local.PairedDeviceDao
import com.madruga665.bookmarks.data.remote.sync.AndroidSyncServer
import com.madruga665.bookmarks.data.remote.sync.PeerDiscoveryManager
import com.madruga665.bookmarks.data.remote.sync.SyncHttpClient
import com.madruga665.bookmarks.data.repository.SyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideSyncHttpClient(okHttpClient: OkHttpClient): SyncHttpClient {
        return SyncHttpClient(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideAndroidSyncServer(
        pairedDeviceDao: PairedDeviceDao,
        collectionDao: CollectionDao,
        bookmarkDao: BookmarkDao
    ): AndroidSyncServer {
        return AndroidSyncServer(
            pairedDeviceDao = pairedDeviceDao,
            collectionDao = collectionDao,
            bookmarkDao = bookmarkDao,
            ioDispatcher = Dispatchers.IO
        )
    }

    @Provides
    @Singleton
    fun providePeerDiscoveryManager(): PeerDiscoveryManager {
        return PeerDiscoveryManager(Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        pairedDeviceDao: PairedDeviceDao,
        collectionDao: CollectionDao,
        bookmarkDao: BookmarkDao,
        syncHttpClient: SyncHttpClient,
        peerDiscoveryManager: PeerDiscoveryManager,
        androidSyncServer: AndroidSyncServer
    ): SyncRepository {
        return SyncRepository(
            pairedDeviceDao = pairedDeviceDao,
            collectionDao = collectionDao,
            bookmarkDao = bookmarkDao,
            syncHttpClient = syncHttpClient,
            peerDiscoveryManager = peerDiscoveryManager,
            androidSyncServer = androidSyncServer,
            ioDispatcher = Dispatchers.IO
        )
    }
}

