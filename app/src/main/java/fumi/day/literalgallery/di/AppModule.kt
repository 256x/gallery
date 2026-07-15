package fumi.day.literalgallery.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import fumi.day.literalgallery.data.exif.ExifReader
import fumi.day.literalgallery.data.media.MediaSource
import fumi.day.literalgallery.data.media.MediaStoreSource
import fumi.day.literalgallery.data.repository.MediaRepository
import fumi.day.literalgallery.data.repository.MediaRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @IntoSet
    abstract fun bindMediaStoreSource(impl: MediaStoreSource): MediaSource

    @Binds
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    companion object {
        @Provides
        @Singleton
        fun provideExifReader(@ApplicationContext context: Context): ExifReader =
            ExifReader(context)
    }
}
