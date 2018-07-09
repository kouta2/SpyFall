import random
from threading import Lock
from ListOfWords import list_of_words

'''        
    TODO: Handle disconnections
'''
class GameManager(object):


    def __init__(self):
        global list_of_words
        ''' 
        self.rooms maps a self.length_of_room_names letter room name to a list of tuples containing
        (True/False for whether the person is a spy, (ip address, port) tuple of the person) 
        '''

        '''
        keeps track of the of the people in each room. (True/Fales for if spy, socket, username)
        Gets added to in join_room and create_room and removed from in end_session and leave_session (when no players left)
        '''
        self.rooms = {}

        # maps room_name to a mutex lock
        self.locks = {}

        # maps room_name to the list words. Gets populated on start_game and unpopulated in end_session and leave_session (when no players left)
        self.words_for_room = {}

        # maps socket to username. Add to dictionary in join_room and create_room and remove in end_session and leave_session
        self.socket_to_username = {}

        # holds a set of room names that are currently in a game. room_name added on start_game and removed on end_game
        self.games_in_progress = set()

        # maps a socket to the room name the socket is in. Add to dictionary in join_room and create_room and remove in end_session and leave_session
        self.socket_to_room = {}

        self.min_people_to_start_game = 3
        self.num_words_in_game = 20
        self.length_of_room_names = 7
        self.list_of_words = list_of_words

    '''
    params: self.length_of_room_names letter room_name and a socket to send to data back to 
    
    result: adds a tuple (True/False of whether he is a spy, socket) to room_name in rooms.
    note: Assumes room was created already or else returns an error message
    returns: array of tuples of (socket, json message response describing result of executing function)

    TODO: check if game already started and either return an error message or check if the person was a user who disconnected from game
    '''
    def join_room(self, room_name, username, socket):
        print('Joining room ' + room_name)
        if room_name not in self.rooms:
            reason = 'room name does not exist'
            return [(socket, {'type': 'Joined', 'room_name': room_name, 'usernames': [username], 'status': 'failed', 'reason': reason})]
        elif socket in self.socket_to_room:
            reason = 'user is already a part of a room'
            return [(socket, {'type': 'Joined', 'room_name': room_name, 'usernames': [username], 'status': 'failed', 'reason': reason})]
        elif self.username_used(username, room_name):
            reason = 'The username: ' + username + ' is already taken'
            return [(socket, {'type': 'Joined', 'room_name': room_name, 'usernames': [username], 'status': 'failed', 'reason': reason})]
        else:
            messages = []
            self.locks[room_name].acquire()
            try:
                self.socket_to_username[socket] = username
                self.socket_to_room[socket] = room_name
                messages = [(data[1], {'type': 'Joined', 'room_name': room_name, 'usernames': [username], 'status': 'success', 'reason': None}) for data in self.rooms[room_name]]
                messages.append((socket, {'type': 'Joined', 'room_name': room_name, 'usernames': self.get_usernames(room_name), 'status': 'success', 'reason': None}))
                self.rooms[room_name].append((False, socket, username)) # default people to not being the spy
            finally:
                self.locks[room_name].release()
            return messages

    '''
    Returns a tuple of (string of a room not in self.rooms, and json message response)
    '''
    def create_room(self, username, socket):
        print('Creating a room')
        if socket in self.socket_to_room:
            reason = 'user is already a part of a room'
            return (socket, {'type': 'Created', 'room_name': self.socket_to_room[socket], 'usernames': [username], 'status': 'failed', 'reason': reason})
        else:
            self.socket_to_username[socket] = username
            new_room_created = False
            sample = ''
            while not new_room_created:
                sample = self.generate_x_length_letter_string(self.length_of_room_names)
                if sample not in self.rooms:
                    self.rooms[sample] = [(False, socket, username)]
                    self.socket_to_room[socket] = sample
                    new_room_created = True
            self.locks[sample] = Lock()
            return_msg = {'type': 'Created', 'room_name': sample, 'usernames': [username], 'status': 'success', 'reason': None}
            return (sample, return_msg)

    '''
    Given room_name and socket, notifies everyone in the room that person associated with socket left the room
    returns json message response
    '''
    def leave_session(self, room_name, socket):
        print('Leaving room ' + room_name)
        if room_name == 'Room Name Not Given' and socket in self.socket_to_username: # this is when there is a disconnection
            messages = []
            print(self.socket_to_username)
            print(self.socket_to_room)
            username = self.socket_to_username[socket]
            room = self.socket_to_room[socket]
            if room in self.locks:
                self.locks[room].acquire()
                try:
                    is_game_over = len(self.rooms[room]) == 3 or self.is_spy(room, socket)
                    self.rooms[room] = self.remove_user(room, socket)
                    del self.socket_to_username[socket]
                    del self.socket_to_room[socket]
                    reason = username + ' Left the session'
                    end = False
                    if is_game_over == True and room in self.games_in_progress:
                        self.games_in_progress.remove(room)
                        end = True
                    messages = [(data[1], {'type': 'Leave_Session', 'room_name': room, 'usernames': [username], 'status': 'success', 'reason': reason, 'end': end}) for data in self.rooms[room]]
                finally:
                    self.locks[room].release()
            return messages
        elif socket in self.socket_to_room and self.socket_to_room[socket] == room_name:
            messages = []
            self.locks[room_name].acquire()
            try:
                reason = self.socket_to_username[socket] + ' Left the session'
                username = self.socket_to_username[socket]
                messages = [(data[1], {'type': 'Leave_Session', 'room_name': room_name, 'usernames': [username], 'status': 'success', 'reason': reason, 'end': False}) for data in self.rooms[room_name]]
                del self.socket_to_username[socket]
                del self.socket_to_room[socket]
                if len(self.rooms[room_name]) == 1:
                    del self.rooms[room_name]
                    if room_name in self.words_for_room:
                        del self.words_for_room[room_name]
                else:
                    self.rooms[room_name] = self.remove_user(room_name, socket)
            finally:
                self.locks[room_name].release()
            return messages
        elif socket in self.socket_to_username:
            reason = self.socket_to_username[socket] + ' is not a part of the game'
            return [(socket, {'type': 'Leave_Session', 'room_name': room_name, 'status': 'failed', 'reason': reason, 'end': False})]
        else:
            reason = 'User is not a part of any game'
            return [(socket, {'type': 'Leave_Session', 'room_name': room_name, 'status': 'failed', 'reason': reason, 'end': False})]

    '''
    Given the self.length_of_room_names letter room_name, removes the room from self.rooms
    returns: json message response
    '''
    def end_session(self, room_name, socket):
        print('Ending room ' + room_name)
        if socket in self.socket_to_room and self.socket_to_room[socket] == room_name:
            messages = []
            self.locks[room_name].acquire()
            try:
                reason = 'Ended by ' + self.socket_to_username[socket]
                messages = [(data[1], {'type': 'Ended_Session', 'room_name': room_name, 'status': 'success', 'reason': reason}) for data in self.rooms[room_name]]
                del self.rooms[room_name]
                del self.socket_to_username[socket]
                del self.socket_to_room[socket]
                if room_name in self.words_for_room:
                    del self.words_for_room[room_name]
            finally:
                self.locks[room_name].release()
            del self.locks[room_name]
            return messages
        else:
            reason = 'User is not a part of the game'
            return [(socket, {'type': 'Ended_Session', 'room_name': room_name, 'status': 'failed', 'reason': reason})]


    '''
    Given room_name ends and clears data about the game running in room_name. 
    Notifies everyone in the room that the game is over
    '''
    def end_game(self, room_name, socket):
        print('Ending game for room ' + room_name)
        if socket in self.socket_to_room and self.socket_to_room[socket] == room_name:
            self.locks[room_name].acquire()
            try:
                if room_name in self.games_in_progress:
                    self.games_in_progress.remove(room_name)
                    
                    self.rooms[room_name] = [(False, data[1], data[2]) for data in self.rooms[room_name]] # reset it so no one is the spy
            finally:
                reason = 'Game ended by ' + self.socket_to_username[socket]
                self.locks[room_name].release()
                return [(data[1], {'type': 'Ended_Game', 'room_name': room_name, 'status': 'success', 'reason': reason}) for data in self.rooms[room_name]]
        else:
            reason = 'User is not a part of the game'
            return [(socket, {'type': 'Ended_Game', 'room_name': room_name, 'status': 'failed', 'reason': reason})]

    '''
    Given a room_name, selects a spy, generates words to be used in the game, and the specific word. 
    Notifies everyone in room that the game started and who is what character.
    If there is an error, it will only notify the person who started the message
    returns: json message response
    '''
    def start_game(self, data, socket):
        room_name = data[0]
        timer = int(data[1].strip())
        print('Starting a game in room ' + room_name)
        reason = ''
        if socket in self.socket_to_room and self.socket_to_room[socket] == room_name and room_name in self.rooms and len(self.rooms[room_name]) >= self.min_people_to_start_game:
            message = []
            self.locks[room_name].acquire()
            try:
                self.select_spy(room_name)
                words_in_game = []
                if len(data) == 3:
                    words_in_game = data[2].strip().split(',')
                else:
                    words_in_game = self.get_words()
                selected_word = self.select_word(words_in_game)
                self.words_for_room[room_name] = (words_in_game, selected_word)
                self.games_in_progress.add(room_name)
                message = self.generate_start_response_messages(room_name, timer)
            finally:
                self.locks[room_name].release()
            return message
        elif room_name not in self.rooms:
            reason = room_name + ' does not exist'
        elif socket not in self.socket_to_room or (socket in self.socket_to_room and self.socket_to_room[socket] != room_name):
            reason = 'User is not a part of the game'
        else:
            reason = room_name + ' has less than ' + str(self.min_people_to_start_game) + ' people. You need at least ' + str(self.min_people_to_start_game) + ' to start a game'
        print(reason)
        return [(socket, {'type': 'Started', 'room_name': room_name, 'status': 'failed', 'reason': reason, 'spy': False, 'list_of_words': None, 'specific_word': None})]


    '''
    Given a room_name and timer, returns a list of tuples (socket, response message) regarding starting the game
    '''
    def generate_start_response_messages(self, room_name, timer):
        spy_msg = {'type': 'Started', 'room_name': room_name, 'status': 'success', 'reason': None, 'spy': True, 'list_of_words': self.words_for_room[room_name][0], 'specific_word': None, 'timer': timer}
        others_msg = {'type': 'Started', 'room_name': room_name, 'status': 'success', 'reason': None, 'spy': False, 'list_of_words': self.words_for_room[room_name][0], 'specific_word': self.words_for_room[room_name][1], 'timer': timer}
        return [(data[1], spy_msg) if data[0] else (data[1], others_msg) for data in self.rooms[room_name]]


    def send_start_game_msg(self, room_name):
        print('Sending start message')

    '''
    Assumes room_name has at least self.min_people_to_start_game people
    '''
    def select_spy(self, room_name):
        spy_index = random.randint(0, len(self.rooms[room_name]) - 1)
        self.rooms[room_name][spy_index] = (True, self.rooms[room_name][spy_index][1], self.rooms[room_name][spy_index][2])

    '''
    Selects a random self.num_words_in_game words from list_of_words for the game
    '''
    def get_words(self):
        selected_words = set()
        while len(selected_words) < self.num_words_in_game:
            index = random.randint(0, len(self.list_of_words) - 1)
            selected_words.add(self.list_of_words[index])
        selected_words = list(selected_words)
        selected_words.sort()
        return selected_words

    '''
    Selects a word from list of words in the game
    '''
    def select_word(self, words_in_game):
        index = random.randint(0, len(words_in_game) - 1)
        return words_in_game[index]

    '''
    Creates a random x_length letter string
    '''
    def generate_x_length_letter_string(self, x_length):
        word_list = ['a' for i in range(x_length)]
        for i in range(x_length):
            index = random.randint(0, 25) # always 26 letters in english alphabet
            word_list[i] = chr(index + 65)
        return ''.join(word_list)

    '''
    Given a room_name returns the list of usernames in that room
    '''
    def get_usernames(self, room_name):
        return [user[2] for user in self.rooms[room_name]]

    '''
    Checks if the username is used in the room
    '''
    def username_used(self, username, room_name):
        for user in self.rooms[room_name]:
            if user[2] == username:
                return True
        return False

    '''
    Removes a user from self.rooms if he is in it
    '''
    def remove_user(self, room_name, socket):
        for i in range(len(self.rooms[room_name])):
            user = self.rooms[room_name][i]
            if user[1] == socket:
                return self.rooms[room_name][:i] + self.rooms[room_name][i+1:]
        return self.rooms[room_name]

    '''
    Given room name and socket, determines if user is the spy
    '''
    def is_spy(self, room_name, socket):
        for i in range(len(self.rooms[room_name])):
            user = self.rooms[room_name][i]
            if user[1] == socket:
                return (user[0] == True)
        return False

if __name__=="__main__":
    game = GameManager()
    print(game.get_words())