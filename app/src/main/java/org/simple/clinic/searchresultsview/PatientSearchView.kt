package org.simple.clinic.searchresultsview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.view.detaches
import io.reactivex.Observable
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.patient_search_view.view.*
import org.simple.clinic.R
import org.simple.clinic.di.injector
import org.simple.clinic.mobius.DeferredEventSource
import org.simple.clinic.mobius.MobiusDelegate
import org.simple.clinic.patient.PatientSearchCriteria
import org.simple.clinic.router.screen.ScreenRouter
import org.simple.clinic.util.unsafeLazy
import org.simple.clinic.widgets.ItemAdapter
import org.simple.clinic.widgets.ScreenDestroyed
import org.simple.clinic.widgets.visibleOrGone
import java.util.UUID
import javax.inject.Inject

private typealias RegisterNewPatientClicked = () -> Unit
private typealias SearchResultClicked = (UUID) -> Unit

class PatientSearchView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs), SearchResultsUi {

  @Inject
  lateinit var screenRouter: ScreenRouter

  @Inject
  lateinit var effectHandler: SearchResultsEffectHandler

  var registerNewPatientClicked: RegisterNewPatientClicked? = null

  var searchResultClicked: SearchResultClicked? = null

  private val adapter = ItemAdapter(SearchResultsItemType.DiffCallback())

  private val externalEvents = DeferredEventSource<SearchResultsEvent>()

  private val delegate by unsafeLazy {
    val uiRenderer = SearchResultsUiRenderer(this)

    MobiusDelegate.forView(
        events = Observable.never(),
        defaultModel = SearchResultsModel.create(),
        update = SearchResultsUpdate(),
        effectHandler = effectHandler.build(),
        init = SearchResultsInit(),
        modelUpdateListener = uiRenderer::render,
        additionalEventSources = listOf(externalEvents)
    )
  }

  @SuppressLint("CheckResult")
  override fun onFinishInflate() {
    super.onFinishInflate()
    inflate(context, R.layout.patient_search_view, this)
    if (isInEditMode) {
      return
    }

    context.injector<Injector>().inject(this)

    val screenDestroys = detaches().map { ScreenDestroyed() }
    setupScreen(screenDestroys)
    isSaveEnabled = false
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    delegate.start()
  }

  override fun onDetachedFromWindow() {
    delegate.stop()
    super.onDetachedFromWindow()
  }

  override fun onSaveInstanceState(): Parcelable? {
    return delegate.onSaveInstanceState(super.onSaveInstanceState())
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    super.onRestoreInstanceState(delegate.onRestoreInstanceState(state))
  }

  fun searchWithCriteria(searchCriteria: PatientSearchCriteria) {
    externalEvents.notify(SearchPatientWithCriteria(searchCriteria))
  }

  private fun setupScreen(screenDestroys: Observable<ScreenDestroyed>) {
    resultsRecyclerView.layoutManager = LinearLayoutManager(context)
    resultsRecyclerView.adapter = adapter

    setupNewPatientClicks()
    setupSearchResultClicks(screenDestroys)
  }

  private fun setupNewPatientClicks() {
    newPatientButton.setOnClickListener { registerNewPatientClicked?.invoke() }
  }

  private fun setupSearchResultClicks(screenDestroys: Observable<ScreenDestroyed>) {
    adapter
        .itemEvents
        .ofType<SearchResultsItemType.Event.ResultClicked>()
        .doOnNext { searchResultClicked?.invoke(it.patientUuid) }
        .takeUntil(screenDestroys)
        .subscribe()
  }

  override fun updateSearchResults(
      results: PatientSearchResults
  ) {
    loader.visibleOrGone(isVisible = false)
    newPatientContainer.visibleOrGone(isVisible = true)
    if (results.hasNoResults) {
      setEmptyStateVisible(true)
      adapter.submitList(emptyList())
    } else {
      setEmptyStateVisible(false)
      adapter.submitList(SearchResultsItemType.from(results))
    }
  }

  private fun setEmptyStateVisible(visible: Boolean) {
    emptyStateView.visibleOrGone(visible)

    newPatientRationaleTextView.setText(when {
      visible -> R.string.patientsearchresults_register_patient_rationale_for_empty_state
      else -> R.string.patientsearchresults_register_patient_rationale
    })
    newPatientButton.setText(when {
      visible -> R.string.patientsearchresults_register_patient_for_empty_state
      else -> R.string.patientsearchresults_register_patient
    })
  }

  interface Injector {
    fun inject(target: PatientSearchView)
  }
}
