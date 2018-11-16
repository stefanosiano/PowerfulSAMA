package com.stefanosiano.powerfulsama

/**
 * Class containing action, error and data sent from the ViewModel to its observers.
 */


//a generic class that describes a data with a error
class VmResponse<A, E, out D>
/** Creates an instance containing action, error and data sent from the ViewModel to its observers.  */
(
        /** Specifies what the response is about  */
        val action: A,
        /** Specifies the error enum occurred during an action  */
        val error: E?,
        /** Whether the action was successful  */
        val isSuccessful: Boolean,
        /** Optional data provided by the action  */
        val data: D?) where A : VmResponse.VmAction, E : VmResponse.VmError {

    override fun toString(): String {
        return "VmResponse{" +
                "action=" + action +
                ", error=" + error +
                ", isSuccessful=" + isSuccessful +
                ", data=" + data +
                '}'
    }


    /** Interface that indicates the action of the VmResponse sent by the ViewModel */
    interface VmAction

    /** Interface that indicates the error of the VmResponse sent by the ViewModel */
    interface VmError
}

