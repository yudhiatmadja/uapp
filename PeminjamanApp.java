package com.example.uap;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;



public class PeminjamanApp extends Application {

    private static final String FILE_PATH = "borrowed_books.txt";

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Aplikasi Peminjaman Buku");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setVgap(10);
        grid.setHgap(10);

        Label titleLabel = new Label("Judul Buku:");
        TextField titleTextField = new TextField();
        grid.add(titleLabel, 0, 0);
        grid.add(titleTextField, 1, 0);

        Label borrowerLabel = new Label("Peminjam:");
        TextField borrowerTextField = new TextField();
        grid.add(borrowerLabel, 0, 1);
        grid.add(borrowerTextField, 1, 1);

        Button borrowButton = new Button("Pinjam");
        grid.add(borrowButton, 0, 2);

        Button returnButton = new Button("Kembalikan");
        grid.add(returnButton, 1, 2);

        Label searchLabel = new Label("Cari Judul:");
        TextField searchTextField = new TextField();
        grid.add(searchLabel, 0, 6);
        grid.add(searchTextField, 1, 6);


        Button deleteButton = new Button("Delete");
        grid.add(deleteButton, 1, 5);

        TableView<Book> tableView = new TableView<>();
        TableColumn<Book, String> titleColumn = new TableColumn<>("Judul Buku");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Book, String> borrowerColumn = new TableColumn<>("Peminjam");
        borrowerColumn.setCellValueFactory(new PropertyValueFactory<>("borrower"));

        TableColumn<Book, String> statusColumn = new TableColumn<>("Status");

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            String searchText = newValue.toLowerCase();
            tableView.getItems().clear();
            List<Book> filteredBooks = loadDataFromFileWithFilter(searchText);
            tableView.getItems().addAll(filteredBooks);
        });
        statusColumn.setCellValueFactory(data -> {
            boolean returned = data.getValue().isReturned();
            return new SimpleStringProperty(returned ? "Dikembalikan" : "Belum Dikembalikan");
        });

        tableView.getColumns().addAll(titleColumn, borrowerColumn, statusColumn);
        grid.add(tableView, 0, 4, 2, 1);

        TextArea outputTextArea = new TextArea();
        outputTextArea.setEditable(false);
        grid.add(outputTextArea, 0, 3, 2, 1);

        loadDataFromFile(outputTextArea, tableView);

        borrowButton.setOnAction(e -> {
            String title = titleTextField.getText();
            String borrower = borrowerTextField.getText();
            String output = "Buku '" + title + "' dipinjam oleh " + borrower;
            outputTextArea.setText(output);
            if (title.isEmpty() || borrower.isEmpty()) {
                outputTextArea.setText("Judul buku dan nama peminjam harus diisi.");
            }
                 else {
                saveToFile(title, borrower);
                tableView.getItems().add(new Book(title, borrower));
            }
        });

        returnButton.setOnAction(e -> {
            String title = titleTextField.getText();
            String borrower = borrowerTextField.getText();
            String output = "Buku '" + title + "' dikembalikan oleh " + borrower;
            outputTextArea.setText(output);

            removeFromFile(title, borrower);

            for (Book book : tableView.getItems()) {
                if (book.getTitle().equals(title) && book.getBorrower().equals(borrower)) {
                    book.setReturned(true);
                    break;
                }
            }

            refreshTableView(tableView);
        });

        deleteButton.setOnAction(e -> {
            Book selectedBook = tableView.getSelectionModel().getSelectedItem();
            if (selectedBook != null) {
                String title = selectedBook.getTitle();
                String borrower = selectedBook.getBorrower();
                String output = "Data '" + title + "' oleh " + borrower + " dihapus.";
                outputTextArea.setText(output);

                removeFromFile(title, borrower);
                tableView.getItems().remove(selectedBook);
            } else {
                outputTextArea.setText("Pilih buku yang ingin dihapus.");
            }
        });

        Scene scene = new Scene(grid, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private List<Book> loadDataFromFileWithFilter(String searchText) {
        List<Book> filteredBooks = new ArrayList<>();
        try {
            Path path = Paths.get(FILE_PATH);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length == 3) {
                        String title = parts[0];
                        String borrower = parts[1];
                        boolean returned = Boolean.parseBoolean(parts[2]);
                        if (title.toLowerCase().contains(searchText)) {
                            filteredBooks.add(new Book(title, borrower, returned));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filteredBooks;
    }


    private void saveToFile(String title, String borrower) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.write(title + "," + borrower + ",false" + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeFromFile(String title, String borrower) {
        try {
            Path path = Paths.get(FILE_PATH);
            List<String> lines = Files.readAllLines(path);
            List<String> updatedLines = lines.stream()
                    .filter(line -> !(line.startsWith(title + "," + borrower)))
                    .collect(Collectors.toList());
            Files.write(path, updatedLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void loadDataFromFile(TextArea outputTextArea, TableView<Book> tableView) {
        try {
            Path path = Paths.get(FILE_PATH);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                outputTextArea.setText("Data Peminjaman:\n");
                List<Book> books = new ArrayList<>();
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length == 3) {
                        String title = parts[0];
                        String borrower = parts[1];
                        boolean returned = Boolean.parseBoolean(parts[2]);
                        outputTextArea.appendText(line + "\n");
                        books.add(new Book(title, borrower, returned));
                    }
                }
                tableView.getItems().addAll(books);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshTableView(TableView<Book> tableView) {
        tableView.refresh();
    }

    public static class Book {
        private String title;
        private String borrower;
        private boolean returned;

        public Book(String title, String borrower) {
            this.title = title;
            this.borrower = borrower;
            this.returned = false; // default: belum dikembalikan
        }

        public Book(String title, String borrower, boolean returned) {
            this.title = title;
            this.borrower = borrower;
            this.returned = returned;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBorrower() {
            return borrower;
        }

        public void setBorrower(String borrower) {
            this.borrower = borrower;
        }

        public boolean isReturned() {
            return returned;
        }

        public void setReturned(boolean returned) {
            this.returned = returned;
        }
    }
}
