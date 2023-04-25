package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Main extends Application {

    public static void main(String[] args) {
        launch(); //从这里开始执行Controller的步骤
    }
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main.fxml"));
        stage.setScene(new Scene(fxmlLoader.load()));
        stage.setTitle(Controller.username);
        //禁用关闭功能，另外设置一个exit
        stage.setOnCloseRequest(Event::consume);
        stage.show();
    }
}
