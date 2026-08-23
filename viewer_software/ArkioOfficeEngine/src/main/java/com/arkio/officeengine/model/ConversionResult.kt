package com.arkio.officeengine.model

data class ConversionResult(
    val success: Boolean,
    val outputPdfPath: String? = null,
    val pageCount: Int = 0,
    val error: ConversionError? = null,
    val conversionTimeMs: Long = 0,
    val originalFormat: String = ""
)
