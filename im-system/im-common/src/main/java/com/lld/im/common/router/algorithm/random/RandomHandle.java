package com.lld.im.common.router.algorithm.random;

import com.lld.im.common.enums.UserErrorCode;
import com.lld.im.common.exception.ApplicationException;
import com.lld.im.common.router.RouterHandle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @作者：xie
 * @时间：2023/4/15 20:45
 */
public class RandomHandle implements RouterHandle {
    @Override
    public String routerService(List<String> values, String key) {
        int size = values.size();
        if(size == 0)
        {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        int i = ThreadLocalRandom.current().nextInt(size);
        return values.get(i);
    }
}
