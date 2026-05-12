package com.example.scorepersistancedemo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ScoreViewModel scoreViewModel;
    private TextView scoreDisplay;
    private Button btnPlus, btnMinus, btnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scoreDisplay = findViewById(R.id.scoreDisplay);
        btnPlus      = findViewById(R.id.btnPlus);
        btnMinus     = findViewById(R.id.btnMinus);
        btnClear     = findViewById(R.id.btnClear);

        scoreViewModel = new ViewModelProvider(this).get(ScoreViewModel.class);

        scoreViewModel.obtenirScore().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer nouvelleValeur) {
                scoreDisplay.setText(String.valueOf(nouvelleValeur));
            }
        });

        btnPlus.setOnClickListener(v  -> scoreViewModel.augmenter());
        btnMinus.setOnClickListener(v -> scoreViewModel.diminuer());
        btnClear.setOnClickListener(v -> scoreViewModel.reinitialiser());
    }
}