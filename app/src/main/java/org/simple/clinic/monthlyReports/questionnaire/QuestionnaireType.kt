package org.simple.clinic.monthlyReports.questionnaire

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.room.TypeConverter
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.parcelize.Parcelize
import org.simple.clinic.util.room.SafeEnumTypeAdapter

sealed class QuestionnaireType : Parcelable {

  object TypeAdapter : SafeEnumTypeAdapter<QuestionnaireType>(
      knownMappings = mapOf(
          MonthlyScreeningReports to "monthly_screening_reports",
      ),
      unknownStringToEnumConverter = ::Unknown,
      unknownEnumToStringConverter = { (it as Unknown).actualValue }
  )

  class RoomTypeConverter {

    @TypeConverter
    fun toEnum(value: String?): QuestionnaireType? = TypeAdapter.toEnum(value)

    @TypeConverter
    fun fromEnum(questionnaireType: QuestionnaireType?): String? = TypeAdapter.fromEnum(questionnaireType)
  }

  class MoshiTypeAdapter {

    @FromJson
    fun fromJson(value: String?): QuestionnaireType? = TypeAdapter.toEnum(value)

    @ToJson
    fun toJson(questionnaireType: QuestionnaireType?): String? = TypeAdapter.fromEnum(questionnaireType)
  }

  companion object {
    @VisibleForTesting
    fun random(): QuestionnaireType = TypeAdapter.knownMappings.keys.shuffled().first()
  }
}

@Parcelize
@VisibleForTesting
object MonthlyScreeningReports : QuestionnaireType()

@Parcelize
data class Unknown(val actualValue: String) : QuestionnaireType()
