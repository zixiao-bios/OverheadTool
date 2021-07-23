package com.zixiao_bios.overheadtool;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;

public class TaskInfoActivity extends AppCompatActivity {

    private TextInputEditText taskMessageText, testingMessageText, reportMessageText;
    private Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_info);

        taskMessageText = findViewById(R.id.taskMassageText);
        testingMessageText = findViewById(R.id.testingMassageText);
        reportMessageText = findViewById(R.id.reportMassageText);
        finishButton = findViewById(R.id.finishButton);
    }
}