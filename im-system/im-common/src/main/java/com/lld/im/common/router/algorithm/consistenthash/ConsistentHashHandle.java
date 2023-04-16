package com.lld.im.common.router.algorithm.consistenthash;

import com.lld.im.common.router.RouterHandle;

import java.util.List;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
public class ConsistentHashHandle implements RouterHandle {

    //TreeMap
    private AbstractConsistentHash hash;

    public void setHash(AbstractConsistentHash hash) {
        this.hash = hash;
    }



    @Override
    public String routerService(List<String> values, String key) {
        return hash.process(values,key);
    }
}
