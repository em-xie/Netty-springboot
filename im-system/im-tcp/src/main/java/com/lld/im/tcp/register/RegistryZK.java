package com.lld.im.tcp.register;

import com.lld.im.codec.config.BootstrapConfig;
import com.lld.im.common.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @作者：xie
 * @时间：2023/4/15 14:43
 */
public class RegistryZK implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(RegistryZK.class);

    private final ZKit zKit;

    private final String ip;

    private final BootstrapConfig.TcpConfig tcpConfig;

    public RegistryZK(ZKit zKit, String ip, BootstrapConfig.TcpConfig tcpConfig) {
        this.zKit = zKit;
        this.ip = ip;
        this.tcpConfig = tcpConfig;
    }

    @Override
    public void run() {
        zKit.createRootNode();
        String tcpPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootTcp + "/" + ip + ":" + tcpConfig.getTcpPort();
        zKit.createNode(tcpPath);
        logger.info("Registry zookeeper tcpPath success, msg=[{}]", tcpPath);

        String webPath =
                Constants.ImCoreZkRoot + Constants.ImCoreZkRootWeb + "/" + ip + ":" + tcpConfig.getWebSocketPort();
        zKit.createNode(webPath);
        logger.info("Registry zookeeper webPath success, msg=[{}]", tcpPath);

    }
}
