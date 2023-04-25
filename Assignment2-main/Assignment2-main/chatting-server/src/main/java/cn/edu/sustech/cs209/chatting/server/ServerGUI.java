package cn.edu.sustech.cs209.chatting.server;


import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ServerGUI {
    private JFrame frame;
    private JLabel label;

    public ServerGUI() {
        frame = new JFrame("Server Status");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        label = new JLabel("Connected Clients: 0");
        label.setHorizontalAlignment(JLabel.CENTER); //水平对齐
        label.setFont(new Font("Arial", Font.PLAIN, 20)); //待学 Frame Label Font

        frame.add(label);
        frame.setVisible(true);
        //自定义退出按钮
    }

    public void updateClientCount(ArrayList<String> usernames) {
        StringBuilder stringBuilder=new StringBuilder();
        for (String username:usernames){
            stringBuilder.append(username).append("<br>");
        }
        String labelText=stringBuilder.toString();
        label.setText("<html>"+labelText+"</html>");
    }
}

