# play-with-websockets
A simple Scala. Play 2 demo app to show off how web sockets work, how to test them

The app stores websockets and a list of friends each user has.  Allowing them to talk to each other and coordinate watching TV.

## To run:

sbt run

open a browser window (I use chrome)
go to : http://www.websocket.org/echo.html
change location field to : ws://localhost:9000/wsWithActor
click connect  (it should say Connected in the textarea)
in the message box type:
    user:1,Bruce  (it should say "Hi Bruce")
    friends:2

open a second browser tab
go to : http://www.websocket.org/echo.html
change location to : ws://localhost:9000/wsWithActor
click connect  (it should say connected)
in the message box type:
    user:2,Bob  (it should say “Hi Bob”)
    friends:1  (it should say “Friend Bruce is online”)

If you go back to the 1st tab, you will see: “Friend Bob in online"
In tab 1 type
    Hi Bob, want to watch a movie
    start:movie

in tab 2, type
    start:movie

you can also try
    stop:movie

caveat: I wrote this code for a Hack day where time was very limited.  Its not the cleanest code, nor production ready in at all!  It's a Proof of concept, how to use and test websockets.  If you want to use it, I’d recommend cleaning up the Maps usage.  Also I have not listened to closing events to cleanup the maps.