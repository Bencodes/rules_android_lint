package com.rules.android.lint.worker

import com.squareup.moshi.Json

data class WorkResponse(
  /**
   * The request ID for the work request
   */
  @Json(name = "requestId")
  val requestId: Int,
  /**
   * Exit status for the work request
   */
  @Json(name = "exitCode")
  val exitCode: Int,
  /**
   * Standard output that was collected during the work request
   */
  @Json(name = "output")
  val output: String,
)
