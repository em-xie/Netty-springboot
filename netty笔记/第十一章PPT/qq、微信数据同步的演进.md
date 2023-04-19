### 数据同步

![image-20230419083037920](E:\java2\netty-springboot\netty笔记\第十一章PPT\image-20230419083037920.png)

![image-20230419083334000](E:\java2\netty-springboot\netty笔记\第十一章PPT\image-20230419083334000.png)

![image-20230419083515534](E:\java2\netty-springboot\netty笔记\第十一章PPT\image-20230419083515534.png)

![image-20230419084027825](E:\java2\netty-springboot\netty笔记\第十一章PPT\image-20230419084027825.png)

### 关系链数据全量拉起改为增量拉去-加上版本号

```
package com.lld.im.service.utils;

import com.lld.im.common.constant.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Service
public class WriteUserSeq {

    //redis
    //uid friend 10
    //    group 12
    //    conversation 123
    @Autowired
    RedisTemplate redisTemplate;

    public void writeUserSeq(Integer appId,String userId,String type,Long seq){
        String key = appId + ":" + Constants.RedisConstants.SeqPrefix + ":" + userId;
        redisTemplate.opsForHash().put(key,type,seq);
    }

}

```

doAddFriend

```
  long seq = 0L;
```

```
 if(fromItem == null){
            //走添加逻辑。
            fromItem = new ImFriendShipEntity();
            seq = redisSeq.doGetSeq(appId+":"+Constants.SeqConstants.Friendship);
            fromItem.setAppId(appId);
            fromItem.setFromId(fromId);
            fromItem.setFriendSequence(seq);
```

```

                seq = redisSeq.doGetSeq(appId+":"+Constants.SeqConstants.Friendship);
                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());

                int result = imFriendShipMapper.update(update, query);
```

就是在每一个添加更新删除操作前添加seq，然后wirte seq 然后再添加进去，





### 实现同步增量接口

![image-20230419095901068](E:\java2\netty-springboot\netty笔记\第十一章PPT\image-20230419095901068.png)

### 好友增量接口

```
    @RequestMapping("/syncFriendshipList")
    public ResponseVO syncFriendshipList(@RequestBody @Validated
                                         SyncReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipService.syncFriendshipList(req);
    }
```

```
  @Override
    public ResponseVO syncFriendshipList(SyncReq req) {

        if(req.getMaxLimit() > 100){
            req.setMaxLimit(100);
        }

        SyncResp<ImFriendShipEntity> resp = new SyncResp<>();
        //seq > req.getseq limit maxLimit
        QueryWrapper<ImFriendShipEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("from_id",req.getOperater());
        queryWrapper.gt("friend_sequence",req.getLastSequence());
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.last(" limit " + req.getMaxLimit());
        queryWrapper.orderByAsc("friend_sequence");
        List<ImFriendShipEntity> list = imFriendShipMapper.selectList(queryWrapper);

        if(!CollectionUtils.isEmpty(list)){
            ImFriendShipEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            //设置最大seq
            Long friendShipMaxSeq = imFriendShipMapper.getFriendShipMaxSeq(req.getAppId(), req.getOperater());
            resp.setMaxSequence(friendShipMaxSeq);
            //设置是否拉取完毕
            resp.setCompleted(maxSeqEntity.getFriendSequence() >= friendShipMaxSeq);
            return ResponseVO.successResponse(resp);
        }

        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }

```

```
    @Select(" select max(friend_sequence) from im_friendship where app_id = #{appId} AND from_id = #{userId} ")
    Long getFriendShipMaxSeq(Integer appId,String userId);
```

### 会话同步增量

```
@RequestMapping("/syncConversationList")
    public ResponseVO syncFriendShipList(@RequestBody @Validated SyncReq req, Integer appId)  {
        req.setAppId(appId);
        return conversationService.syncConversationSet(req);
    }
```

```
 public ResponseVO syncConversationSet(SyncReq req) {
        if(req.getMaxLimit() > 100){
            req.setMaxLimit(100);
        }

        SyncResp<ImConversationSetEntity> resp = new SyncResp<>();
        //seq > req.getseq limit maxLimit
        QueryWrapper<ImConversationSetEntity> queryWrapper =
                new QueryWrapper<>();
        queryWrapper.eq("from_id",req.getOperater());
        queryWrapper.gt("sequence",req.getLastSequence());
        queryWrapper.eq("app_id",req.getAppId());
        queryWrapper.last(" limit " + req.getMaxLimit());
        queryWrapper.orderByAsc("sequence");
        List<ImConversationSetEntity> list = imConversationSetMapper
                .selectList(queryWrapper);

        if(!CollectionUtils.isEmpty(list)){
            ImConversationSetEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            //设置最大seq
            Long friendShipMaxSeq = imConversationSetMapper.geConversationSetMaxSeq(req.getAppId(), req.getOperater());
            resp.setMaxSequence(friendShipMaxSeq);
            //设置是否拉取完毕
            resp.setCompleted(maxSeqEntity.getSequence() >= friendShipMaxSeq);
            return ResponseVO.successResponse(resp);
        }

        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);

    }
```

### 群组同步增量

