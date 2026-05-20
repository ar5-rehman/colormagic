package com.colormagic.kids.data.di

import javax.inject.Qualifier

// Marks a process-lifetime CoroutineScope — used for work that must outlive
// any single screen / ViewModel (e.g. the auth-state StateFlow that the
// whole app observes).
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
