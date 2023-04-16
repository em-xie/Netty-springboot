package com.lld.im.common.router.algorithm.loop;

import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.common.router.RouterHandle;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @作者：xie
 * @时间：2023/4/16 8:21
 */
public class LoopHandle implements RouterHandle {
    private final AtomicLong index = new AtomicLong();
    @Override
    public String routerService(List<String> values, String key) {

        int size = values.size();
        if(size == 0)
        {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        long l = index.incrementAndGet() % size;
        if(l < 0)
        {
            l = 0L;
        }
        return values.get((int) l);
    }
}
