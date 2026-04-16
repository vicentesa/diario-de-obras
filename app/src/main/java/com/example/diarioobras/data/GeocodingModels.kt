package com.example.diarioobras.data

data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList(),
    val status: String = ""
)

data class GeocodingResult(
    val address_components: List<AddressComponent> = emptyList()
)

data class AddressComponent(
    val long_name: String = "",
    val short_name: String = "",
    val types: List<String> = emptyList()
)