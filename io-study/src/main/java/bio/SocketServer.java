package bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * @description: bio演示代码
 * @param
 * @return
 * @author lld
 */
public class SocketServer {

    //1.0 服务端在处理完第一个客户端的所有事件之前，无法为其他客户端提供服务。
    //2.0 会产生大量空闲线程，浪费服务器资源。
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(9001);
        while (true) {
            System.out.println("等待连接..");
            //阻塞方法
            Socket clientSocket = serverSocket.accept();
            System.out.println("有客户端连接了..");
//            handler(clientSocket);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        handler(clientSocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private static void handler(Socket clientSocket) throws Exception {
//        while (true){
            byte[] bytes = new byte[1024];
            System.out.println("准备read..");
            //接收客户端的数据，阻塞方法，没有数据可读时就阻塞
            int read = clientSocket.getInputStream().read(bytes);
            System.out.println("read完毕。。");
            if (read != -1) {
//                int i = (int) (1 + Math.random() * (10 - 1 + 1));
//                System.out.println("收到客户端: " + Thread.currentThread().getName() + "收到了数据开始休眠");
//                Thread.sleep(i*1000L);
                System.out.println("接收到客户端的数据：" + new String(bytes, 0, read));
            }
//            clientSocket.getOutputStream().write("HelloClient".getBytes());
//            clientSocket.getOutputStream().flush();
//        }

    }


}