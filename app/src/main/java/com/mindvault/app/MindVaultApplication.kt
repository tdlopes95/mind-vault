package com.mindvault.app

import android.app.Application
import com.mindvault.app.data.repository.NoteRepositoryInterface
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MindVaultApplication : Application() {

    @Inject
    lateinit var noteRepository: NoteRepositoryInterface

    override fun onCreate() {
        super.onCreate()
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            noteRepository.purgeOldDeletedNotes(cutoff)
        }
    }
}
