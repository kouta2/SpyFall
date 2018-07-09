import socket
import threading
import json

from GameManager import GameManager

class Server(object):

    '''
    Handler function used to handle a users request to the server
    Takes in a socket to communicate with the server and address information
    '''
    def handler(self, socket, address):
        print("received a connection")
        print("connection: " + str(socket))
        print("address: " + str(address))
        while True:
            data = ''
            try:
                data = socket.recv(100) # receive a message from user
            except:
                self.leave_session_msg('Room Name Not Given', socket)
                socket.close()
                return
            if not data:
                print('0 bytes of data')
                self.leave_session_msg('Room Name Not Given', socket)
                socket.close()
                return
            else:
                try: # try to parse data
                    # data = data.strip()
                    data = data.decode('utf-8')
                    data = (data.split('\n'))[0]
                    print(data)
                    self.handle_received_data(data, socket)
                except BaseException as e: # user's message was malformed so let user know
                    print('Malformed input')
                    print(str(e))
                    print(data)
                    reason = 'invalid data format'
                    print(reason)
                    return_msg = {'type': 'Unknown', 'room_name': None, 'status': 'failed', 'reason': reason}
                    socket.send(str.encode(json.dumps(return_msg)))

    '''
    Parses a line of data received from address. socket is where the message came from.

    What data can look like:
    data = "Start:<room_name>:<timer>"
    data = "Start:<room_name>:<timer>:<comma separated list_of_words>
    data = "Leave_Session:<room_name>:<game_end>"
    data = "End_Session:<room_name>"
    data = "End_Game:<room_name>"
    data = "Create:<username>"
    data = "Join:<room_name>:<username>"
    '''
    def handle_received_data(self, data, socket):
        data_split = data.split(':')
        msg_type = data_split[0] # (Start | End | Create | Join)
        print('Message split is: ')
        print(data_split)
        if msg_type == 'Start' and (len(data_split) == 3 or len(data_split) == 4):
            self.start_msg(data_split[1:], socket)
        elif msg_type == 'Leave_Session' and len(data_split) == 2:
            self.leave_session_msg(data_split[1].strip(), socket)
        elif msg_type == 'End_Session' and len(data_split) == 2:
            self.end_session_msg(data_split[1].strip(), socket)
        elif msg_type == 'End_Game' and len(data_split) == 2:
            self.end_game_msg(data_split[1].strip(), socket)
        elif msg_type == 'Create' and len(data_split) == 2:
            self.create_msg(data_split[1].strip(), socket)
        elif msg_type == 'Join' and len(data_split) == 3:
            self.join_msg(data_split[1].strip(), data_split[2].strip(), socket)
        else: # Command was unknown so report to user that their command is unknown
            reason = 'invalid data format'
            print(reason)
            return_msg = {'type': 'Unknown', 'room_name': None, 'status': 'failed', 'reason': reason}
            socket.send(str.encode(json.dumps(return_msg) + '\n'))
            # socket.send(str.encode('\n'))

    '''
    Helper function to parse a start command. 
    Updates game manager and either sends success to everyone in the room or failure to just the person 
    requesting to start the game
    '''
    def start_msg(self, data, socket):
        messages = self.game.start_game(data, socket)
        print(messages)
        for (sock, return_msg) in messages:
            sock.send(str.encode(json.dumps(return_msg) + '\n'))

    '''
    Helper function to parse a Leave_Session command.
    Updates game manager and either sends everyone a message of who left or failure to just the person 
    requesting to leave the game 
    '''
    def leave_session_msg(self, room_name, socket):
        messages = self.game.leave_session(room_name, socket)
        print(messages)
        for (sock, return_msg) in messages:
            sock.send(str.encode(json.dumps(return_msg) + '\n'))
            # sock.send(str.encode('\n'))
        # socket.close() # end tcp connection and make them connect again by joining/creating a new session

    '''
    Helper function to parse a End_Session command.
    Updates game manager and either sends everyone a message of who ended the room or failure to just the person 
    requesting to end the room
    '''
    def end_session_msg(self, room_name, socket):
        messages = self.game.end_session(room_name, socket)
        print(messages)
        for (sock, return_msg) in messages:
            sock.send(str.encode(json.dumps(return_msg) + '\n'))
            # sock.send(str.encode('\n'))
            # sock.close() # end tcp connection and make them connect again by joining/creating a new session

    '''
    Helper function to parse a End_Game command.
    Updates game manager and lets the person know who sent the message that the game is successfully over on the server
    '''
    def end_game_msg(self, room_name, socket):
        messages = self.game.end_game(room_name, socket)
        print(messages)
        for (sock, return_msg) in messages:
            sock.send(str.encode(json.dumps(return_msg) + '\n'))
            # sock.send(str.encode('\n'))
    #
    '''
    Helper function to parse a Create command.
    Updates game manager and either lets the person know whether the room was successfully created or not
    '''
    def create_msg(self, username, socket):
        room_name, return_msg = self.game.create_room(username, socket)
        print('Return message is: ' + str(json.dumps(return_msg)))
        socket.send(str.encode(json.dumps(return_msg) + '\n'))
        # socket.send(str.encode('\n'))

    '''
    Helper function to parse a Join command.
    Updates game manager and either sends everyone a message of who joined the room or failure to just the person 
    requesting to join the room
    '''
    def join_msg(self, room_name, username, socket):
        messages = self.game.join_room(room_name, username, socket)
        print(messages)
        for (sock, return_msg) in messages:
            sock.send(str.encode(json.dumps(return_msg) + '\n'))
            # sock.send(str.encode('\n'))

    '''
    Server constructor that sets up the game and the socket to receive messages
    '''
    def __init__(self):
        self.game = GameManager()
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM) # create a socket to accept connections

        # address = 192.168.198.1
        sock.bind(('0.0.0.0', 10001))
        sock.listen(1)

        while True:
            c, a = sock.accept()
            thread = threading.Thread(target=self.handler, args=(c, a)) # for every connection, span a thread to handle it
            thread.daemon = True
            thread.start()

if __name__=="__main__":
    server = Server() # starts the server

