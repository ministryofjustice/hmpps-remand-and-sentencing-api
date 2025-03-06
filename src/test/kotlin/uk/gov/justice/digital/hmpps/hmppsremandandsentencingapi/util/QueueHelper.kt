package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.util

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.util.concurrent.TimeUnit

fun numberOfMessagesCurrentlyOnQueue(client: SqsAsyncClient, queueUrl: String, count: Int) {
  await untilCallTo {
    client.countAllMessagesOnQueue(queueUrl).get()
  } matches { it == count }
}
