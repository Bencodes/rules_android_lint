package com.rules.android.lint.worker

import com.squareup.moshi.Json

data class WorkRequest(
  /**
   * Request ID associated with the work request
   */
  @Json(name = "requestId")
  val requestId: Int = 0,
  /**
   * The work request arguments
   */
  @Json(name = "arguments")
  val arguments: List<String>,
)
