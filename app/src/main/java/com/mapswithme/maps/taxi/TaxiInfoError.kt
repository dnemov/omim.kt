package com.mapswithme.maps.taxi

class TaxiInfoError(type: Int, errorCode: String) {
    private val mType: TaxiType
    val code: TaxiManager.ErrorCode

    val providerName: String
        get() = mType.providerName

    override fun toString(): String {
        return "TaxiInfoError{" +
                "mType=" + mType +
                ", mCode=" + code +
                '}'
    }

    init {
        mType = TaxiType.values()[type]
        code = TaxiManager.ErrorCode.valueOf(errorCode)
    }
}