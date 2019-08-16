package org.simple.clinic.di

import org.simple.clinic.crash.CrashReporterModule
import org.simple.clinic.crash.NoOpCrashReporter
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.user.UserSession

class TestCrashReporterModule : CrashReporterModule() {
  override fun crashReporter(
      userSession: UserSession,
      facilityRepository: FacilityRepository
  ) = NoOpCrashReporter()
}
