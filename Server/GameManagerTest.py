from unittest import TestCase
from GameManager import GameManager
from threading import Lock

class GameManagerTest(TestCase):

    def test_join_room(self):
        g = GameManager()
        room = 'ASDFGHJ'
        username = 'username'
        socket = 'None'
        reason = 'room name does not exist'
        g.locks[room] = Lock()
        self.assertEquals(g.join_room(room, username, socket), [(socket, {'type': 'Joined', 'room_name': 'ASDFGHJ', 'usernames': [username], 'status': 'failed', 'reason': reason})])

        g.rooms[room] = [(None, socket, username)]
        reason = 'The username: ' + username + ' is already taken'
        self.assertEquals(g.join_room(room, username, socket), [(socket, {'type': 'Joined', 'room_name': 'ASDFGHJ', 'usernames': [username], 'status': 'failed', 'reason': reason})])

        g.socket_to_room[socket] = 'OTHERRM'
        g.rooms[room] = [(False, None, 'user1')]
        reason = 'user is already a part of a room'
        self.assertEquals(g.join_room(room, username, socket), [(socket, {'type': 'Joined', 'room_name': 'ASDFGHJ', 'usernames': [username], 'status': 'failed', 'reason': reason})])

        del g.socket_to_room[socket]
        expected = [(socket, {'type': 'Joined', 'room_name': 'ASDFGHJ', 'usernames': [username], 'status': 'failed', 'reason': reason})]
        g.join_room(room, username, socket)
        self.assertEquals(g.socket_to_room[socket], room)
        self.assertEquals(g.socket_to_username[socket], username)
        self.assertEquals(g.rooms[room], [(False, None, 'user1'), (False, socket, username)])

    def test_create_room(self):
        g = GameManager()
        username = 'username'
        socket = 'socket'
        g.create_room(username, socket)
        self.assertEquals(socket in g.socket_to_room, True)
        self.assertEquals(g.socket_to_username[socket], username)

        reason = 'user is already a part of a room'
        expected = (socket, {'type': 'Created', 'room_name': g.socket_to_room[socket], 'usernames': [username], 'status': 'failed', 'reason': reason})
        self.assertEquals(g.create_room(username, socket), expected)

    def test_leave_session(self):
        g = GameManager()
        room_name = 'AAAAAAA'
        g.locks[room_name] = Lock()

        socket = 'socket'
        g.rooms[room_name] = [(False, None, 'user1'), (False, None, 'user2'), (False, None, 'user3')]

        reason = 'User is not a part of any game'
        expected = [(socket, {'type': 'Leave_Session', 'room_name': room_name, 'status': 'failed', 'reason': reason, 'end': False})]
        self.assertEquals(g.leave_session(room_name, socket), expected)

        username = 'user4'
        g.rooms['BBBBBBB'] = [(False, socket, username)]
        g.socket_to_username[socket] = username
        reason = g.socket_to_username[socket] + ' is not a part of the game'
        expected = [(socket, {'type': 'Leave_Session', 'room_name': room_name, 'status': 'failed', 'reason': reason, 'end': False})]
        self.assertEquals(g.leave_session(room_name, socket), expected)

        g.rooms['BBBBBBB'] = []
        g.join_room(room_name, username, socket)
        self.assertEquals(len(g.rooms[room_name]), 4)
        self.assertEquals(g.socket_to_room[socket], room_name)
        self.assertEquals(g.socket_to_username[socket], username)

        g.leave_session(room_name, socket)
        self.assertEquals(len(g.rooms[room_name]), 3)
        self.assertEquals(socket in g.socket_to_room, False)
        self.assertEquals(socket in g.socket_to_username, False)

        g.rooms['ASDFQWER'] = [(False, None, 'user1'), (False, socket, 'user2'), (False, None, 'user3')]
        g.socket_to_username[socket] = 'user2'
        g.socket_to_room[socket] = 'ASDFQWER'
        g.locks['ASDFQWER'] = Lock()
        g.games_in_progress.add('ASDFQWER')
        g.leave_session('Room Name Not Given', socket)
        self.assertEquals('ASDFQWER' not in g.games_in_progress, True)
        self.assertEquals(g.username_used('user2', 'ASDFQWER'), False)


    def test_end_session(self):
        g = GameManager()
        room = 'ASDFGHJ'
        g.locks[room] = Lock()
        socket = 'socket'
        g.socket_to_room[socket] = room
        g.socket_to_username[socket] = 'user2'
        g.rooms[room] = [(False, None, 'user1'), (False, socket, 'user2'), (False, None, 'user3')]

        reason = 'User is not a part of the game'
        expected = [(socket, {'type': 'Ended_Session', 'room_name': 'AAAAAAA', 'status': 'failed', 'reason': reason})]
        self.assertEquals(g.end_session('AAAAAAA', socket), expected)

        g.end_session(room, socket)
        self.assertEquals(room in g.rooms, False)
        self.assertEquals(socket in g.socket_to_room, False)
        self.assertEquals(socket in g.socket_to_username, False)


    def test_end_game(self):
        g = GameManager()
        room = 'ASDFGHJ'
        username = 'asdf'
        g.locks[room] = Lock()
        socket = 'socket'
        g.socket_to_room[socket] = room
        g.games_in_progress.add(room)
        g.rooms[room] = [(False, 'socket1', None), (True, 'socket2', None), (False, socket, username)]
        g.socket_to_username[socket] = username

        reason = 'User is not a part of the game'
        expected = [(socket, {'type': 'Ended_Game', 'room_name': 'AAAAAAA', 'status': 'failed', 'reason': reason})]
        self.assertEquals(g.end_game('AAAAAAA', socket), expected)

        reason = 'Game ended by ' + username
        expected = [('socket1', {'type': 'Ended_Game', 'room_name': room, 'status': 'success', 'reason': reason}), ('socket2', {'type': 'Ended_Game', 'room_name': room, 'status': 'success', 'reason': reason}), (socket, {'type': 'Ended_Game', 'room_name': room, 'status': 'success', 'reason': reason})]
        self.assertEquals(g.end_game(room, socket), expected)
        self.assertEquals(g.rooms[room], [(False, 'socket1', None), (False, 'socket2', None), (False, socket, username)])

    def test_start_game(self):
        g = GameManager()
        room = 'ASDFGHJ'
        g.locks[room] = Lock()
        socket = 'socket'
        timer = '1'
        username = 'username'
        g.socket_to_room[socket] = room
        g.socket_to_username[socket] = username

        reason = 'AAAAAAA does not exist'
        expected = [(socket, {'type': 'Started', 'room_name': 'AAAAAAA', 'status': 'failed', 'reason': reason, 'spy': False, 'list_of_words': None, 'specific_word': None})]
        self.assertEquals(g.start_game(['AAAAAAA', '1'], socket), expected)

        g.rooms['AAAAAAA'] = []
        reason = 'User is not a part of the game'
        expected = [(socket, {'type': 'Started', 'room_name': 'AAAAAAA', 'status': 'failed', 'reason': reason, 'spy': False, 'list_of_words': None, 'specific_word': None})]
        self.assertEquals(g.start_game(['AAAAAAA', '1'], socket), expected)

        g.rooms[room] = [(False, None, 'user1'), (False, socket, username)]
        reason = room + ' has less than ' + str(g.min_people_to_start_game) + ' people. You need at least ' + str(g.min_people_to_start_game) + ' to start a game'
        expected = [(socket, {'type': 'Started', 'room_name': room, 'status': 'failed', 'reason': reason, 'spy': False, 'list_of_words': None, 'specific_word': None})]
        self.assertEquals(g.start_game([room, '1'], socket), expected)

        self.assertEquals(room in g.games_in_progress, False)

        data = [room, timer]
        g.rooms[room].append((False, None, 'user3'))
        g.start_game(data, socket)
        words, selected_word = g.words_for_room[room]
        self.assertEquals(len(words), g.num_words_in_game)
        self.assertEquals(selected_word in words, True)
        self.assertEquals(room in g.games_in_progress, True)