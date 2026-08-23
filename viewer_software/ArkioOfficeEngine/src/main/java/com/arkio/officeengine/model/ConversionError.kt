package com.arkio.officeengine.model

data class ConversionError(
    val code: ErrorCode,
    val message: String,
    val cause: Throwable? = null
)

enum class ErrorCode {
    FILE_NOT_FOUND,
    UNSUPPORTED_FORMAT,
    CORRUPTED_FILE,
    PASSWORD_PROTECTED,
    OUT_OF_MEMORY,
    CONVERSION_FAILED,
    STORAGE_FULL,
    UNKNOWN
}
