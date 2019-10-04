package org.simple.clinic.newentry

import com.f2prateek.rx.preferences2.Preference
import com.spotify.mobius.rx2.RxMobius
import io.reactivex.Completable
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import io.reactivex.rxkotlin.Singles
import org.simple.clinic.analytics.Analytics
import org.simple.clinic.facility.FacilityRepository
import org.simple.clinic.patient.OngoingNewPatientEntry
import org.simple.clinic.patient.PatientEntryValidationError
import org.simple.clinic.patient.PatientEntryValidationError.BOTH_DATEOFBIRTH_AND_AGE_ABSENT
import org.simple.clinic.patient.PatientEntryValidationError.BOTH_DATEOFBIRTH_AND_AGE_PRESENT
import org.simple.clinic.patient.PatientEntryValidationError.COLONY_OR_VILLAGE_EMPTY
import org.simple.clinic.patient.PatientEntryValidationError.DATE_OF_BIRTH_IN_FUTURE
import org.simple.clinic.patient.PatientEntryValidationError.DISTRICT_EMPTY
import org.simple.clinic.patient.PatientEntryValidationError.EMPTY_ADDRESS_DETAILS
import org.simple.clinic.patient.PatientEntryValidationError.FULL_NAME_EMPTY
import org.simple.clinic.patient.PatientEntryValidationError.INVALID_DATE_OF_BIRTH
import org.simple.clinic.patient.PatientEntryValidationError.MISSING_GENDER
import org.simple.clinic.patient.PatientEntryValidationError.PERSONAL_DETAILS_EMPTY
import org.simple.clinic.patient.PatientEntryValidationError.PHONE_NUMBER_LENGTH_TOO_LONG
import org.simple.clinic.patient.PatientEntryValidationError.PHONE_NUMBER_LENGTH_TOO_SHORT
import org.simple.clinic.patient.PatientEntryValidationError.PHONE_NUMBER_NON_NULL_BUT_BLANK
import org.simple.clinic.patient.PatientEntryValidationError.STATE_EMPTY
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.user.UserSession
import org.simple.clinic.util.DistinctValueCallback
import org.simple.clinic.util.scheduler.SchedulersProvider

object PatientEntryEffectHandler {
  fun createEffectHandler(
      userSession: UserSession,
      facilityRepository: FacilityRepository,
      patientRepository: PatientRepository,
      patientRegisteredCount: Preference<Int>,
      ui: PatientEntryUi,
      schedulersProvider: SchedulersProvider
  ): ObservableTransformer<PatientEntryEffect, PatientEntryEvent> {
    val distinctShowDatePatternInLabelCallback = DistinctValueCallback<Boolean>()

    return RxMobius
        .subtypeEffectHandler<PatientEntryEffect, PatientEntryEvent>()
        .addTransformer(FetchPatientEntry::class.java, fetchOngoingEntryEffectHandler(userSession, facilityRepository, patientRepository, schedulersProvider.io()))
        .addConsumer(PrefillFields::class.java, { ui.preFillFields(it.patientEntry) }, schedulersProvider.ui())
        .addAction(ScrollFormToBottom::class.java, { ui.scrollFormToBottom() }, schedulersProvider.ui())
        .addConsumer(ShowEmptyFullNameError::class.java, { ui.showEmptyFullNameError(it.show) }, schedulersProvider.ui())
        .addAction(HidePhoneLengthErrors::class.java, { hidePhoneLengthErrors(ui) }, schedulersProvider.ui())
        .addAction(HideDateOfBirthErrors::class.java, { hideDateOfBirthErrors(ui) }, schedulersProvider.ui())
        .addAction(HideEmptyDateOfBirthAndAgeError::class.java, { ui.showEmptyDateOfBirthAndAgeError(false) }, schedulersProvider.ui())
        .addAction(HideMissingGenderError::class.java, { ui.showMissingGenderError(false) }, schedulersProvider.ui())
        .addAction(HideEmptyColonyOrVillageError::class.java, { ui.showEmptyColonyOrVillageError(false) }, schedulersProvider.ui())
        .addAction(HideEmptyDistrictError::class.java, { ui.showEmptyDistrictError(false) }, schedulersProvider.ui())
        .addAction(HideEmptyStateError::class.java, { ui.showEmptyStateError(false) }, schedulersProvider.ui())
        .addConsumer(ShowDatePatternInDateOfBirthLabel::class.java, {
          distinctShowDatePatternInLabelCallback.pass(it.show, ui::setShowDatePatternInDateOfBirthLabel)
        }, schedulersProvider.ui())
        .addTransformer(SavePatient::class.java,
            savePatientTransformer(patientRepository, patientRegisteredCount, ui, schedulersProvider)
        )
        .addConsumer(ShowValidationErrors::class.java, { showValidationErrors(ui, it.errors) }, schedulersProvider.ui())
        .build()
  }

