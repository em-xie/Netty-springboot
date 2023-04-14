package com.lld.im.codec.proto;

import lombok.Data;

/**
 * @作者：xie
 * @时间：2023/4/14 13:13
 */
@Data
public class Message {

    private MessageHeader messageHeader;

    private Object messagePack;

    @Override
    public String toString() {
        return "Message{" +
                "messageHeader=" + messageHeader +
                ", messagePack=" + messagePack +
                '}';
    }
}
