package com.lld.im.service.config;

import com.lld.im.common.config.AppConfig;
import com.lld.im.common.enums.ImUrlRouteWayEnum;
import com.lld.im.common.enums.RouteHashMethodEnum;
import com.lld.im.common.router.RouterHandle;
import com.lld.im.common.router.algorithm.consistenthash.AbstractConsistentHash;
import com.lld.im.common.router.algorithm.consistenthash.ConsistentHashHandle;
import com.lld.im.common.router.algorithm.consistenthash.TreeMapConsistentHash;
import com.lld.im.common.router.algorithm.loop.LoopHandle;
import com.lld.im.common.router.algorithm.random.RandomHandle;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.TreeMap;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Configuration
public class BeanConfig {

    @Autowired
    AppConfig appConfig;

    @Bean
    public ZkClient buildZkClient()
    {
        return new ZkClient(appConfig.getZkAddr(),appConfig.getZkConnectTimeOut());
    }
//    @Bean
//    public RouterHandle routerHandle()
//    {
//        return new LoopHandle();
//    }

//    @Bean
//    public RouterHandle routerHandle()
//    {
//        ConsistentHashHandle consistentHashHandle = new ConsistentHashHandle();
//        TreeMapConsistentHash TreeMap = new TreeMapConsistentHash();
//        consistentHashHandle.setHash(TreeMap);
//        return consistentHashHandle;
//    }

    @Bean
    public RouterHandle routerHandle() throws Exception{
        Integer imRouteWay = appConfig.getImRouteWay();
        String routeWay = "";
        ImUrlRouteWayEnum handler = ImUrlRouteWayEnum.getHandler(imRouteWay);
        assert handler != null;
        routeWay = handler.getClazz();
        RouterHandle routerHandle = (RouterHandle) Class.forName(routeWay).newInstance();
        if(handler == ImUrlRouteWayEnum.HASH)
        {
            Method setHash = Class.forName(routeWay).getMethod("setHash", AbstractConsistentHash.class);
            Integer consistentHashWay = appConfig.getConsistentHashWay();
            String hashWay = "";
            RouteHashMethodEnum hashMethodHandler = RouteHashMethodEnum.getHandler(consistentHashWay);
            assert hashMethodHandler != null;
            hashWay = hashMethodHandler.getClazz();
            AbstractConsistentHash abstractConsistentHash = (AbstractConsistentHash) Class.forName(hashWay).newInstance();
            setHash.invoke(routerHandle,abstractConsistentHash);
        }
        return routerHandle;
    }




}
