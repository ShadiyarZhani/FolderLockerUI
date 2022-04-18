package com.example.folderlockerui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.ResourceBundle;

/* Used external API
*   Zip protected files:
*   https://www.baeldung.com/java-password-protected-zip-unzip
*   Asymmetric Cryptography
*   https://mkyong.com/java/java-asymmetric-cryptography-example
*   Crypto Utils
*   https://www.codejava.net/coding/file-encryption-and-decryption-simple-example
* */

public class MainController implements Initializable {
    // <filepath, password>
    HashMap<String, String> files = new HashMap<>();
    Boolean encrypted = false;
    String filePath;
    String passText;
    String encryptionPassword;
    File file;
    ObservableList<String> observableFiles;
    boolean fileCreated = false;
    boolean fileDeleted = false;

    @FXML
    private Label singleFileLab;
    @FXML
    private AnchorPane anchorPane;
    @FXML
    private PasswordField password;
    @FXML
    private TextField newPassword;
    @FXML
    private Label outputLabel;
    @FXML
    private ListView<String> fileListView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) { }

    void initData(String password) {
        encryptionPassword = password;

        File yourFile = new File("file.txt");
        try {
            if (yourFile.createNewFile()) {
                writeApp(files, encryptionPassword);
            }

            files = readApp(encryptionPassword);
        } catch (IOException | ClassNotFoundException | CryptoException e) {
            e.printStackTrace();
        }

        fileListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        observableFiles = FXCollections.observableArrayList(files.keySet());
        fileListView.setItems(observableFiles);

        fileListView.getSelectionModel().selectedItemProperty().addListener((observableValue, s, t1) -> {
            if (t1 == null)
                return;

            filePath = fileListView.getSelectionModel().getSelectedItem();
            file = new File(filePath);

            singleFileLab.setText(filePath);
            setPasswordFieldVisibility();
        });
    }

    private void deleteDirectoryStream(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    private void setPasswordFieldVisibility() {
        encrypted = files.containsKey(file.getAbsolutePath());

        if (encrypted) {
            newPassword.setVisible(false);
            password.setVisible(true);
        } else {
            password.setVisible(false);
            newPassword.setVisible(true);
        }
    }

    private void RenewVariables() throws IOException, CryptoException {
        if (fileCreated) {
            files.put(filePath, passText);
            observableFiles.add(filePath);
        } else if (fileDeleted) {
            files.remove(filePath, passText);
            observableFiles.remove(filePath);
        }

        fileListView.getSelectionModel().clearSelection();

        encrypted = false;
        singleFileLab.setText("");
        filePath = "";
        password.setText("");
        newPassword.setText("");
        password.setVisible(false);
        newPassword.setVisible(false);
        file = null;
        passText = null;
        fileDeleted = false;
        fileCreated = false;

        writeApp(files, encryptionPassword);
    }

    private void RaiseAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }


    @FXML
    void SingleFileChooser(ActionEvent event) {
        FileChooser fc = new FileChooser();
        Stage initialStage = (Stage) anchorPane.getScene().getWindow();
        file = fc.showOpenDialog(initialStage);

        if (file != null) {
            filePath = file.getAbsolutePath();
            singleFileLab.setText("Selected File:: " + filePath);
            singleFileLab.setVisible(true);
            setPasswordFieldVisibility();
        }
    }

    @FXML
    void SingleDirChooser(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        Stage initialStage = (Stage) anchorPane.getScene().getWindow();
        file = dc.showDialog(initialStage);

        if (file != null) {
            filePath = file.getAbsolutePath();
            singleFileLab.setText("Selected File:: " + filePath);
            singleFileLab.setVisible(true);
            setPasswordFieldVisibility();
        }
    }

    @FXML
    void SetHideFolder() throws Exception {
        passText = encrypted ? password.getText() : newPassword.getText();

        if (passText == null || passText.isEmpty()) {
            RaiseAlert("Write the password first.");

            return;
        }

        //locate the full path to the file
        Path p = Paths.get(filePath);

        if (encrypted) {
            if (files.get(filePath).equals(passText)) {
                //unhide the Log file
                Files.setAttribute(p, "dos:hidden", false);
                fileDeleted = true;
            } else {
                RaiseAlert("Wrong password!");
            }
        } else {
            //hide the Log file
            Files.setAttribute(p, "dos:hidden", true);
            fileCreated = true;
        }

        //link file to DosFileAttributes
        DosFileAttributes dos = Files.readAttributes(p, DosFileAttributes.class);

        RenewVariables();
    }

    @FXML
    void EncryptDecrypt() throws Exception {
        passText = encrypted ? password.getText() : newPassword.getText();

        if (passText == null || passText.isEmpty()) {
            RaiseAlert("Write the password first.");

            return;
        }

        if (file!=null) {
            if (!file.isDirectory()) {
                if (encrypted)
                    Decrypt();
                else
                    Encrypt();
            } else {
                RaiseAlert("Choose a FILE to encrypt.");
            }

            RenewVariables();
        }
    }

    @FXML
    void ZipUnzip() throws IOException, CryptoException {
        passText = encrypted ? password.getText() : newPassword.getText();
        boolean equals = Optional.of("zip").equals(getExtensionByStringHandling(filePath));

        if (passText == null || passText.isEmpty()) {
            RaiseAlert("Write the password first.");

            return;
        }

        if (file.isDirectory())
            Zip();
        else if (equals)
            UnZip();
        else
            RaiseAlert("This zip file is not in our list.");

        RenewVariables();
    }

    private void Zip() throws IOException {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);

        filePath+=".zip";

        ZipFile zipFile = new ZipFile(new File(filePath), passText.toCharArray());
        zipFile.addFolder(file, zipParameters);

        deleteDirectoryStream(file.toPath());

        outputLabel.setText("Zipped");
        fileCreated = true;
    }

    private void UnZip() throws IOException {
        if (files.get(filePath).equals(passText)) {
            ZipFile zipFile = new ZipFile(file, passText.toCharArray());
            zipFile.extractAll(file.getAbsoluteFile().getParent());

            new File(filePath).delete();
            outputLabel.setText("Unzipped");
            fileDeleted = true;
        }
    }

    private void Encrypt() throws CryptoException {
        CryptoUtils.encrypt(passText, file, file);
        //setHideFolder(filePath, true);
        outputLabel.setText("Encrypted");
        fileCreated = true;
    }

    private void Decrypt() throws CryptoException {
        if (files.get(filePath).equals(passText)) {
            CryptoUtils.decrypt(passText, file, file);
            //setHideFolder(filePath, false);
            outputLabel.setText("Decrypted");
            fileDeleted = true;
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText(null);
            alert.setContentText("Wrong Password");
            alert.showAndWait();
        }
    }

    public Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }


    public static void writeApp(HashMap<String, String> filesToSave, String encryptionPassword) throws IOException, CryptoException {
        FileOutputStream fos = new FileOutputStream("file.txt");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(filesToSave);

        oos.flush();
        fos.close();
        oos.close();

        File file = new File("file.txt");
        CryptoUtils.encrypt(encryptionPassword, file, file);
    }

    public static HashMap<String, String> readApp(String encryptionPassword) throws IOException, ClassNotFoundException, CryptoException {
        File file = new File("file.txt");
        CryptoUtils.decrypt(encryptionPassword, file, file);

        FileInputStream fis = new FileInputStream("file.txt");
        ObjectInputStream ois = new ObjectInputStream(fis);
        HashMap<String, String> files = (HashMap<String, String>)ois.readObject();

        fis.close();
        ois.close();
        return files;
    }
}
