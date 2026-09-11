package com.mindspring.app

import com.mindspring.app.data.local.MindSpringDatabase

/** The app under Robolectric: an in-memory database and no scheduled reminders. */
class TestApp : MindSpringApp() {
    override fun createContainer(): AppContainer = AppContainer(this, MindSpringDatabase.inMemory(this))
    override fun startBackgroundWork() = Unit
}
