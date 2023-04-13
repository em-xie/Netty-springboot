#-*- coding: UTF-8 -*-
import socket,os,struct
s=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
s.connect(("127.0.0.1",9001))

filepath = "D://natapp.zip"
if os.path.isfile(filepath):
    
    filename = os.path.basename(filepath).encode('utf-8')

    # 请求传输文件
    command = 1
    
    body_len = len(filename)
    fileNameData = bytes(filename)
    i = body_len.to_bytes(4, byteorder='big')
    c = command.to_bytes(4, byteorder='big')

    s.sendall(c + i + fileNameData) 

    fo = open(filepath,'rb')
    while True:
      command = 2;
      c = command.to_bytes(4, byteorder='big')
      filedata = fo.read(1024)
      print(len(filedata))
      b = len(filedata).to_bytes(4, byteorder='big')
      if not filedata:
        break
      s.sendall(c + i + fileNameData + b + filedata)
    fo.close()
    #s.close()
else:
    print(False)