package com.rules.android.lint.worker

import okio.Buffer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Test


import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4::class)
class WorkerJsonMessageProcessorTest {

  private val jsonBuffer = Buffer()
  private val processor = WorkerJsonMessageProcessor(
    inputStream = jsonBuffer.inputStream(),
    outputStream = jsonBuffer.outputStream(),
  )

  @After
  fun reset() {
    jsonBuffer.close()
  }

  @Test
  fun doesEncodeWorkResponseJsonCorrectly() {
    processor.writeWorkResponse(WorkResponse(requestId = 110, exitCode = 100, output = "foo"))

    val json = jsonBuffer.readString(Charsets.UTF_8)
    assertThat(json).isEqualTo("{\"requestId\":110,\"exitCode\":100,\"output\":\"foo\"}")
  }

  @Test
  fun doesParseWorkRequestJsonCorrectly() {
    jsonBuffer.writeString("{\"requestId\": 100, \"arguments\": [\"foo\"]}", Charsets.UTF_8)

    val workRequest = processor.readWorkRequest()
    assertThat(workRequest).isNotNull
    assertThat(workRequest.requestId).isEqualTo(100)
    assertThat(workRequest.arguments).containsExactly("foo")
  }

  @Test
  fun doesDefaultRequestIDToZeroWhenNotPresent() {
    jsonBuffer.writeString("{\"arguments\": []}", Charsets.UTF_8)

    val workRequest = processor.readWorkRequest()
    assertThat(workRequest).isNotNull
    assertThat(workRequest.requestId).isEqualTo(0)
  }

  @Test
  fun doesCrashWhenArgumentsNotPresent() {
    jsonBuffer.writeString("{\"requestId\": 100}", Charsets.UTF_8)

    try {
      processor.readWorkRequest()
      fail<Unit>("readNextWorkRequest is expected to fail when arguments not provided!")
    } catch (e: Exception) {
      assertThat(e).hasMessage("Required value 'arguments' missing at \$")
    }
  }
}
