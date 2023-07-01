import socket
import threading


def get_length_from_head_data(head_data):
    if not len(head_data) == 8:
        return
    ch1 = head_data[4] & 0x00FF
    ch2 = head_data[5] & 0x00FF
    ch3 = head_data[6] & 0x00FF
    ch4 = head_data[7] & 0x00FF
    return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0)


def get_type_from_head_data(head_data):
    if not len(head_data) == 8:
        return
    ch1 = head_data[0] & 0x00FF
    ch2 = head_data[1] & 0x00FF
    ch3 = head_data[2] & 0x00FF
    ch4 = head_data[3] & 0x00FF
    return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0)


def wrapper_data(data_type, body_data):
    print("data_type:", data_type)
    print("body_data len:", len(body_data))

    type_bytes = data_type.to_bytes(4, 'big')
    print("type_bytes:", type_bytes)
    body_len = len(body_data)

    body_len_bytes = body_len.to_bytes(4, 'big')
    print("body_len_bytes:", body_len_bytes)

    head_data = type_bytes + body_len_bytes
    print("head_data:", head_data)

    data = head_data + body_data
    return data


class TcpServer:
    def __init__(self):
        self.connected_listener = None
        self.server_threading = None
        self.clientSocket = None
        self.receive_listener = None
        ip = '192.168.31.215'  # 设置本机在局域网内ip
        self.port = 2233  # 设置端口
        self.HEAD_LEN = 8
        self.tcpServerSocket = socket.socket()  # 创建socket对象
        self.tcpServerSocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)  # 让端口可以复用
        self.tcpServerSocket.bind((ip, self.port))  # 将地址与套接字绑定，且套接字要求是从未被绑定过的
        self.tcpServerSocket.listen(5)  # 代办事件中排队等待connect的最大数目

    def set_receive_listener(self, receive_listener):
        self.receive_listener = receive_listener

    def server(self):
        while True:
            print("等待连接")
            self.clientSocket, addr = self.tcpServerSocket.accept()
            print('连接地址：', addr)
            if self.connected_listener:
                self.connected_listener()
            while True:
                try:
                    head_data = self.clientSocket.recv(self.HEAD_LEN)
                    if not len(head_data) == 8:
                        print("bad package!head_data len:", head_data)
                        self.restart()
                        return
                    body_len = get_length_from_head_data(head_data)
                    body_data = self.clientSocket.recv(body_len)
                    if not body_len == len(body_data):
                        print("bad package!body_len:", body_len)
                        self.restart()
                        return
                    data_type = get_type_from_head_data(head_data)

                    if data_type == 1:  # test/json data
                        text = body_data.decode()
                        print(text)
                        if not text:
                            break
                        if self.receive_listener:
                            self.receive_listener(text)
                    elif data_type == 2:  # image data
                        pass

                except ConnectionResetError:
                    print("ConnectionResetError!")
                    self.restart()
                    return
            self.clientSocket.close()  # 关闭连接
            self.tcpServerSocket.close()

    def start(self):
        self.server_threading = threading.Thread(target=self.server, args=())
        self.server_threading.start()

    def restart(self):
        self.start()

    def send_data(self, data):
        try:
            self.clientSocket.send(data)
        except ConnectionResetError:
            print("ConnectionResetError!")
            self.restart()

    def send_img(self, bytes_data):
        data = wrapper_data(2, bytes_data)
        self.send_data(data)

    def send_text(self, text):
        data = wrapper_data(1, text.encode())
        self.send_data(data)
