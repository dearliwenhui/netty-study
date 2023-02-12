package cn.tuling.nettyadv;

import cn.tuling.nettyadv.busivo.User;
import cn.tuling.nettyadv.busivo.UserContact;

import java.util.Scanner;

/**
 * @author Mark老师   享学课堂 https://enjoy.ke.qq.com
 * 往期课程和VIP课程咨询 依娜老师  QQ：2133576719
 * 类说明：业务方如何调用Netty客户端演示
 */
public class BusiClient {
    public static void main(String[] args) throws Exception {
        NettyClient nettyClient = new NettyClient();
        new Thread(nettyClient).start();
        while(!nettyClient.isConnected()){
            synchronized (nettyClient){
                nettyClient.wait();
            }
        }
        System.out.println("网络通信已准备好，可以进行业务操作了........");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String msg = scanner.next();
            if (msg == null) {
                break;
            } else if ("q".equals(msg.toLowerCase())) {
                nettyClient.close();
                while(nettyClient.isConnected()){
                    synchronized (nettyClient){
                        nettyClient.wait();
                    }
                }
                scanner.close();
                System.exit(1);
            } else if("v".equals(msg.toLowerCase())){
                User user = new User();
                user.setAge(19);
                String userName = "ABCDEFG --->1";
                user.setUserName(userName);
                user.setId("No:1");
                user.setUserContact(
                        new UserContact(userName+"@xiangxue.com",
                                "133"));
                nettyClient.send(user);
            }
            else {
                nettyClient.send(msg);
            }
        }
    }
}
