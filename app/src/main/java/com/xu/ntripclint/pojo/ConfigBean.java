package com.xu.ntripclint.pojo;

public class ConfigBean {


    public String ntripServer;
    public int ntripServerPort;
    public String ntripServerMount;
    public String userName;
    public String password;
    public String uploadServer;
    public int uploadPort;
    public int uploadTime;

    public ConfigBean(String ntripServer, int ntripServerPort, String ntripServerMount, String userName, String password, String uploadServer, int uploadTime) {
        this.ntripServer = ntripServer;
        this.ntripServerPort = ntripServerPort;
        this.ntripServerMount = ntripServerMount;
        this.userName = userName;
        this.password = password;
        this.uploadServer = uploadServer;
        this.uploadTime = uploadTime;
    }

    public ConfigBean(String ntripServer, int ntripServerPort, String ntripServerMount, String userName, String password, String uploadServer, int uploadPort, int uploadTime) {
        this.ntripServer = ntripServer;
        this.ntripServerPort = ntripServerPort;
        this.ntripServerMount = ntripServerMount;
        this.userName = userName;
        this.password = password;
        this.uploadServer = uploadServer;
        this.uploadPort = uploadPort;
        this.uploadTime = uploadTime;
    }
}
