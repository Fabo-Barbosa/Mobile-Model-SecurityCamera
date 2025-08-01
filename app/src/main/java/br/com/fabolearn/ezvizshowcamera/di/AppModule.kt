package br.com.fabolearn.ezvizshowcamera.di

import android.content.Context
import br.com.fabolearn.ezvizshowcamera.data.repository.CameraRepository
import br.com.fabolearn.ezvizshowcamera.data.repository.WifiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideWifiRepository(@ApplicationContext context: Context): WifiRepository {
        return WifiRepository(context)
    }

    @Singleton
    @Provides
    fun provideCameraRepository() : CameraRepository {
        return CameraRepository()
    }
}