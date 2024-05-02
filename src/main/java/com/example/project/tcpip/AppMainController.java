package com.example.project.tcpip;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.util.HashMap;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AppMainController implements Initializable {
    public Label label_temperature;
    public ProgressBar progressbar_light_value;
    public Label label_humidity;
    public Label title;
    public PasswordField passwordField;
    public Button button_add_card;
    public Button button_auto_led;
    public Button button_num1;
    public Button button_num2;
    public Button button_num3;
    public Button button_num4;
    public Button button_num5;
    public Button button_num6;
    public Button button_num7;
    public Button button_num8;
    public Button button_num9;
    public Button button_num0;
    public Button button_Enter;

    public Button button_pro;
    public Button button_add_pwd;
    public Button button_light_onoff;
    public Button button_siren_OnOff;


    private boolean state_of_button_red_led;
    private final Socket socket;

    HashMap<String, Boolean> keyValueMap = new HashMap<>();
    boolean add_card = false;

    String password = "";

    boolean pro_btn_state = false;
    String pro_password = "0000";
    boolean new_pro_password_state = false;
    boolean add_password_state = false;
    boolean new_add_password_state = false;

    boolean auto_light_onoff = true;

    boolean warning = false;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AppMainController()
    {
        this.state_of_button_red_led = false;
        this.socket = new Socket();
    }


    public void buttonOnClickedLEDAUTO() {
        if(this.socket.isConnected()){
            this.state_of_button_red_led ^= true;
            if(state_of_button_red_led) {
                button_auto_led.setText("Auto_Led_On");
                transport("Auto_Led_On\n");
            } else {
                button_auto_led.setText("Auto_Led_Off");
                transport("Auto_Led_Off\n");
            }
        }
    }

    public void buttonOnClickedAddCard() {
        add_card = true;
        title.setText("카드를 대십시오.");
        System.out.println("추가 버튼 클릭");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
//            this.socket.connect(new InetSocketAddress("192.168.0.13", Integer.parseInt("9999")));
            this.socket.connect(new InetSocketAddress("172.30.1.31", Integer.parseInt("9999")));
            this.received_data_from_server(new ActionEvent());
        } catch (IOException e) {
//            throw new RuntimeException(e);
            System.out.printf("%s\r\n", e.getMessage());
        }
    }

    private void received_data_from_server(ActionEvent event){
        Thread thread_of_receiving = new Thread(()->{
            while(true)
            {
                try {
                    final InputStream inputStream = this.socket.getInputStream();
                    byte[] bytes_data = new byte[512];
                    final int read_byte_count = inputStream.read(bytes_data);
                    final String serial_input_data =
                            new String(bytes_data, 0, read_byte_count, StandardCharsets.UTF_8);
                    final String[] parsings_data = serial_input_data.split(",");
                    if(parsings_data.length != 3) continue;
                    final String UID = parsings_data[0];
                    final int light_value = Integer.parseUnsignedInt(parsings_data[1]);
                    final int siren_count = Integer.parseUnsignedInt(parsings_data[2].trim());
                    final double changed_light_value = change_progress_value(light_value, 0, 900, 0.0, 1.0);
                    if(siren_count >= 5){
                        button_add_card.setDisable(true);
                        button_auto_led.setDisable(true);
                        button_pro.setDisable(true);
                        button_add_pwd.setDisable(true);
                        button_light_onoff.setDisable(true);
                        Platform.runLater(() -> {
                            title.setText("경보!! 경보!! 관리자 확인이 필요합니다!");
                            button_siren_OnOff.setText("사이렌 확인");
                        });
                    }else{
                        button_add_card.setDisable(false);
                        button_auto_led.setDisable(false);
                        button_pro.setDisable(false);
                        button_add_pwd.setDisable(false);
                        button_light_onoff.setDisable(false);
                    }
                    Platform.runLater(() -> {
                        progressbar_light_value.setProgress(changed_light_value);
                    });
                    if(add_card && !UID.isEmpty()) {
                        keyValueMap.put(UID, true);
                        System.out.println(UID + " is " + keyValueMap.get(UID));
                        transport("Add_card\n");
                        Platform.runLater(() -> {
                            title.setText("등록 되었습니다.");
                        });
                        scheduler.schedule(() -> {
                            // 3초 후에 실행될 코드...
                            transport("Led_Off\n");
                        }, 3, TimeUnit.SECONDS);
                        add_card = false;
                    }else if (!UID.isEmpty()){
                        if(keyValueMap.get(UID) != null && keyValueMap.get(UID)){
                            System.out.println("문 열림");
                            transport("Open_the_door\n");
                            Platform.runLater(() -> {
                                title.setText(UID + "문이 열렸습니다.");
                            });
                            scheduler.schedule(() -> {
                                // 3초 후에 실행될 코드...
                                Platform.runLater(() -> {
                                    title.setText("안녕하세요");
                                });
                            }, 3, TimeUnit.SECONDS);
                        } else if(keyValueMap.get(UID) == null || !keyValueMap.get(UID)){
                            System.out.println("카드 정보 없음");

                            transport("Not Correct!\n");

                            scheduler.schedule(() -> {
                                // 3초 후에 실행될 코드...
                                transport("Led_Off\n");
                            }, 3, TimeUnit.SECONDS);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread_of_receiving.start();
    }
    private void transport(String command){
        byte[] bytes_command = (command).getBytes(StandardCharsets.UTF_8);
        try {
            final var output_stream = this.socket.getOutputStream();
            output_stream.write(bytes_command);
            output_stream.flush();
        } catch (IOException e) {
            System.out.printf("%s\r\n", e.getMessage());
        }
    }

    private double change_progress_value(double x, double in_min, double in_max, double out_min, double out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }


    public void buttonOnClicked1() {
        password += "1";
        passwordField.setText(password);
    }
    public void buttonOnClicked2() {
        password += "2";
        passwordField.setText(password);
    }
    public void buttonOnClicked3() {
        password += "3";
        passwordField.setText(password);
    }
    public void buttonOnClicked4() {
        password += "4";
        passwordField.setText(password);
    }
    public void buttonOnClicked5() {
        password += "5";
        passwordField.setText(password);
    }
    public void buttonOnClicked6() {
        password += "6";
        passwordField.setText(password);
    }
    public void buttonOnClicked7() {
        password += "7";
        passwordField.setText(password);
    }
    public void buttonOnClicked8() {
        password += "8";
        passwordField.setText(password);
    }
    public void buttonOnClicked9() {
        password += "9";
        passwordField.setText(password);
    }
    public void buttonOnClicked0() {
        password += "0";
        passwordField.setText(password);
    }
    public void buttonOnClickedEnter() {
        if(pro_btn_state){                                                  //관리자 비밀번호 변경
            if(pro_password.equals(password)){
                new_pro_password_state = true;
                pro_btn_state = false;
                title.setText("변경할 관리자 비밀번호를 입력하세요.");
            }else {
                pro_btn_state = false;
                title.setText("관리자 비밀번호가 올바르지 않습니다.");

                scheduler.schedule(() -> {
                    // 3초 후에 실행될 코드...
                    Platform.runLater(() -> {
                        title.setText("안녕하세요");
                    });
                }, 3, TimeUnit.SECONDS);
            }
        }else if(new_pro_password_state){
            pro_password = password;
            title.setText("관리자 비밀번호 변경이 완료 되었습니다.");
            new_pro_password_state = false;
            scheduler.schedule(() -> {
                // 3초 후에 실행될 코드...
                Platform.runLater(() -> {
                    title.setText("안녕하세요");
                });
            }, 3, TimeUnit.SECONDS);
        }else if(add_password_state){                                         //비밀번호 추가
            if(pro_password.equals(password)){
                add_password_state = false;
                new_add_password_state = true;
                title.setText("새로 추가할 비밀번호를 입력하세요.");
            }else {
                add_password_state = false;
                title.setText("관리자 비밀번호가 올바르지 않습니다.");
                button_add_pwd.setText("비밀번호 추가");
                scheduler.schedule(() -> {
                    // 3초 후에 실행될 코드...
                    Platform.runLater(() -> {
                        title.setText("안녕하세요");
                    });
                }, 3, TimeUnit.SECONDS);
            }
        }else if(new_add_password_state){
            keyValueMap.put(password, true);
            new_add_password_state = false;
            title.setText("새로운 비밀번호가 추가 되었습니다.");
            button_add_pwd.setText("비밀번호 추가");

            scheduler.schedule(() -> {
                // 3초 후에 실행될 코드...
                Platform.runLater(() -> {
                    title.setText("안녕하세요");
                });
            }, 3, TimeUnit.SECONDS);
        }else if(warning){
            if(pro_password.equals(password)) {
                warning = false;
                transport("Siren_Stop\n");
                button_siren_OnOff.setText("사이렌 On");
                title.setText("경고가 해제 되었습니다.");

                scheduler.schedule(() -> {
                    // 3초 후에 실행될 코드...
                    Platform.runLater(() -> {
                        title.setText("안녕하세요");
                    });
                }, 3, TimeUnit.SECONDS);
            }else{
                title.setText("관리자 번호가 일치하지 않습니다.");
            }
        }else{
            if (keyValueMap.get(password) == null || !keyValueMap.get(password)) {
                title.setText("비밀번호가 일치하지 않습니다.");

                transport("Not Correct!\n");

                scheduler.schedule(() -> {
                    // 3초 후에 실행될 코드
                    transport("Led_Off\n");
                    Platform.runLater(() -> {
                        title.setText("안녕하세요");
                    });
                }, 3, TimeUnit.SECONDS);
            } else if (keyValueMap.get(password)) {
                title.setText("문이 열렸습니다.");

                transport("Open_the_door\n");

                scheduler.schedule(() -> {
                    // 3초 후에 실행될 코드...
                    Platform.runLater(() -> {
                        title.setText("안녕하세요");
                    });
                }, 5, TimeUnit.SECONDS);
            }
        }

        password = "";
        passwordField.setText(password);
    }

    public void buttonOnClickedPro() {
        pro_btn_state ^= true;
        if(pro_btn_state) title.setText("기존의 관리자 비밀번호를 입력하세요.");
        else title.setText("안녕하세요");
    }

    public void buttonOnClickedAddPwd() {
        add_password_state ^= true;
        if(add_password_state) {
            title.setText("관리자 번호를 입력하세요.");
            button_add_pwd.setText("비밀번호 추가 취소");
        }
        else {
            password = "";
            passwordField.setText(password);
            title.setText("안녕하세요");
            button_add_pwd.setText("비밀번호 추가");
        }
    }

    public void buttonOnClickedLightOnOff() {
        transport("Auto_Light_On_Off\n");
        auto_light_onoff ^= true;
        if(auto_light_onoff)
            button_light_onoff.setText("자동 현관 조명 On");
        else
            button_light_onoff.setText("자동 현관 조명 Off");
    }

    public void buttonOnClickedSirenOnOff() {
        if(button_siren_OnOff.getText().equals("사이렌 확인")){
            warning = true;
            title.setText("관리자 번호를 입력하세 경고를 해제하여 주십시오.");
        }
    }
}
