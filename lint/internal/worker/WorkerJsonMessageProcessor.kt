import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.*
import java.io.InputStream
import java.io.OutputStream

class WorkerJsonMessageProcessor(
  inputStream: InputStream,
  outputStream: OutputStream,
) : Worker.WorkerMessageProcessor {

  // Moshi JSON type adapters
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  private val workRequestAdapter: JsonAdapter<WorkRequest> = moshi.adapter(WorkRequest::class.java)
  private val workResponseAdapter: JsonAdapter<WorkResponse> =
    moshi.adapter(WorkResponse::class.java)

  // Streams to read and write on
  private val bufferedInputStream: BufferedSource = inputStream.source().buffer()
  private val bufferedOutputStream: BufferedSink = outputStream.sink().buffer()

  override fun readWorkRequest(): WorkRequest {
    val workRequest = workRequestAdapter.fromJson(bufferedInputStream)
    requireNotNull(workRequest) { "Error reading next work request!" }
    return workRequest
  }

  override fun writeWorkResponse(workResponse: WorkResponse) {
    workResponseAdapter.toJson(bufferedOutputStream, workResponse)
    bufferedOutputStream.flush()
  }
}
