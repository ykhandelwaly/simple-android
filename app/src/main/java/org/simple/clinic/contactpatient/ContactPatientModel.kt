package org.simple.clinic.contactpatient

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.simple.clinic.home.overdue.OverdueAppointment
import org.simple.clinic.overdue.AppointmentConfig
import org.simple.clinic.overdue.PotentialAppointmentDate
import org.simple.clinic.patient.PatientProfile
import org.simple.clinic.phone.PhoneNumberMaskerConfig
import org.simple.clinic.util.Optional
import org.simple.clinic.util.ParcelableOptional
import org.simple.clinic.util.UserClock
import org.simple.clinic.util.parcelable
import org.threeten.bp.LocalDate
import java.util.UUID

@Parcelize
data class ContactPatientModel(
    val patientUuid: UUID,
    val patientProfile: PatientProfile? = null,
    val appointment: ParcelableOptional<OverdueAppointment>? = null,
    val secureCallingFeatureEnabled: Boolean,
    val potentialAppointments: List<PotentialAppointmentDate>,
    val selectedAppointmentDate: LocalDate
) : Parcelable {

  companion object {
    fun create(
        patientUuid: UUID,
        phoneNumberMaskerConfig: PhoneNumberMaskerConfig,
        appointmentConfig: AppointmentConfig,
        userClock: UserClock
    ): ContactPatientModel {
      val secureCallingFeatureEnabled = with(phoneNumberMaskerConfig) {
        phoneMaskingFeatureEnabled && proxyPhoneNumber.isNotBlank()
      }

      val potentialAppointments = PotentialAppointmentDate.from(appointmentConfig.remindAppointmentsIn, userClock)

      return ContactPatientModel(
          patientUuid = patientUuid,
          secureCallingFeatureEnabled = secureCallingFeatureEnabled,
          potentialAppointments = potentialAppointments,
          selectedAppointmentDate = potentialAppointments.first().scheduledFor
      )
    }
  }

  val hasLoadedPatientProfile: Boolean
    get() = patientProfile != null

  val hasLoadedAppointment: Boolean
    get() = appointment != null

  fun patientProfileLoaded(patientProfile: PatientProfile): ContactPatientModel {
    return copy(patientProfile = patientProfile)
  }

  fun overdueAppointmentLoaded(appointment: Optional<OverdueAppointment>): ContactPatientModel {
    return copy(appointment = appointment.parcelable())
  }
}