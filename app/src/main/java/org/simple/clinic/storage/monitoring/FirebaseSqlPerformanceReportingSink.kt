package org.simple.clinic.storage.monitoring

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import org.simple.clinic.storage.monitoring.SqlPerformanceReporter.ReportSink
import org.simple.clinic.storage.monitoring.SqlPerformanceReporter.SqlOperation

class FirebaseSqlPerformanceReportingSink : ReportSink {

  /**
   * Maintains the list of running spans for every DB call.
   *
   * This can potentially lead to memory leaks if there are DB operations that start but never
   * finish. However, the chances of that seem small enough that it may not be necessary to take
   * on the maintenance effort of periodically removing long running spans.
   *
   * Something to investigate at a later date.
   **/
  private var runningSpans = emptyMap<SqlOperation, Trace>()

  override fun begin(operation: SqlOperation) {
    val trace = FirebasePerformance.getInstance()
        .newTrace("room.query")
        .apply {
          putAttribute("dao", operation.daoName)
          putAttribute("method", operation.methodName)

          start()
        }

    runningSpans = runningSpans + (operation to trace)
  }

  override fun end(operation: SqlOperation) {
    val traceForOperation = runningSpans[operation]

    /*
    * There is a possibility for a completed operation to not be present in the list of running
    * spans since we make copies of the map of running spans and the DB queries can run on multiple
    * threads.
    *
    * In this scenario, we'll just not report this span since the cost of thread synchronisation is
    * not worth the small chances of a span being lost.
    **/
    if (traceForOperation != null) {
      traceForOperation.stop()
      runningSpans = runningSpans - operation
    }
  }
}
