import org.specs2.mutable._
import play.api.test.Helpers._
import play.api.test._

class ApplicationSpec extends Specification {

  "Application" should {

    "work" in {
      running(TestServer(9000)) {

        val clientInteraction = new ClientInteraction()

        clientInteraction.client.connectBlocking()
        clientInteraction.client.send("Hello Server")

        eventually {
          clientInteraction.messages.contains("Hello Client")
        }
      }
    }

    "interact with friends" in {
      running(TestServer(9000)) {

        val clientInteractionBruce = new ClientInteraction()

        clientInteractionBruce.client.connectBlocking()
        clientInteractionBruce.client.send("user:1,Bruce")
        clientInteractionBruce.client.send("friends:2,Bob")
        eventually {
          clientInteractionBruce.messages.contains("Hi Bruce")
        }

        val clientInteractionBob = new ClientInteraction()
        clientInteractionBob.client.connectBlocking()
        clientInteractionBob.client.send("user:2,Bob")
        clientInteractionBob.client.send("friends:1,Bruce")

        eventually {
          clientInteractionBob.messages.contains("Hi Bob")
          clientInteractionBruce.messages.contains("Friend Bob is online")
          clientInteractionBob.messages.contains("Friend Bruce is online")
        }

        clientInteractionBruce.client.send("Shall we watch a movie?")
        eventually {
          clientInteractionBob.messages.contains("Bruce > Shall we watch a movie?")
        }

        clientInteractionBruce.client.send("start:movies-stream")
        Thread.sleep(100)

        clientInteractionBob.client.send("start:movies-stream")
        eventually {
          clientInteractionBruce.messages.toList === List(
            "Hi Bruce",
            "Friend Bob is online",
            "Bob has joined the stream"
          )
          clientInteractionBob.messages.toList ===  List(
            "Hi Bob",
            "Friend Bruce is online",
            "Bruce > Shall we watch a movie?",
            "Bruce is watching this stream"
          )
        }

      }}
  }
}
