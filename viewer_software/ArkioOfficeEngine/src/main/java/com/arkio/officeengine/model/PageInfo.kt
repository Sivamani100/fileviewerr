package com.arkio.officeengine.model

data class PageInfo(
    val pageNumber: Int,
    val widthPx: Int,
    val heightPx: Int,
    val bitmapPath: String? = null
)
