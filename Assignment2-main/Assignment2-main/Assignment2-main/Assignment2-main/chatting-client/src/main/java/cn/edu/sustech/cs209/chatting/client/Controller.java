package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.beans.value.ChangeListener;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class Controller implements Initializable {
  static String username;
  private Socket serverSocket;
  private DataInputStream dis;
  private int myPort=0;
  private DataOutputStream dos;
  ArrayList<String> currentUsers=new ArrayList<>();
  ArrayList<chatRoomThread> chatRooms=new ArrayList<>();
  boolean exit=true;
    @FXML
  public void handleExitButton() throws IOException {
    exit=false;
    dos.writeUTF("Exit-"+username);
    dis.close();
    dos.close();
    serverSocket.close();
    Platform.exit();
  }
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle){
        try {
            //Establish I/O Stream.
            serverSocket = new Socket("localhost", 10086);
            dos=new DataOutputStream(serverSocket.getOutputStream());
            dis=new DataInputStream(serverSocket.getInputStream());
            Thread announcement=new Thread(new announcement());
            announcement.start();
            //Wait for current username is refresh.
            while(!flag){
                Thread.sleep(100);
            }
            flag=false;
            while(true){
                //Establish the window to input username.
                Dialog<String> dialog = new TextInputDialog();
                dialog.setTitle("Login");
                dialog.setHeaderText(null);
                dialog.setContentText("Username:");
                Optional<String> input;
                input = dialog.showAndWait();
                boolean checkUsername=true;
                for (String string: currentUsers) {
                    if (Objects.equals(string, input.get())) {
                        checkUsername = false;
                        break;
                    }
                }
                //Check if the username is valid.
                if (input.isPresent() && !input.get().isEmpty()) {
                    if (!checkUsername) {
                        System.err.println("This user is online, please try again.");
                        continue;
                    }
                    username = input.get();
                    dos.writeUTF(username);
                    dos.flush();
                    System.out.println("Set the client successfully.");
                    break;
                } else {
                    System.out.println("The username is empty");
                    Platform.exit();
                    break;
                }
            }
        } catch (IOException e){
            System.err.println("Error");
            Platform.exit();
            System.exit(0);
        } catch (Exception e) {
            Platform.exit();
        }
    }

    @FXML
    public void createPrivateChat(){
        AtomicReference<String> user = new AtomicReference<>();
        ComboBox<String> userSel = new ComboBox<>();
        //Ask for current clients
        try{
            dos.writeUTF("Current username.");
            dos.flush();
            Thread.sleep(1000);
            while(!flag){
                Thread.sleep(100);
            }
            flag=false;
            currentUsers.remove(username);
            userSel.getItems().addAll(currentUsers);
        }catch (IOException | InterruptedException AF_user){
            System.out.println("Fail acquire.");
        }
        Stage stage = new Stage();
        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        try{
            stage.showAndWait();
            String friend=user.get();
            if (friend.length()==0)
                return;
            String chatName=username+"-"+friend;
            for (chatRoomThread thread:chatRooms){
                if (Objects.equals(thread.getChatName(),chatName)){
                    thread.stageShow();
                    return;
                }
            }
            dos.writeUTF("Group_Client-"+friend);
            //创建聊天窗口
            chatRoomThread chatRoom=new chatRoomThread(friend);
            chatRooms.add(chatRoom);
            chatRoom.start();
            System.out.println("Set up the window thread successfully!");
            dos.writeUTF("Me-20230426-"+username+"-"+friend+"-"+"I have set up a chatting, now let's chat!");
        }catch (Exception e){
            System.err.println("Chatting is quit.");
        }
    }
    /**
     * A new dialog should contain a multi-select list, showing all user's name.
     * You can select several users that will be joined in the group chat, including yourself.
     * <p>
     * The naming rule for group chats is similar to WeChat:
     * If there are > 3 users: display the first three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for example:
     * UserA, UserB, UserC... (10)
     * If there are <= 3 users: do not display the ellipsis, for example:
     * UserA, UserB (2)
     */
    @FXML
    public void createGroupChat() {
        AtomicReference<ArrayList<String>> selectedFriend=new AtomicReference<>();
        try{
            dos.writeUTF("Current username.");
            dos.flush();
            Thread.sleep(1000);
            while(!flag){
                Thread.sleep(100);
            }
            flag=false;
            currentUsers.remove(username);
        }catch (IOException | InterruptedException AF_user){
            System.out.println("Fail acquire.");
        }
        Stage stage=new Stage();
        ArrayList<String> selectedUsers=new ArrayList<>();
        stage.setTitle("User Selection");
        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.setPadding(new Insets(10));
        // 创建复选框的 ObservableList
        ObservableList<CheckBox> checkBoxes = FXCollections.observableArrayList();
        // 根据用户名数组动态创建复选框，并添加到 ObservableList 中
        for (String username : currentUsers) {
            CheckBox checkBox = new CheckBox(username);
            checkBoxes.add(checkBox);
            // 监听复选框的选中状态
            checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (newValue) {
                        selectedUsers.add(username); // 添加选中用户到 ArrayList
                    } else {
                        selectedUsers.remove(username); // 从 ArrayList 中移除取消选中的用户
                    }
                }
            });
            vBox.getChildren().add(checkBox);
        }
        Label label = new Label("Selected Users:");
        Button button = new Button("OK");
        button.setOnAction(event -> {
            selectedFriend.set(selectedUsers);
            stage.close();
        });
        vBox.getChildren().addAll(label, button);
        stage.setScene(new Scene(vBox, 300, 200));
        //注意show()和showAndWait()的区别。
        stage.showAndWait();
        try{
            ArrayList<String> friends = selectedFriend.get();
            friends.sort(Comparator.naturalOrder());
            String friend = String.join(",", friends);
            String chatName = username + "-" + friend;
            for (chatRoomThread thread : chatRooms) {
                if (Objects.equals(thread.getChatName(), chatName)) {
                    thread.stageShow();
                    return;
                }
            }
            dos.writeUTF("Group_Client-" + friend);
            dos.flush();
            //创建聊天窗口
            chatRoomThread chatRoom = new chatRoomThread(friend);
            chatRooms.add(chatRoom);
            chatRoom.start();
            System.out.println("Set up the window thread successfully!");
            dos.writeUTF("Me-20230426-"+username+"-"+friend+"-"+"I have set up a group, now let's chat!");
            dos.flush();
        } catch (Exception e){
            System.err.println("Chatting is quit.");
        }
    }

    /**
     * Sends the message to the <b>currently selected</b> chat.
     * <p>
     * Blank messages are not allowed.
     * After sending the message, you should clear the text input field.
     */
    @FXML
    private void setupChatWindow(Stage stage,String stageName,ListView<Message> chatList){
        System.out.println("Start set up Window.");
        MessageCellFactory cellFactory=new MessageCellFactory();
        chatList.setCellFactory(cellFactory);
        //用于确认换行符的存在
        Pattern p=Pattern.compile("\n");
        // Create TextArea and Button
        TextArea inputArea = new TextArea();
        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> {
            String data=inputArea.getText();
            inputArea.clear();
            if (data.trim().length()!=0||p.matcher(data).find()){
                String sentTO=stageName.split("-")[1];
                Message message = new Message(20230426L, username, sentTO, data);
                chatList.getItems().add(message);
                System.out.println("Message:" + data);
                try {
                    dos.writeUTF("Me-20230426-"+username+"-"+sentTO+"-"+data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Create HBox for inputArea and sendButton
        HBox inputAreaBox = new HBox(inputArea, sendButton);
        HBox.setHgrow(inputArea, Priority.ALWAYS);

        // Create SplitPane for chatContentList and inputAreaBox
        SplitPane splitPane2 = new SplitPane(inputAreaBox);

        // Create VBox for chatList and splitPane2
        VBox vBox = new VBox(chatList, splitPane2);
        VBox.setVgrow(splitPane2, Priority.ALWAYS);

        // Create main SplitPane for chatList and vBox
        SplitPane splitPane1 = new SplitPane(vBox);
        splitPane1.setDividerPositions(1);

        // Set a chatContentList on the left.
        ListView<String> chatContentList=new ListView<>();
        String[] friends=stageName.split("-")[1].split(",");
        chatContentList.getItems().addAll(friends);
        HBox allBox=new HBox(chatContentList,splitPane1);

        SplitPane splitPane=new SplitPane(allBox);
        splitPane.setDividerPositions(1);


        // Set focusTraversable to true
        splitPane.setFocusTraversable(true);

        // Create Scene
        Scene scene = new Scene(splitPane, 820, 600);
        stage.setScene(scene);
        stage.setTitle(stageName);
        stage.show();
    }
    private boolean flag=false;
    class announcement implements Runnable{
        boolean running=true;
        @Override
        public void run(){
            while(running){
                try {
                    String message=dis.readUTF();
                    if (message.startsWith("Fl-")){
                        //每次执行完一次操作后均有一个flag
                        Thread.sleep(1000);
                        flag = true;
                    }
                    if (message.startsWith("Me-")){
                        //处理收到的信息，格式: Me-timestamp-sentBy-friend1,friend2,friend3-data
                        String[] part=message.split("-");
                        Long timestamp=Long.parseLong(part[1]);
                        String sentBy=part[2];
                        String data=part[4];
                        String[] sentTo=part[3].split(",");
                        ArrayList<String> friends=new ArrayList<>();
                        for (String string:sentTo){
                            if (!Objects.equals(string,username))
                                friends.add(string);
                        }
                        friends.add(sentBy);
                        friends.sort(Comparator.naturalOrder());
                        String chatName=String.join(",",friends);
                        System.out.println(username+"-"+chatName);
                        boolean chatRoomExist=false;
                        chatRoomThread chatRoom=new chatRoomThread(chatName);
                        for (chatRoomThread thread:chatRooms){
                            if (Objects.equals(thread.getChatName(),username+"-"+chatName)){
                                chatRoom=thread;
                                chatRoomExist=true;
                                break;
                            }
                        }
                        if (!chatRoomExist){
                            chatRooms.add(chatRoom);
                            chatRoom.start();
                        }
                        Message chatting=new Message(timestamp,sentBy,username,data);
                        chatRoom.addChatList(chatting);
                        chatRoom.refresh();
                        String text = "Receive from: "+sentBy;
                        if (chatName.contains(","))
                            text+=" | Group: "+chatName;
                        JOptionPane.showMessageDialog(null, text);
                    }
                    if (message.startsWith("CU-")){
                        //生成在线用户信息，格式：CU-user1,user2,user3
                        String messageSub=message.substring(3);
                        ArrayList<String> usernames=new ArrayList<>();
                        Arrays.stream(messageSub.split(","))
                                .forEach(e->{
                                    System.out.println("user: "+e);
                                    usernames.add(e);
                                });
                        currentUsers=usernames;
                    }
                    if (message.startsWith("Po-")){
                        myPort=Integer.parseInt(message.substring(3));
                    }
                    if (Objects.equals(message,"Server close.")){
                        String text="The server is close.";
                        JOptionPane.showMessageDialog(null,text);
                        dos.writeUTF("OK");
                        dos.flush();
                        dis.close();
                        dos.close();
                        serverSocket.close();
                        Platform.exit();
                        System.exit(0);
                    }
                } catch (IOException | InterruptedException e) {
                    running=false;
                    try {
                        if (exit){
                            String text = "The server is close.";
                            JOptionPane.showMessageDialog(null, text);
                        }
                        dis.close();
                        dos.close();
                        serverSocket.close();
                        Platform.exit();
                        System.exit(0);
                    } catch (IOException ex) {
                        System.out.println("Client is out_Announcement");
                    }
                    System.err.println("Receive wrong.");
                }
            }
        }
    }
    private class chatRoomThread extends Thread{
        private String friend=null;
        private String chatName=null;
        public chatRoomThread(String friend){
            this.friend=friend;
            this.chatName=username+"-"+friend;
        }
        public String getChatName(){
            return chatName;
        }
        @FXML
        ListView<Message> chatList=new ListView<>();
        @FXML
        public void addChatList(Message message){
            try{
                chatList.getItems().add(message);
            }catch (Exception e){
                System.out.println("Receive the message.");
            }
        }
        @FXML
        public void refresh(){
                Platform.runLater(() -> chatList.refresh());
        }
        private Stage chatStage;
        public void stageShow(){
            chatStage.show();
        }
        @Override
        public void run(){
            Platform.runLater(()->{
                chatStage = new Stage();
                String stageName = chatName;
                setupChatWindow(chatStage, stageName, chatList);
            });
        }
    }
    /**
     * You may change the cell factory if you changed the design of {@code Message} model.
     * Hint: you may also define a cell factory for the chats displayed in the left panel, or simply override the toString method.
     */
    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {

                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }
                    Platform.runLater(()->setContentDisplay(ContentDisplay.GRAPHIC_ONLY));
                    Platform.runLater(()->setGraphic(wrapper));
                }
            };
        }
    }
}
