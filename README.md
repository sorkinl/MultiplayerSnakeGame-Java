# 2-person snake game
2-person game with HTML/CSS/JavaScript client and Java WebSocket Server



## Usage

Change the value of the IP variable in line 26 of the snakeclient.html, to the IP address of the host machine where you are running the server. After that run:

```
java -jar MultiThreadWebSocket.jar
```
Once the server is running, open the snakeclient.html on 2 different machines and start playing. The game will stop if you bump into each other or when one of the players reaches 200 points.
