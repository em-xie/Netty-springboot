package com.lld.im.common.router.algorithm.consistenthash;

import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public class TreeMapConsistentHash extends AbstractConsistentHash {

    private TreeMap<Long,String> treeMap = new TreeMap<>();

    private static final int NODE_SIZE = 2;

    @Override
    protected void add(long key, String value) {
        //虚拟节点
        //一个结点有3个值
        for (int i = 0; i < NODE_SIZE; i++) {
            treeMap.put(super.hash("node" + key +i),value);
        }
        treeMap.put(key,value);
    }

    
    @Override
    protected String getFirstNodeValue(String value) {
        //用户id
        Long hash = super.hash(value);
        SortedMap<Long, String> last = treeMap.tailMap(hash);
        if(!last.isEmpty()){
            return last.get(last.firstKey());
        }

        if (treeMap.size() == 0){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE) ;
        }

        return treeMap.firstEntry().getValue();
    }

    @Override
    protected void processBefore() {
        treeMap.clear();
    }
}
