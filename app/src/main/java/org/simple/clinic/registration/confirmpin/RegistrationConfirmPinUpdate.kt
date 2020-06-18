package org.simple.clinic.registration.confirmpin

import com.spotify.mobius.Next
import com.spotify.mobius.Update
import org.simple.clinic.mobius.dispatch
import org.simple.clinic.mobius.next

class RegistrationConfirmPinUpdate : Update<RegistrationConfirmPinModel, RegistrationConfirmPinEvent, RegistrationConfirmPinEffect> {

  override fun update(
      model: RegistrationConfirmPinModel,
      event: RegistrationConfirmPinEvent
  ): Next<RegistrationConfirmPinModel, RegistrationConfirmPinEffect> {
    return when (event) {
      is RegistrationConfirmPinTextChanged -> next(model.withEnteredPinConfirmation(event.confirmPin))
      is RegistrationConfirmPinDoneClicked -> dispatch(ValidatePinConfirmation(model.enteredPinConfirmation, model.ongoingRegistrationEntry))
      is PinConfirmationValidated -> validateEnteredPin(model, event)
      is RegistrationEntrySaved -> dispatch(OpenFacilitySelectionScreen)
      is RegistrationResetPinClicked -> dispatch(GoBackToPinEntry(model.ongoingRegistrationEntry.resetPin()))
    }
  }

  private fun validateEnteredPin(
      model: RegistrationConfirmPinModel,
      event: PinConfirmationValidated
  ): Next<RegistrationConfirmPinModel, RegistrationConfirmPinEffect> {
    var updatedModel = model.validatedPinConfirmation(event.result)

    return if (updatedModel.pinConfirmationMatchesEnteredPin) {
      updatedModel = updatedModel.updateRegistrationEntryWithPinConfirmation(updatedModel.enteredPinConfirmation, event.timestamp)

      next(updatedModel, SaveCurrentRegistrationEntry(updatedModel.ongoingRegistrationEntry))
    } else {
      next(updatedModel, ClearPin)
    }
  }
}