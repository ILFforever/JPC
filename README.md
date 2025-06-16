# JPC
JavaPeerConnect (JPC) is a simple, peer-to-peer chat application built using JavaFX allowing users to chat to and from using their LAN.

## Background 
JPC was created as a POC (Proof of concept) as a LAN communication system backbone for our team's multiplayer game [AmongCEDT TODO!](https://github.com/adam-p/markdown-here/wiki/markdown-cheatsheet) project for Chula Engineering Software Engineering course. 🎓

## Features
- **Host a Server**: Start your own chat server that others can connect to.
- **Connect as Client**: Join an existing chat server to send and receive messages.
- **Dynamic Port Assignment**: Servers automatically select a port between 1024 and 65535.
- **User Nicknames**: Set a custom name for yourself before connecting or hosting.
- **Real-time Logging**: All chat messages and system events are displayed in a dedicated log area.
- **Server Client Management**: The server keeps track of connected clients and their names.
- **Ping-Pong Heartbeat**: The server sends regular pings to clients, and clients respond with pongs. If a client misses too many pongs, they are considered disconnected.
- **Hand Shaking**: The server does initial handshaking for clients to ensure communication success.
- **System Commands**:
  - Massage types are annouced using massage headers.
  - /sys/ls: Both clients and servers can use this command to request a list of connected players and the server's own address/port.
* **Disconnect Functionality**: Easily disconnect from a server or shut down a hosted server.

## Images
<details>
  <summary>Click to view images</summary>
  
![Home Page](https://github.com/ILFforever/JPC/blob/main/Images/home.png)
  
![Host Page](https://github.com/ILFforever/JPC/blob/main/Images/hoster.png)

![Client Page](https://github.com/ILFforever/JPC/blob/main/Images/client.png)

![Both Page](https://github.com/ILFforever/JPC/blob/main/Images/both.png)

![Command Page](https://github.com/ILFforever/JPC/blob/main/Images/sysls.png)

</details>

## Credit
Chulalongkorn University's Software Enginnering 2110423 for the Assignment.
