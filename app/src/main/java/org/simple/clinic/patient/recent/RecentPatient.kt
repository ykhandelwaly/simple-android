package org.simple.clinic.patient.recent

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import io.reactivex.Flowable
import org.simple.clinic.patient.Age
import org.simple.clinic.patient.Gender
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import java.util.UUID

data class RecentPatient(

    val uuid: UUID,

    val fullName: String,

    val gender: Gender,

    val dateOfBirth: LocalDate?,

    @Embedded(prefix = "age_")
    val age: Age?,

    val bpUpdatedAt: Instant,

    @Embedded(prefix = "bp_")
    val lastBp: LastBp?
) {

  @Dao
  interface RoomDao {

    @Query("""
        SELECT P.*,
        BP.systolic bp_systolic, BP.diastolic bp_diastolic, BP.updatedAt bpUpdatedAt,
        MAX(
            IFNULL(P.updatedAt, '0'),
            IFNULL(BP.latestUpdatedAt, '0'),
            IFNULL(PD.latestUpdatedAt, '0'),
            IFNULL(AP.latestUpdatedAt, '0'),
            IFNULL(COMM.latestUpdatedAt, '0'),
            IFNULL(MH.latestUpdatedAt, '0')
        ) latestUpdatedAt
        FROM Patient P
          LEFT JOIN (
            SELECT MAX(updatedAt) latestUpdatedAt, T.*
              FROM BloodPressureMeasurement T
              WHERE facilityUuid = :facilityUuid
              GROUP BY patientUuid
          ) BP ON P.uuid=BP.patientUuid
          LEFT JOIN (
            SELECT MAX(updatedAt) latestUpdatedAt, T.*
              FROM PrescribedDrug T
              WHERE facilityUuid = :facilityUuid
              GROUP BY patientUuid
          ) PD ON P.uuid = PD.patientUuid
          LEFT JOIN (
            SELECT MAX(updatedAt) latestUpdatedAt, T.*
              FROM Appointment T
              WHERE facilityUuid = :facilityUuid
              GROUP BY patientUuid
          ) AP ON P.uuid = AP.patientUuid
          LEFT JOIN (
            SELECT MAX(updatedAt) latestUpdatedAt, T.*
              FROM Communication T
              GROUP BY appointmentUuid
          ) COMM ON AP.uuid = COMM.appointmentUuid
          LEFT JOIN (
            SELECT MAX(updatedAt) latestUpdatedAt, T.*
              FROM MedicalHistory T
              GROUP BY patientUuid
          ) MH ON P.uuid = MH.patientUuid
        WHERE (
          BP.facilityUuid = :facilityUuid OR
          PD.facilityUuid = :facilityUuid OR
          AP.facilityUuid = :facilityUuid
        )
        ORDER BY latestUpdatedAt DESC
        LIMIT :limit
    """)
    fun recentPatients(facilityUuid: UUID, limit: Int): Flowable<List<RecentPatient>>
  }

  data class LastBp(val systolic: String, val diastolic: String)
}