```
    @RequestMapping("/syncJoinedGroup")
    public ResponseVO syncJoinedGroup(@RequestBody @Validated SyncReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        return groupService.syncJoinedGroupList(req);
    }
```

```
    @Override
    public ResponseVO syncJoinedGroupList(SyncReq req) {
        if(req.getMaxLimit() > 100){
            req.setMaxLimit(100);
        }

        SyncResp<ImGroupEntity> resp = new SyncResp<>();

        ResponseVO<Collection<String>> memberJoinedGroup = groupMemberService.syncMemberJoinedGroup(req.getOperater(), req.getAppId());
        if(memberJoinedGroup.isOk()){

            Collection<String> data = memberJoinedGroup.getData();
            QueryWrapper<ImGroupEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("app_id",req.getAppId());
            queryWrapper.in("group_id",data);
            queryWrapper.gt("sequence",req.getLastSequence());
            queryWrapper.last(" limit " + req.getMaxLimit());
            queryWrapper.orderByAsc("sequence");

            List<ImGroupEntity> list = imGroupDataMapper.selectList(queryWrapper);

            if(!CollectionUtils.isEmpty(list)){
                ImGroupEntity maxSeqEntity
                        = list.get(list.size() - 1);
                resp.setDataList(list);
                //设置最大seq
                Long maxSeq =
                        imGroupDataMapper.getGroupMaxSeq(data, req.getAppId());
                resp.setMaxSequence(maxSeq);
                //设置是否拉取完毕
                resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
                return ResponseVO.successResponse(resp);
            }

        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }
```

```
    @Override
    public ResponseVO<Collection<String>> syncMemberJoinedGroup(String operater, Integer appId) {
        return ResponseVO.successResponse(imGroupMemberMapper.syncJoinedGroupId(appId,operater,GroupMemberRoleEnum.LEAVE.getCode()));
    }
```

```
    @Select(" <script> " +
            " select max(sequence) from im_group where app_id = #{appId} and group_id in " +
            "<foreach collection='groupId' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " </script> ")
    Long getGroupMaxSeq(Collection<String> groupId, Integer appId);
```

### 获取用户sequence接口

```
    @RequestMapping("/getUserSequence")
    public ResponseVO getUserSequence(@RequestBody @Validated
                                      GetUserSequenceReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserSequence(req);
    }
```

```
    @Override
    public ResponseVO getUserSequence(GetUserSequenceReq req) {
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(req.getAppId() + ":" + Constants.RedisConstants.SeqPrefix + ":" + req.getUserId());
        Long groupSeq = imGroupService.getUserGroupMaxSeq(req.getUserId(),req.getAppId());
        map.put(Constants.SeqConstants.Group,groupSeq);
        return ResponseVO.successResponse(map);
    }
```

```
    @Override
    public Long getUserGroupMaxSeq(String userId, Integer appId) {

        ResponseVO<Collection<String>> memberJoinedGroup = groupMemberService.syncMemberJoinedGroup(userId, appId);
        if(!memberJoinedGroup.isOk()){
            throw new ApplicationException(500,"");
        }
        Long maxSeq =
                imGroupDataMapper.getGroupMaxSeq(memberJoinedGroup.getData(),
                        appId);
        return maxSeq;
    }
```

### 实现增量拉取离线消息

```
    @RequestMapping("/syncOfflineMessage")
    public ResponseVO syncOfflineMessage(@RequestBody
                                             @Validated SyncReq req, Integer appId)  {
        req.setAppId(appId);
        return messageSyncService.syncOfflineMessage(req);
    }
```

```
 public ResponseVO syncOfflineMessage(SyncReq req) {

        SyncResp<OfflineMessageContent> resp = new SyncResp<>();

        String key = req.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + req.getOperater();
        //获取最大的seq
        Long maxSeq = 0L;
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        Set set = zSetOperations.reverseRangeWithScores(key, 0, 0);
        if(!CollectionUtils.isEmpty(set)){
            List list = new ArrayList(set);
            DefaultTypedTuple o = (DefaultTypedTuple) list.get(0);
            maxSeq = o.getScore().longValue();
        }

        List<OfflineMessageContent> respList = new ArrayList<>();
        resp.setMaxSequence(maxSeq);

        Set<ZSetOperations.TypedTuple> querySet = zSetOperations.rangeByScoreWithScores(key,
                req.getLastSequence(), maxSeq, 0, req.getMaxLimit());
        for (ZSetOperations.TypedTuple<String> typedTuple : querySet) {
            String value = typedTuple.getValue();
            OfflineMessageContent offlineMessageContent = JSONObject.parseObject(value, OfflineMessageContent.class);
            respList.add(offlineMessageContent);
        }
        resp.setDataList(respList);

        if(!CollectionUtils.isEmpty(respList)){
            OfflineMessageContent offlineMessageContent = respList.get(respList.size() - 1);
            resp.setCompleted(maxSeq <= offlineMessageContent.getMessageKey());
        }

        return ResponseVO.successResponse(resp);
    }
```

### 客户端数据库sqlite-uniapp-nview





增量优化

![image-20230419124625130](E:\java2\netty-springboot\netty笔记\第十一章PPT\image-20230419124625130.png)





