package controllers

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import play.api.Play.current
import play.api._
import play.api.libs.ws._
import play.api.mvc._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set

class Application @Inject()(ws: WSClient) extends Controller {

  val clients = ListBuffer[ActorRef]()

  val userIdMap = mutable.Map[ActorRef, String]() // socket->id
  val userSocketMap = mutable.Map[String, ActorRef]() //Id->socket
  val userNameMapById = mutable.Map[String, String]() //Id -> Name
  val userStreamById = mutable.Map[String, String]() //Id -> stream
  val userFriendsMap = mutable.Map[String, Set[String]]() //Id -> ids

  val reg = "(.*):(.*)".r

  def index = Action {
    Logger.info(s"Hello!")
    Ok("Hello!")
  }

  class EchoWebSocketActor(out: ActorRef) extends Actor {
    def receive = {
      case msg: String =>

        msg match {

          case reg("user", userData) => {//fbid,name
            Logger.info(s"got user data: $userData")
            if (userData.contains(",")) {
              val id = userData.split(",").head //
              val name = userData.split(",").tail.head
              sendMessageToMeOnly(s"Hi $name")
              userIdMap.put(out, id)
              userSocketMap.put(id, out)
              userNameMapById.put(id, name)
            } else {
              Logger.warn("bad username!!")
            }
          }
          case reg("friends", friendIds) => addFriends(friendIds.split(","))

          case reg("start", stream) => {
            Logger.info(s"started stream $stream")
            userIdMap.get(out) map {userStreamById.put(_, stream)}
            val peopleMessaged = sendMessageToFriends(s"$myUserName has joined the stream", false, Some(stream))
            val names = peopleMessaged map { friendUserName(_) }
            if (names.nonEmpty) {
              if (names.size==1)
                sendMessageToMeOnly(s"${names.mkString(", ")} is watching this stream")
              else
                sendMessageToMeOnly(s"${names.mkString} are watching this stream")
            }
          }

          case reg("stop", stream) => {
            Logger.info(s"stopped stream $stream")
            userIdMap.get(out) map {userStreamById.remove(_)}
            val peopleMessaged = sendMessageToFriends(s"$myUserName has left the stream", false, Some(stream))
            val names = peopleMessaged map { friendUserName(_) }
          }

          case _ =>
            if(msg.equalsIgnoreCase("Hello Server")) sendMessageToMeOnly("Hello Client")
            if(msg=="goodbye") self ! PoisonPill
            else sendMessageToFriends(msg, false, None)
        }

    }

    def addFriends(friendIds:Array[String]) {
      userIdMap.get(out) match {
        case Some(myUserId) =>
          Logger.info(s"got your friend list : ${friendIds.mkString(", ")}")
          userFriendsMap.put(myUserId, Set(friendIds :_* ))

          friendIds.foreach { friendUserId =>
            userSocketMap.get(friendUserId) match {
              case Some(friendSocket) => {

                val messageAboutFriend: String = s"Friend ${friendUserName(friendUserId)} is online"
                Logger.info(messageAboutFriend)
                friendSocket ! s"Friend $myUserName is online"
                sendMessageToMeOnly(messageAboutFriend)

                userFriendsMap.get(friendUserId) match {
                  case Some(myFriendsFriendsSet) => {
                    myFriendsFriendsSet.add(myUserId)
                  }
                  case None => {
                    userFriendsMap.put(friendUserId, Set(myUserId))
                  }//im the 1st friend
                }
              }
              case None => Logger.info(s"Friend id number $friendUserId is not online")
            }
          }
        case None => sendMessageToMeOnly("add your name/id first!!")
      }
    }

    def myUserName = userIdMap.get(out) match {
      case None => "your friend"
      case Some(userId) => friendUserName(userId)
    }
    def myUserId:String = userIdMap.get(out) match {
      case None => "unknown"
      case Some(userId) => userId
    }

    def friendUserName(friendId:String) = userNameMapById.get(friendId) match {
      case None => "your friend"
      case Some(name) => name
    }

    def sendMessageToFriends(msg:String, echoBack:Boolean, streamOpt:Option[String]): ListBuffer[String] = {

      val peopleMessaged = ListBuffer[String]()
      Logger.info(s"message: $msg")

      userFriendsMap.get(myUserId) match {
        case Some(friends) => {
          friends foreach { friendId =>
            streamOpt match {
              case Some(stream) =>
                //only if the streams match
                userStreamById.get(friendId) map {friendStream => if (friendStream == stream) {
                  peopleMessaged.append(friendId)
                  userSocketMap.get(friendId) map { _ ! (msg) }
                }}
                //None means send to everyone
              case None => {
                userSocketMap.get(friendId) map { _ ! (s"${myUserName} > " + msg) }
                peopleMessaged.append(friendId)
              }
            }

          }
        }
        case None =>
      }
      if (echoBack)
        out ! ("I received your message: " + msg)
      peopleMessaged
    }

    def sendMessageToMeOnly(msg:String): Unit = {
      Logger.info(s"$myUserName $msg")
      out ! (msg)
    }
  }

  def wsConnect = WebSocket.acceptWithActor[String, String] {
    request =>
      out:ActorRef => {
        Logger.info("wsWithActor, client connected")
        clients.append(out)
        Props(new EchoWebSocketActor(out))
      }
  }

}
