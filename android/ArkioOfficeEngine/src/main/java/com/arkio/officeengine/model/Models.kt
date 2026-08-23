package com.arkio.officeengine.model

data class ConversionResult(
    val success: Boolean,
    val outputPdfPath: String? = null,
    val pageCount: Int = 0,
    val error: ConversionError? = null,
    val originalFormat: String = ""
)

data class ConversionError(
    val code: ErrorCode,
    val message: String
)

enum class ErrorCode {
    FILE_NOT_FOUND,
    UNSUPPORTED_FORMAT,
    CONVERSION_FAILED,
    UNKNOWN
}
