package org.simple.clinic.home.patients

import org.simple.clinic.widgets.UiEvent

sealed class PatientsEvent : UiEvent

class PatientsEnterCodeManuallyClicked : PatientsEvent() {
  override val analyticsName = "Patients:Enter Code Manually Clicked"
}

object NewPatientClicked : PatientsEvent() {
  override val analyticsName = "Patients:Search For Patient Clicked"
}
