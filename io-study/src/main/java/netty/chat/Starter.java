package netty.chat;

import netty.chat.server.DiscardServer;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public class Starter {

    // line
    // header
    // body

    public static void main(String[] args) throws Exception {
        new DiscardServer(9001).run();
    }
}
