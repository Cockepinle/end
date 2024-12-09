package com.example.faaaaa;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText nameText, descriptionText;
    private Button addButton, uploadImageButton, deleteButton; // Добавляем кнопку удаления
    private ImageView itemImageView;
    private ListView listView;

    private ArrayAdapter<String> adapter;
    private List<ClothindItem> clothingItems = new ArrayList<>();
    private String selectedImagePath;
    private String selectedItemId; // Хранит ID выбранного товара

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Paper.init(this);

        nameText = findViewById(R.id.nameText);
        descriptionText = findViewById(R.id.descriptionText);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        itemImageView = findViewById(R.id.itemImageView);
        addButton = findViewById(R.id.addButton);
        deleteButton = findViewById(R.id.deleteButton); // Инициализация кнопки удаления
        listView = findViewById(R.id.listView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getClothingItemNames());
        listView.setAdapter(adapter);

        uploadImageButton.setOnClickListener(v -> openFileChooser());

        addButton.setOnClickListener(v -> {
            String name = nameText.getText().toString();
            String description = descriptionText.getText().toString();
            if (!name.isEmpty() && !description.isEmpty() && selectedImagePath != null) {
                ClothindItem item = new ClothindItem(name + "_" + System.currentTimeMillis(), name, description, selectedImagePath);
                Paper.book().write(item.getId(), item);
                updateClothingList();
                clearInputs();
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            ClothindItem item = clothingItems.get(position);
            nameText.setText(item.getName());
            descriptionText.setText(item.getDescription());
            selectedImagePath = item.getImagePath();
            selectedItemId = item.getId(); // Сохраняем ID выбранного товара

            // Загрузка изображения в ImageView
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(selectedImagePath));
                itemImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Логика для кнопки удаления
        deleteButton.setOnClickListener(v -> {
            if (selectedItemId == null) {
                Toast.makeText(MainActivity.this, "Пожалуйста, сначала выберите товар", Toast.LENGTH_SHORT).show();
                return;
            }
            Paper.book().delete(selectedItemId); // Удаляем товар из базы данных
            updateClothingList(); // Обновляем список
            clearInputs(); // Очищаем поля ввода
            Toast.makeText(MainActivity.this, "Товар удален", Toast.LENGTH_SHORT).show();
        });

        updateClothingList(); // Обновляем список при запуске приложения
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            selectedImagePath = uri.toString(); // Сохраняем путь к изображению
            itemImageView.setImageURI(uri); // Отображаем изображение в ImageView
        }
    }

    private List<String> getClothingItemNames() {
        List<String> names = new ArrayList<>();
        clothingItems.clear(); // Очищаем список перед обновлением
        for (String key : Paper.book().getAllKeys()) {
            ClothindItem item = Paper.book().read(key);
            names.add(item.getName());
            clothingItems.add(item); // Добавляем товар в список
        }
        return names;
    }

    private void updateClothingList() {
        adapter.clear();
        adapter.addAll(getClothingItemNames());
        adapter.notifyDataSetChanged();
    }

    private void clearInputs() {
        nameText.setText("");
        descriptionText.setText("");
        selectedImagePath = null; // Сбрасываем путь к изображению
        selectedItemId = null; // Сбрасываем ID выбранного товара
        itemImageView.setImageResource(0); // Очищаем ImageView
    }
}