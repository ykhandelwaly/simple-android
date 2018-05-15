package org.resolvetosavelives.red.newentry.personal

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.RelativeLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotterknife.bindView
import org.resolvetosavelives.red.R
import org.resolvetosavelives.red.TheActivity
import org.resolvetosavelives.red.newentry.address.PatientAddressEntryScreen
import org.resolvetosavelives.red.newentry.search.OngoingPatientEntry
import org.resolvetosavelives.red.newentry.search.PatientRepository
import org.resolvetosavelives.red.router.screen.ScreenRouter
import org.resolvetosavelives.red.widgets.showKeyboard
import javax.inject.Inject

class PatientPersonalDetailsEntryScreen(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

  companion object {
    val KEY = PatientPersonalDetailsScreenKey()
  }

  @Inject
  lateinit var repository: PatientRepository

  @Inject
  lateinit var screenRouter: ScreenRouter

  private val fullNameEditText by bindView<EditText>(R.id.patientpersonaldetails_full_name)
  private val nextButton by bindView<View>(R.id.patientpersonaldetails_next_button)

  override fun onFinishInflate() {
    super.onFinishInflate()
    if (isInEditMode) {
      return
    }
    TheActivity.component.inject(this)

    fullNameEditText.showKeyboard()

    nextButton.setOnClickListener({
      repository.ongoingEntry()
          .map { entry -> entry.copy(personalDetails = OngoingPatientEntry.PersonalDetails(fullNameEditText.text.toString())) }
          .flatMapCompletable { entry: OngoingPatientEntry -> repository.save(entry) }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe({
            screenRouter.push(PatientAddressEntryScreen.KEY)
          })
    })
  }
}