  private fun fetchOngoingEntryEffectHandler(
      userSession: UserSession,
      facilityRepository: FacilityRepository,
      patientRepository: PatientRepository,
      scheduler: Scheduler
  ): ObservableTransformer<FetchPatientEntry, PatientEntryEvent> {
    return ObservableTransformer { fetchPatientEntries ->
      val getPatientEntryAndFacility = Singles.zip(
          patientRepository.ongoingEntry(),
          facilityRepository.currentFacility(userSession).firstOrError()
      )

      fetchPatientEntries
          .flatMapSingle { getPatientEntryAndFacility }
          .subscribeOn(scheduler)
          .map { (entry, facility) ->
            // TODO(rj): 2019-10-03 Extract as function!
            entry.takeIf { it.address != null }
                ?: entry.copy(address = OngoingNewPatientEntry.Address(colonyOrVillage = "", district = facility.district, state = facility.state))
          }
          .map { OngoingEntryFetched(it) }
    }
  }

  private fun hidePhoneLengthErrors(ui: PatientEntryUi) {
    with(ui) {
      showLengthTooLongPhoneNumberError(false)
      showLengthTooShortPhoneNumberError(false)
    }
  }

  private fun hideDateOfBirthErrors(ui: PatientEntryUi) {
    with(ui) {
      showEmptyDateOfBirthAndAgeError(false)
      showInvalidDateOfBirthError(false)
      showDateOfBirthIsInFutureError(false)
    }
  }

  private fun savePatientTransformer(
      patientRepository: PatientRepository,
      patientRegisteredCount: Preference<Int>,
      ui: PatientEntryUi,
      schedulersProvider: SchedulersProvider
  ): ObservableTransformer<SavePatient, PatientEntryEvent> {
    return ObservableTransformer { savePatientEffects ->
      savePatientEffects
          .map { it.entry }
          .subscribeOn(schedulersProvider.io())
          .flatMapCompletable {
            patientRepository
                .saveOngoingEntry(it)
                .observeOn(schedulersProvider.ui())
                .andThen(Completable.fromAction {
                  patientRegisteredCount.set(patientRegisteredCount.get().plus(1))
                  ui.openMedicalHistoryEntryScreen() // TODO(rj): 2019-10-04 Split this logic and dispatch a patient saved event + threading
                })
          }
          .toObservable()
    }
  }

  private fun showValidationErrors(
      ui: PatientEntryUi,
      errors: List<PatientEntryValidationError>
  ) {
    errors
        .onEach { Analytics.reportInputValidationError(it.analyticsName) }
        .forEach {
          when (it) {
            FULL_NAME_EMPTY -> ui.showEmptyFullNameError(true)
            PHONE_NUMBER_LENGTH_TOO_SHORT -> ui.showLengthTooShortPhoneNumberError(true)
            PHONE_NUMBER_LENGTH_TOO_LONG -> ui.showLengthTooLongPhoneNumberError(true)
            BOTH_DATEOFBIRTH_AND_AGE_ABSENT -> ui.showEmptyDateOfBirthAndAgeError(true)
            INVALID_DATE_OF_BIRTH -> ui.showInvalidDateOfBirthError(true)
            DATE_OF_BIRTH_IN_FUTURE -> ui.showDateOfBirthIsInFutureError(true)
            MISSING_GENDER -> ui.showMissingGenderError(true)
            COLONY_OR_VILLAGE_EMPTY -> ui.showEmptyColonyOrVillageError(true)
            DISTRICT_EMPTY -> ui.showEmptyDistrictError(true)
            STATE_EMPTY -> ui.showEmptyStateError(true)

            EMPTY_ADDRESS_DETAILS,
            PHONE_NUMBER_NON_NULL_BUT_BLANK,
            BOTH_DATEOFBIRTH_AND_AGE_PRESENT,
            PERSONAL_DETAILS_EMPTY -> {
              throw AssertionError("Should never receive this error: $it")
            }
          }
        }

    if (errors.isNotEmpty()) {
      ui.scrollToFirstFieldWithError()
    }
  }
}
