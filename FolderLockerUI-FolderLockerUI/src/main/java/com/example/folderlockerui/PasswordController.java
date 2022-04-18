package com.example.folderlockerui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/* Used external API
 *   Secure Password Storage / Password Encryption Service:
 *  https://www.javacodegeeks.com/2012/05/secure-password-storage-donts-dos-and.html
 * */

public class PasswordController {
    PasswordEncryptionService encryptionService;
    String passwordText;
    byte[] encryptedPassword;
    Pair<byte[], byte[]> encryptedPassAndSalt;

    @FXML
    private PasswordField password;
    @FXML
    private AnchorPane anchorPane;

    @FXML
    private void Enter() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, ClassNotFoundException {
        passwordText = password.getText();

        if (passwordText == null || passwordText.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("Write the password first.");
            alert.showAndWait();

            return;
        }

        encryptionService = new PasswordEncryptionService();

        File passwordFile = new File("passwords");
        if (passwordFile.createNewFile()) {
            byte[] salt = encryptionService.generateSalt();
            encryptedPassword = encryptionService.getEncryptedPassword(passwordText, salt);
            encryptedPassAndSalt = new Pair<>(encryptedPassword, salt);

            writeApp(encryptedPassAndSalt);
        } else {
            encryptedPassAndSalt = readApp();

            if (encryptionService.authenticate(passwordText,
                                                encryptedPassAndSalt.getKey(),
                                                encryptedPassAndSalt.getValue())) {

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource(
                                "main-view.fxml"
                        )
                );

                Stage stage = new Stage(StageStyle.DECORATED);
                stage.setScene(
                        new Scene(loader.load())
                );

                MainController controller = loader.getController();
                controller.initData(passwordText);

                stage.show();

                anchorPane.getScene().getWindow().hide();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText(null);
                alert.setContentText("Wrong password.");
                alert.showAndWait();
            }
        }
    }

    public static void writeApp(Pair<byte[], byte[]> encryptedPassAndSalt) throws IOException {
        FileOutputStream fos = new FileOutputStream("passwords");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(encryptedPassAndSalt);

        oos.flush();
        fos.close();
        oos.close();
    }

    public static Pair<byte[], byte[]> readApp() throws IOException, ClassNotFoundException {

        FileInputStream fis = new FileInputStream("passwords");
        ObjectInputStream ois = new ObjectInputStream(fis);
        Pair<byte[], byte[]> encryptedPassAndSalt = (Pair<byte[], byte[]>)ois.readObject();

        fis.close();
        ois.close();
        return encryptedPassAndSalt;
    }
}
