package com.lld.im.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.lld.im.common.constant.Constants;
import com.lld.im.common.enums.DelFlagEnum;
import com.lld.im.common.model.message.DoStoreGroupMessageDto;
import com.lld.im.common.model.message.GroupChatMessageContent;
import com.lld.im.common.model.message.ImMessageBody;
import com.lld.im.common.model.message.MessageContent;
import com.lld.im.service.group.dao.ImGroupMessageHistoryEntity;
import com.lld.im.service.group.dao.mapper.ImGroupMessageHistoryMapper;
import com.lld.im.service.message.dao.ImMessageBodyEntity;
import com.lld.im.service.message.dao.ImMessageHistoryEntity;
import com.lld.im.service.message.dao.mapper.ImMessageBodyMapper;
import com.lld.im.service.message.dao.mapper.ImMessageHistoryMapper;
import com.lld.im.service.utils.SnowflakeIdWorker;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @作者：xie
 * @时间：2023/4/17 14:18
 */
@Service
public class MessageStoreService {
    @Autowired
    ImMessageHistoryMapper imMessageHistoryMapper;

    @Autowired
    ImMessageBodyMapper imMessageBodyMapper;

    @Autowired
    ImGroupMessageHistoryMapper imGroupMessageHistoryMapper;

    @Autowired
    SnowflakeIdWorker snowflakeIdWorker;

    @Transactional
    public void storeP2PMessage(MessageContent messageContent){

        //messageContent 转化成 messageBody
        ImMessageBodyEntity imMessageBodyEntity = extractMessageBody(messageContent);
        //插入messageBody
        imMessageBodyMapper.insert(imMessageBodyEntity);
//        //转化成MessageHistory
        List<ImMessageHistoryEntity> imMessageHistoryEntities = extractToP2PMessageHistory(messageContent, imMessageBodyEntity);
//        //批量插入
        imMessageHistoryMapper.insertBatchSomeColumn(imMessageHistoryEntities);
        messageContent.setMessageKey(imMessageBodyEntity.getMessageKey());

//        //messageContent 转化成 messageBody
//        ImMessageBody imMessageBody = extractMessageBody(messageContent);
        //插入messageBody

        //转化成MessageHistory
        //extractToP2PMessageHistory(messageContent,imMessageBody);

        //批量插入
//        imMessageHistoryMapper.insertBatchSomeColumn();
//         imMessageBody.setMessageKey(messageContent.getMessageKey());

    }
    public ImMessageBodyEntity extractMessageBody(MessageContent messageContent){
        ImMessageBodyEntity messageBody = new ImMessageBodyEntity();

        messageBody.setAppId(messageContent.getAppId());
        messageBody.setMessageKey(snowflakeIdWorker.nextId());
        messageBody.setCreateTime(System.currentTimeMillis());
        messageBody.setSecurityKey("");
        messageBody.setExtra(messageContent.getExtra());
        messageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
        messageBody.setMessageTime(messageContent.getMessageTime());
        messageBody.setMessageBody(messageContent.getMessageBody());
        return messageBody;
    }

//    public ImMessageBody extractMessageBody(MessageContent messageContent){
//        ImMessageBody messageBody = new ImMessageBody();
//
//        messageBody.setAppId(messageContent.getAppId());
//        messageBody.setMessageKey(snowflakeIdWorker.nextId());
//        messageBody.setCreateTime(System.currentTimeMillis());
//        messageBody.setSecurityKey("");
//        messageBody.setExtra(messageContent.getExtra());
//        messageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
//        messageBody.setMessageTime(messageContent.getMessageTime());
//        messageBody.setMessageBody(messageContent.getMessageBody());
//        return messageBody;
//    }


    public List<ImMessageHistoryEntity> extractToP2PMessageHistory(MessageContent messageContent,
                                                                   ImMessageBodyEntity imMessageBodyEntity){
        List<ImMessageHistoryEntity> list = new ArrayList<>();
        ImMessageHistoryEntity fromHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent,fromHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        fromHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());

        ImMessageHistoryEntity toHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent,toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());

        list.add(fromHistory);
        list.add(toHistory);
        return list;
    }


    @Transactional
    public void storeGroupMessage(GroupChatMessageContent messageContent){
        ImMessageBodyEntity imMessageBody = extractMessageBody(messageContent);
        imMessageBodyMapper.insert(imMessageBody);
        ImGroupMessageHistoryEntity imGroupMessageHistoryEntity = extractToGroupMessageHistory(messageContent,imMessageBody);
        imGroupMessageHistoryMapper.insert(imGroupMessageHistoryEntity);
        messageContent.setMessageKey(imMessageBody.getMessageKey());
        //DoStoreGroupMessageDto dto = new DoStoreGroupMessageDto();
        //dto.setMessageBody(imMessageBody);
        //dto.setGroupChatMessageContent(messageContent);
//        rabbitTemplate.convertAndSend(Constan ts.RabbitConstants.StoreGroupMessage,
//                "",
//                JSONObject.toJSONString(dto));
       // messageContent.setMessageKey(imMessageBody.getMessageKey());
    }

    private ImGroupMessageHistoryEntity extractToGroupMessageHistory(GroupChatMessageContent
                                                                             messageContent ,ImMessageBodyEntity messageBodyEntity){
        ImGroupMessageHistoryEntity result = new ImGroupMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent,result);
        result.setGroupId(messageContent.getGroupId());
        result.setMessageKey(messageBodyEntity.getMessageKey());
        result.setCreateTime(System.currentTimeMillis());
        return result;
    }

}
