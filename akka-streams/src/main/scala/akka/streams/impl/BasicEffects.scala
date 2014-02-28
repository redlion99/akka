package akka.streams.impl

import scala.language.existentials
import rx.async.spi.{ Publisher, Subscription, Subscriber }

/** Predefined effects */
object BasicEffects {
  // Subscriber

  case class SubscriberOnNext[O](subscriber: Subscriber[O], o: O) extends ExternalEffect {
    def run() = subscriber.onNext(o)
  }
  case class SubscriberOnComplete(subscriber: Subscriber[_]) extends ExternalEffect {
    def run() = subscriber.onComplete()
  }
  case class SubscriberOnError(subscriber: Subscriber[_], cause: Throwable) extends ExternalEffect {
    def run() = subscriber.onError(cause)
  }

  def forSubscriber[I](subscriber: ⇒ Subscriber[I]): Downstream[I] =
    new Downstream[I] {
      override def toString: String = s"Downstream from Subscriber $subscriber"

      lazy val next: I ⇒ Effect = BasicEffects.SubscriberOnNext(subscriber, _)
      lazy val complete: Effect = BasicEffects.SubscriberOnComplete(subscriber)
      lazy val error: Throwable ⇒ Effect = BasicEffects.SubscriberOnError(subscriber, _)
    }

  // Subscription

  case class RequestMoreFromSubscription(subscription: Subscription, n: Int) extends ExternalEffect {
    require(n > 0)
    require(subscription ne null)
    def run(): Unit = subscription.requestMore(n)
  }
  case class CancelSubscription(subscription: Subscription) extends ExternalEffect {
    def run(): Unit = subscription.cancel()
  }

  def forSubscription(subscription: ⇒ Subscription): Upstream =
    new Upstream {
      lazy val requestMore: Int ⇒ Effect = BasicEffects.RequestMoreFromSubscription(subscription, _)
      lazy val cancel: Effect = BasicEffects.CancelSubscription(subscription)
    }

  case class Subscribe[T](publisher: Publisher[T], subscriber: Subscriber[T]) extends ExternalEffect {
    def run(): Unit = publisher.subscribe(subscriber)
  }

  // SyncSink

  case class HandleNextInSink[B](right: SyncSink[B], element: B) extends SingleStep {
    def runOne(): Effect = right.handleNext(element)
  }
  case class CompleteSink(right: SyncSink[_]) extends SingleStep {
    def runOne(): Effect = right.handleComplete()
  }
  case class HandleErrorInSink(right: SyncSink[_], cause: Throwable) extends SingleStep {
    def runOne(): Effect = right.handleError(cause)
  }

  def forSink[B](sink: SyncSink[B]): Downstream[B] =
    new Downstream[B] {
      val next: B ⇒ Effect = HandleNextInSink(sink, _)
      val complete: Effect = CompleteSink(sink)
      val error: Throwable ⇒ Effect = HandleErrorInSink(sink, _)
    }

  // SYncSource

  case class RequestMoreFromSource(left: SyncSource, n: Int) extends SingleStep {
    require(n > 0)
    def runOne(): Effect = left.handleRequestMore(n)
  }
  case class CancelSource(left: SyncSource) extends SingleStep {
    def runOne(): Effect = left.handleCancel()
  }

  def forSource[B](source: SyncSource): Upstream =
    new Upstream {
      val requestMore: Int ⇒ Effect = RequestMoreFromSource(source, _)
      val cancel: Effect = CancelSource(source)
    }
}
