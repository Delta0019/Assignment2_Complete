package cn.edu.sustech.cs209.chatting.server;
import javax.naming.Name;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class Server {
    private static ServerGUI serverGUI;
    static ServerSocket serverSocket;
    static ArrayList<String> currentUsernames=new ArrayList<>();
    static String folderPath="C:\\Users\\23763\\Desktop\\java\\assignment2\\chat_history\\";
    static HashMap<String,Socket> socketHashMap=new HashMap<>();

    public Server() {
        serverGUI = new ServerGUI();
    }

    public void start() {
        int port = 10086; // 设置服务器端口号
        try {
            serverSocket = new ServerSocket(port); // 创建ServerSocket对象并绑定端口
            System.out.println("服务器已启动,监听接口："+port);
            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                Thread clientThread=new Thread(new ClientThreadController(clientSocket));
                clientThread.start();
                System.out.println("客户端已连接：" + clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                serverSocket.close();
                System.out.println("Server is out.");
            }catch (Exception e){

            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    static class ClientThreadController implements Runnable {
        private Socket clientSocket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private boolean running=true;
        public ClientThreadController(Socket clientSocket) throws IOException{
            this.clientSocket=clientSocket;
            dis=new DataInputStream(clientSocket.getInputStream());
            dos=new DataOutputStream(clientSocket.getOutputStream());
            //DataInputStream并不关心数据的结构或语义，仅是将读入的数据转换为字节型并存储
            //因此，输入和输出应该提前约定，使得读取与发送逻辑一致
        }
        @Override
        public void run(){
            try{
                //Sent info to client.
                String currentUser="CU-"+String.join(",",currentUsernames);
                dos.writeUTF(currentUser);
                dos.flush();
                dos.writeUTF("Fl-flag");
                dos.flush();

                // 在这里可以对客户端进行初始化处理，例如读取、写入数据等
                String username=dis.readUTF();
                currentUsernames.add(username);
                socketHashMap.put(username,clientSocket);
                System.out.println("用户添加成功！");
                dos.writeUTF("Po-"+clientSocket.getPort());
                serverGUI.updateClientCount(currentUsernames); // 更新客户端数量的显示

                //在这里对客户端进行轮询操作
                while(running){
                    try {
                        String data= dis.readUTF();
                        System.out.println("Receive information from client: "+data);
                        if (Objects.equals(data,"Current username.")){
                            String string="CU-"+String.join(",",currentUsernames);
                            System.out.println(string);
                            dos.writeUTF(string);
                            dos.flush();
                            dos.writeUTF("Fl-flag");
                            dos.flush();
                        }
                        if (data.startsWith("Group_Client")){
                            String friend=data.split("-")[1];
                            String fileName="PrivateChat-"+username+"-"+friend;
                            System.out.println(fileName);
                            File file=new File(folderPath+fileName);
                            if (!file.exists()){
                                try{
                                    boolean fileCreated=file.createNewFile();
                                    if (fileCreated) System.out.println("File of chat history is created!");
                                    else System.out.println("File fail.");
                                } catch (Exception e){
                                    System.err.println("File fail.");
                                }
                            }
                        }
                        //Broadcast to all friends.
                        if (data.startsWith("Me-")){
                            String[] strings=data.split("-");
                            String[] sentTos=strings[3].split(",");
                            for (String sentTo:sentTos){
                                if (socketHashMap.get(sentTo)!=null) {
                                    Socket targetSocket = socketHashMap.get(sentTo);
                                    DataOutputStream sender = new DataOutputStream(targetSocket.getOutputStream());
                                    sender.writeUTF(data);
                                    System.out.println("Send to " + sentTo + ": " + data);
                                }
                            }
                        }
                        if (data.startsWith("Exit-")){
                            String clientExit=data.split("-")[1];
                            //remove方法返回该hashmap的值，可以根据需要进行处理。
                            socketHashMap.put(clientExit,null);
                            currentUsernames.remove(clientExit);
                            serverGUI.updateClientCount(currentUsernames);
                        }
                    } catch(Exception clientOut){
                        System.out.println("Client is outline.");
                        running=false;
                    }
                }
            } catch (IOException e) {
                try{
                    dos.close();
                    dis.close();
                    clientSocket.close(); // 关闭客户端连接
                } catch (IOException exception){
                    System.err.println("The client is out.");
                }
            }
        }
    }
}
