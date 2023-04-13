
import socket

s=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
s.connect(("127.0.0.1",9001))

for i in range(100):
    print(i)
    string = "hello1哈_";
    body = bytes(string, 'utf-8')
    s.sendall(body)



