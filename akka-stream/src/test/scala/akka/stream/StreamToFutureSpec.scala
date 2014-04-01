/**
 * Copyright (C) 2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.stream

import akka.testkit.AkkaSpec
import akka.stream.testkit.ScriptedTest
import scala.concurrent.forkjoin.ThreadLocalRandom.{ current ⇒ random }
import akka.stream.testkit.StreamTestKit
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Failure

class StreamToFutureSpec extends AkkaSpec with ScriptedTest {

  val gen = ProcessorGenerator(GeneratorSettings(
    initialInputBufferSize = 2,
    maximumInputBufferSize = 16,
    initialFanOutBufferSize = 1,
    maxFanOutBufferSize = 16))

  "A Stream with toFuture" must {

    "yield the first value" in {
      val p = StreamTestKit.producerProbe[Int]
      val f = Stream(p).toFuture(gen)
      val proc = p.expectSubscription
      proc.expectRequestMore()
      proc.sendNext(42)
      Await.result(f, 100.millis) should be(42)
      proc.expectCancellation()
    }

    "yield the first error" in {
      val p = StreamTestKit.producerProbe[Int]
      val f = Stream(p).toFuture(gen)
      val proc = p.expectSubscription
      proc.expectRequestMore()
      val ex = new RuntimeException("ex")
      proc.sendError(ex)
      Await.ready(f, 100.millis)
      f.value.get should be(Failure(ex))
    }

  }

}