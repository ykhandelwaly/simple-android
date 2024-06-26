package org.simple.clinic.newentry

import org.simple.clinic.newentry.country.InputFields
import org.simple.clinic.patient.OngoingNewPatientEntry

interface PatientEntryUiActions {
  fun prefillFields(entry: OngoingNewPatientEntry)
  fun scrollToFirstFieldWithError()
  fun scrollFormOnGenderSelection()
  fun setShowDatePatternInDateOfBirthLabel(showPattern: Boolean)
  fun openMedicalHistoryEntryScreen()
  fun showEmptyFullNameError(show: Boolean)
  fun showLengthTooShortPhoneNumberError(show: Boolean, requiredNumberLength: Int)
  fun showMissingGenderError(show: Boolean)
  fun showEmptyColonyOrVillageError(show: Boolean)
  fun showEmptyDistrictError(show: Boolean)
  fun showEmptyStateError(show: Boolean)
  fun showEmptyDateOfBirthAndAgeError(show: Boolean)
  fun showInvalidDateOfBirthError(show: Boolean)
  fun showDateOfBirthIsInFutureError(show: Boolean)
  fun showAgeExceedsMaxLimitError(show: Boolean)
  fun showDOBExceedsMaxLimitError(show: Boolean)
  fun showAgeExceedsMinLimitError(show: Boolean)
  fun showDOBExceedsMinLimitError(show: Boolean)
}
